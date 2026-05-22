"use strict";

const crypto = require("crypto");
const admin = require("firebase-admin");
const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { onSchedule } = require("firebase-functions/v2/scheduler");
const { defineSecret, defineString } = require("firebase-functions/params");

admin.initializeApp();

const db = admin.firestore();

const REGION = "asia-southeast1";
const HOLD_MINUTES = 15;

const vnpayTmnCode = defineSecret("VNPAY_TMN_CODE");
const vnpayHashSecret = defineSecret("VNPAY_HASH_SECRET");
const vnpayPaymentUrl = defineString("VNPAY_PAYMENT_URL", {
  default: "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html",
});
const vnpayReturnUrl = defineString("VNPAY_RETURN_URL", {
  default: "",
});

function requireAuth(request) {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "Login is required.");
  }
  return request.auth.uid;
}

async function requireAdmin(uid) {
  const user = await admin.auth().getUser(uid);
  if (user.customClaims && user.customClaims.admin === true) {
    return;
  }

  const profile = await db.collection("users").doc(uid).get();
  if (profile.exists && profile.get("role") === "ADMIN" && profile.get("isBlocked") !== true) {
    return;
  }

  throw new HttpsError("permission-denied", "Admin permission is required.");
}

function formatVnpDate(date) {
  const pad = (value) => String(value).padStart(2, "0");
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds()),
  ].join("");
}

function sortObject(input) {
  return Object.keys(input)
    .sort()
    .reduce((result, key) => {
      const value = input[key];
      if (value !== undefined && value !== null && value !== "") {
        result[key] = String(value);
      }
      return result;
    }, {});
}

function buildSignedVnpayUrl(params, hashSecret) {
  const sortedParams = sortObject(params);
  const signData = new URLSearchParams(sortedParams).toString();
  const secureHash = crypto
    .createHmac("sha512", hashSecret)
    .update(Buffer.from(signData, "utf-8"))
    .digest("hex");

  sortedParams.vnp_SecureHash = secureHash;
  return `${vnpayPaymentUrl.value()}?${new URLSearchParams(sortedParams).toString()}`;
}

function verifyVnpaySignature(query, hashSecret) {
  const params = { ...query };
  const receivedHash = params.vnp_SecureHash;
  delete params.vnp_SecureHash;
  delete params.vnp_SecureHashType;

  const sortedParams = sortObject(params);
  const signData = new URLSearchParams(sortedParams).toString();
  const expectedHash = crypto
    .createHmac("sha512", hashSecret)
    .update(Buffer.from(signData, "utf-8"))
    .digest("hex");

  return receivedHash === expectedHash;
}

async function updatePaymentFromVnpay(query, hashSecret) {
  const isValid = verifyVnpaySignature(query, hashSecret);
  if (!isValid) {
    return { rspCode: "97", message: "Invalid checksum" };
  }

  const paymentId = String(query.vnp_TxnRef || "");
  if (!paymentId) {
    return { rspCode: "01", message: "Missing payment reference" };
  }

  const paymentRef = db.collection("payments").doc(paymentId);
  const paymentSnapshot = await paymentRef.get();
  if (!paymentSnapshot.exists) {
    return { rspCode: "01", message: "Payment not found" };
  }

  const payment = paymentSnapshot.data();
  const success = query.vnp_ResponseCode === "00" && query.vnp_TransactionStatus === "00";
  const paymentStatus = success ? "SUCCESS" : "FAILED";
  const ticketStatus = success ? "CONFIRMED" : "PAYMENT_FAILED";
  const tripSeatStatus = success ? "CONFIRMED" : "EXPIRED";

  await db.runTransaction(async (transaction) => {
    const ticketRef = db.collection("tickets").doc(payment.ticketId);
    const tripSeatRef = db.collection("tripSeats").doc(`${payment.tripId}_${payment.seatId}`);

    transaction.update(paymentRef, {
      status: paymentStatus,
      vnpTransactionNo: query.vnp_TransactionNo || null,
      vnpResponseCode: query.vnp_ResponseCode || null,
      vnpPayDate: query.vnp_PayDate || null,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    transaction.update(ticketRef, {
      status: ticketStatus,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    if (success) {
      transaction.set(
        tripSeatRef,
        {
          status: tripSeatStatus,
          updatedAt: admin.firestore.FieldValue.serverTimestamp(),
        },
        { merge: true }
      );
    } else {
      transaction.delete(tripSeatRef);
    }
  });

  return { rspCode: "00", message: "Confirm success" };
}

exports.createVnpayPaymentUrl = onCall(
  {
    region: REGION,
    secrets: [vnpayTmnCode, vnpayHashSecret],
  },
  async (request) => {
    const uid = requireAuth(request);
    const paymentId = String(request.data && request.data.paymentId ? request.data.paymentId : "");
    const ipAddress = request.rawRequest.ip || "127.0.0.1";

    if (!paymentId) {
      throw new HttpsError("invalid-argument", "paymentId is required.");
    }

    const paymentRef = db.collection("payments").doc(paymentId);
    const paymentSnapshot = await paymentRef.get();

    if (!paymentSnapshot.exists) {
      throw new HttpsError("not-found", "Payment not found.");
    }

    const payment = paymentSnapshot.data();
    if (payment.userId !== uid) {
      throw new HttpsError("permission-denied", "This payment belongs to another user.");
    }

    if (!["CREATED", "PENDING"].includes(payment.status)) {
      throw new HttpsError("failed-precondition", "Payment is not payable.");
    }

    const now = new Date();
    const expireAt = new Date(now.getTime() + HOLD_MINUTES * 60 * 1000);
    const amount = Math.round(Number(payment.amount || 0));

    if (amount <= 0) {
      throw new HttpsError("failed-precondition", "Invalid payment amount.");
    }

    const returnUrl = payment.returnUrl || vnpayReturnUrl.value();
    if (!returnUrl) {
      throw new HttpsError("failed-precondition", "VNPAY_RETURN_URL is not configured.");
    }

    const params = {
      vnp_Version: "2.1.0",
      vnp_Command: "pay",
      vnp_TmnCode: vnpayTmnCode.value(),
      vnp_Amount: amount * 100,
      vnp_CurrCode: "VND",
      vnp_TxnRef: paymentId,
      vnp_OrderInfo: `Thanh toan ve xe ${payment.ticketId}`,
      vnp_OrderType: "other",
      vnp_Locale: "vn",
      vnp_ReturnUrl: returnUrl,
      vnp_IpAddr: ipAddress,
      vnp_CreateDate: formatVnpDate(now),
      vnp_ExpireDate: formatVnpDate(expireAt),
    };

    const paymentUrl = buildSignedVnpayUrl(params, vnpayHashSecret.value());

    await paymentRef.update({
      status: "PENDING",
      vnpTxnRef: paymentId,
      updatedAt: admin.firestore.FieldValue.serverTimestamp(),
    });

    return {
      paymentId,
      paymentUrl,
      expiresAt: expireAt.getTime(),
    };
  }
);

exports.vnpayIpnHandler = onRequest(
  {
    region: REGION,
    secrets: [vnpayHashSecret],
  },
  async (request, response) => {
    try {
      const result = await updatePaymentFromVnpay(request.query, vnpayHashSecret.value());
      response.json({ RspCode: result.rspCode, Message: result.message });
    } catch (error) {
      console.error(error);
      response.json({ RspCode: "99", Message: "Unknown error" });
    }
  }
);

exports.vnpayReturnUrlHandler = onRequest(
  {
    region: REGION,
    secrets: [vnpayHashSecret],
  },
  async (request, response) => {
    const valid = verifyVnpaySignature(request.query, vnpayHashSecret.value());
    const success =
      valid &&
      request.query.vnp_ResponseCode === "00" &&
      request.query.vnp_TransactionStatus === "00";

    response
      .status(valid ? 200 : 400)
      .send(`
        <!doctype html>
        <html>
          <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1" />
            <title>BusBooking Payment</title>
          </head>
          <body>
            <h1>${success ? "Thanh toán thành công" : "Thanh toán chưa hoàn tất"}</h1>
            <p>Bạn có thể quay lại ứng dụng BusBooking.</p>
          </body>
        </html>
      `);
  }
);

exports.expirePendingPayments = onSchedule(
  {
    region: REGION,
    schedule: "every 5 minutes",
    timeZone: "Asia/Bangkok",
  },
  async () => {
    const now = Date.now();
    const expiredPayments = await db
      .collection("payments")
      .where("status", "in", ["CREATED", "PENDING"])
      .where("holdExpiresAt", "<=", now)
      .limit(100)
      .get();

    const batch = db.batch();

    expiredPayments.docs.forEach((doc) => {
      const payment = doc.data();
      const ticketRef = db.collection("tickets").doc(payment.ticketId);
      const tripSeatRef = db.collection("tripSeats").doc(`${payment.tripId}_${payment.seatId}`);

      batch.update(doc.ref, {
        status: "EXPIRED",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      batch.update(ticketRef, {
        status: "PAYMENT_FAILED",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      });
      batch.delete(tripSeatRef);
    });

    if (!expiredPayments.empty) {
      await batch.commit();
    }

    console.log(`Expired payments processed: ${expiredPayments.size}`);
  }
);

exports.setAdminClaim = onCall(
  {
    region: REGION,
  },
  async (request) => {
    const callerUid = requireAuth(request);
    await requireAdmin(callerUid);

    const targetUid = String(request.data && request.data.uid ? request.data.uid : "");
    const adminValue = Boolean(request.data && request.data.admin);

    if (!targetUid) {
      throw new HttpsError("invalid-argument", "uid is required.");
    }

    await admin.auth().setCustomUserClaims(targetUid, { admin: adminValue });
    await db.collection("users").doc(targetUid).set(
      {
        role: adminValue ? "ADMIN" : "USER",
        updatedAt: admin.firestore.FieldValue.serverTimestamp(),
      },
      { merge: true }
    );

    return {
      uid: targetUid,
      admin: adminValue,
    };
  }
);
