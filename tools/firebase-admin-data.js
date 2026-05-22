const path = require("path");
const admin = require(path.join(__dirname, "..", "functions", "node_modules", "firebase-admin"));

const serviceAccount = require(path.join(__dirname, "..", "admin-web", "config", "firebase-service-account.json"));

const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || "busbooking-f44f162d";
const DAY_MS = 86_400_000;
const HOUR_MS = 3_600_000;
const MINUTE_MS = 60_000;

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  projectId: PROJECT_ID,
});

const db = admin.firestore();

const provinceNames = {
  ha_noi: "H\u00e0 N\u1ed9i",
  da_nang: "\u0110\u00e0 N\u1eb5ng",
  ho_chi_minh: "TP. H\u1ed3 Ch\u00ed Minh",
};

const routePairs = [
  ["ha_noi", "da_nang", 765, 13, 650000],
  ["da_nang", "ho_chi_minh", 980, 16, 850000],
];

const busPlates = [
  "43A-12345",
  "43A-23456",
];

const tripSlots = [
  [7, 0],
  [15, 30],
];

function routes() {
  const result = [];
  for (const [originId, destinationId, distanceKm, durationHours, price] of routePairs) {
    const durationMs = Math.round(durationHours * HOUR_MS);
    result.push({ originId, destinationId, distanceKm, durationMs, price });
    result.push({ originId: destinationId, destinationId: originId, distanceKm, durationMs, price });
  }
  return result;
}

function dayStart(dayOffset) {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  return date.getTime() + dayOffset * DAY_MS;
}

function ts(dayOffset, hour, minute) {
  return dayStart(dayOffset) + hour * HOUR_MS + minute * MINUTE_MS;
}

function seatNumber(index) {
  const floor = index <= 17 ? "A" : "B";
  const number = index <= 17 ? index : index - 17;
  return `${floor}${number}`;
}

async function commitChunks(ops) {
  for (let i = 0; i < ops.length; i += 450) {
    const batch = db.batch();
    ops.slice(i, i + 450).forEach((op) => batch.set(op.ref, op.data, { merge: true }));
    await batch.commit();
  }
}

async function main() {
  const now = Date.now();
  const routeList = routes();
  const ops = [];

  routeList.forEach((route, index) => {
    const id = index + 1;
    ops.push({
      ref: db.collection("routes").doc(String(id)),
      data: {
        id,
        originId: route.originId,
        destinationId: route.destinationId,
        origin: provinceNames[route.originId],
        destination: provinceNames[route.destinationId],
        distance: route.distanceKm,
        durationMs: route.durationMs,
        suggestedPrice: route.price,
        isActive: true,
        createdAt: now,
      },
    });
  });

  busPlates.forEach((plate, index) => {
    const id = index + 1;
    ops.push({
      ref: db.collection("buses").doc(String(id)),
      data: {
        id,
        busName: "Xe T\u00e2n Quang D\u0169ng",
        totalSeats: 34,
        licensePlate: plate,
        seatLayoutJson: "",
        isActive: true,
        createdAt: now,
      },
    });

    for (let seat = 1; seat <= 34; seat += 1) {
      const floor = seat <= 17 ? 1 : 2;
      ops.push({
        ref: db.collection("buses").doc(String(id)).collection("seats").doc(String(seat)),
        data: {
          id: seat,
          busId: id,
          seatNumber: seatNumber(seat),
          floor,
          rowIndex: Math.floor(((seat - 1) % 17) / 3),
          columnIndex: (seat - 1) % 3,
          isWindow: seat % 3 === 1 || seat % 3 === 0,
          isAisle: seat % 3 === 2,
          seatType: "STANDARD",
          createdAt: now,
        },
      });
    }
  });

  let tripId = 1;
  let busIndex = 0;
  for (let dayOffset = 1; dayOffset <= 2; dayOffset += 1) {
    routeList.forEach((route, routeIndex) => {
      tripSlots.forEach(([hour, minute]) => {
        const depart = ts(dayOffset, hour, minute);
        const busId = (busIndex % busPlates.length) + 1;
        busIndex += 1;
        ops.push({
          ref: db.collection("trips").doc(String(tripId)),
          data: {
            id: tripId,
            routeId: routeIndex + 1,
            busId,
            departureTime: depart,
            arrivalTime: depart + route.durationMs,
            price: route.price,
            tripDate: dayStart(dayOffset),
            status: "SCHEDULED",
            createdAt: now,
          },
        });
        tripId += 1;
      });
    });
  }

  await commitChunks(ops);

  const appRoute = await db.collection("routes").doc("1").get();
  const firstTrip = await db.collection("trips").doc("1").get();
  const firstBusId = firstTrip.exists ? firstTrip.get("busId") : null;
  const firstBus = firstBusId ? await db.collection("buses").doc(String(firstBusId)).get() : null;
  const firstBusSeats = firstBus?.exists
    ? await db.collection("buses").doc(String(firstBusId)).collection("seats").get()
    : { size: 0 };

  console.log(JSON.stringify({
    projectId: PROJECT_ID,
    routes: routeList.length,
    buses: busPlates.length,
    seats: busPlates.length * 34,
    trips: tripId - 1,
    appUserQueryCheck: {
      route: "H\u00e0 N\u1ed9i -> \u0110\u00e0 N\u1eb5ng",
      routeFound: appRoute.exists,
      routeId: appRoute.exists ? appRoute.get("id") : null,
      firstTripFound: firstTrip.exists,
      firstBusFound: Boolean(firstBus?.exists),
      firstBusSeats: firstBusSeats.size,
    },
  }, null, 2));
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
