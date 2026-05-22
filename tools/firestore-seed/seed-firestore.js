const fs = require("fs");
const path = require("path");
const admin = require("firebase-admin");

const serviceAccountPath = process.env.GOOGLE_APPLICATION_CREDENTIALS ||
  path.join(__dirname, "..", "..", "admin-web", "config", "firebase-service-account.json");

if (!fs.existsSync(serviceAccountPath)) {
  console.error(`Missing service account file: ${serviceAccountPath}`);
  process.exit(1);
}

const PROJECT_ID = process.env.FIREBASE_PROJECT_ID || "busbooking-f44f162d";
const DAY_MS = 86_400_000;
const HOUR_MS = 3_600_000;

admin.initializeApp({
  credential: admin.credential.cert(require(serviceAccountPath)),
  projectId: PROJECT_ID,
});

const db = admin.firestore();

const provinces = {
  ha_noi: "H\u00e0 N\u1ed9i",
  da_nang: "\u0110\u00e0 N\u1eb5ng",
  ho_chi_minh: "TP. H\u1ed3 Ch\u00ed Minh",
};

const routePairs = [
  ["ha_noi", "da_nang", 765, 13, 650000],
  ["da_nang", "ho_chi_minh", 980, 16, 850000],
];

const buses = ["43A-12345", "43A-23456"].map((licensePlate, index) => ({
  id: index + 1,
  busName: "Xe T\u00e2n Quang D\u0169ng",
  totalSeats: 34,
  licensePlate,
  seatLayoutJson: "",
  isActive: true,
  createdAt: Date.now(),
}));

function routes() {
  return routePairs.flatMap(([originId, destinationId, distance, durationHours, price]) => {
    const durationMs = durationHours * HOUR_MS;
    return [
      { originId, destinationId, distance, durationMs, price },
      { originId: destinationId, destinationId: originId, distance, durationMs, price },
    ];
  });
}

function todayMidnight() {
  const date = new Date();
  date.setHours(0, 0, 0, 0);
  return date.getTime();
}

function dayTs(dayOffset) {
  return todayMidnight() + dayOffset * DAY_MS;
}

function ts(dayOffset, hour, minute) {
  return dayTs(dayOffset) + hour * HOUR_MS + minute * 60_000;
}

function seatNumber(index) {
  return `${index <= 17 ? "A" : "B"}${index <= 17 ? index : index - 17}`;
}

function generateSeats(busId) {
  const seats = [];
  for (let i = 1; i <= 34; i += 1) {
    const floor = i <= 17 ? 1 : 2;
    seats.push({
      id: i,
      busId,
      seatNumber: seatNumber(i),
      floor,
      rowIndex: Math.floor(((i - 1) % 17) / 3),
      columnIndex: (i - 1) % 3,
      isWindow: i % 3 === 1 || i % 3 === 0,
      isAisle: i % 3 === 2,
      seatType: "STANDARD",
      createdAt: Date.now(),
    });
  }
  return seats;
}

async function commitInChunks(operations) {
  for (let index = 0; index < operations.length; index += 450) {
    const batch = db.batch();
    operations.slice(index, index + 450).forEach((operation) => {
      batch.set(operation.ref, operation.data, { merge: true });
    });
    await batch.commit();
  }
}

async function seed() {
  const routeList = routes();
  const operations = [];
  const now = Date.now();

  routeList.forEach((route, index) => {
    const id = index + 1;
    operations.push({
      ref: db.collection("routes").doc(String(id)),
      data: {
        id,
        originId: route.originId,
        destinationId: route.destinationId,
        origin: provinces[route.originId],
        destination: provinces[route.destinationId],
        distance: route.distance,
        durationMs: route.durationMs,
        suggestedPrice: route.price,
        isActive: true,
        createdAt: now,
      },
    });
  });

  buses.forEach((bus) => {
    operations.push({ ref: db.collection("buses").doc(String(bus.id)), data: bus });
    generateSeats(bus.id).forEach((seat) => {
      operations.push({
        ref: db.collection("buses").doc(String(bus.id)).collection("seats").doc(String(seat.id)),
        data: seat,
      });
    });
  });

  let tripId = 1;
  let busIndex = 0;
  for (let dayOffset = 1; dayOffset <= 2; dayOffset += 1) {
    routeList.forEach((route, routeIndex) => {
      [[7, 0], [15, 30]].forEach(([hour, minute]) => {
        const departureTime = ts(dayOffset, hour, minute);
        const bus = buses[busIndex % buses.length];
        busIndex += 1;
        operations.push({
          ref: db.collection("trips").doc(String(tripId)),
          data: {
            id: tripId,
            routeId: routeIndex + 1,
            busId: bus.id,
            departureTime,
            arrivalTime: departureTime + route.durationMs,
            price: route.price,
            tripDate: dayTs(dayOffset),
            status: "SCHEDULED",
            createdAt: now,
          },
        });
        tripId += 1;
      });
    });
  }

  await commitInChunks(operations);
  console.log(`Seeded ${routeList.length} routes, ${buses.length} buses, ${buses.length * 34} seats, ${tripId - 1} trips.`);
}

seed()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
