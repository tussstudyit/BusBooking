const path = require("path");
const admin = require(path.join(__dirname, "..", "functions", "node_modules", "firebase-admin"));
const serviceAccount = require(path.join(__dirname, "..", "admin-web", "config", "firebase-service-account.json"));

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: process.env.FIREBASE_PROJECT_ID || "busbooking-f44f162d",
});

const email = "admin@busbooking.com";
const password = "Admin@123456";
const phone = (process.env.ADMIN_PHONE || "").trim();

async function main() {
  let user;
  try {
    user = await admin.auth().getUserByEmail(email);
    user = await admin.auth().updateUser(user.uid, {
      password,
      disabled: false,
      emailVerified: true,
    });
  } catch (error) {
    if (error.code !== "auth/user-not-found") {
      throw error;
    }
    user = await admin.auth().createUser({
      email,
      password,
      disabled: false,
      emailVerified: true,
      displayName: "BusBooking Admin",
    });
  }

  await admin.firestore().collection("users").doc(user.uid).set({
    uid: user.uid,
    name: "BusBooking Admin",
    email,
    authEmail: email,
    phone,
    role: "ADMIN",
    isBlocked: false,
    updatedAt: Date.now(),
  }, { merge: true });

  if (phone) {
    await admin.firestore().collection("phoneLogins").doc(phone).set({
      uid: user.uid,
      authEmail: email,
      email,
      phone,
      updatedAt: Date.now(),
    }, { merge: true });
  }

  console.log(JSON.stringify({ email, phone: phone || null, uid: user.uid, role: "ADMIN", isBlocked: false }, null, 2));
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
