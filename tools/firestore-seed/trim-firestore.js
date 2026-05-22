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

admin.initializeApp({
  credential: admin.credential.cert(require(serviceAccountPath)),
  projectId: PROJECT_ID,
});

const db = admin.firestore();

const ROOT_COLLECTIONS_TO_CLEAR = [
  "routes",
  "buses",
  "trips",
  "tickets",
  "payments",
  "tripSeats",
];

async function deleteDocumentWithSubcollections(docRef) {
  const subcollections = await docRef.listCollections();
  for (const subcollection of subcollections) {
    await deleteCollection(subcollection);
  }
  await docRef.delete();
}

async function deleteCollection(collectionRef) {
  let deleted = 0;
  while (true) {
    const snapshot = await collectionRef.limit(100).get();
    if (snapshot.empty) {
      return deleted;
    }

    for (const doc of snapshot.docs) {
      await deleteDocumentWithSubcollections(doc.ref);
      deleted += 1;
    }
  }
}

async function main() {
  const totals = {};
  for (const collectionName of ROOT_COLLECTIONS_TO_CLEAR) {
    const deleted = await deleteCollection(db.collection(collectionName));
    totals[collectionName] = deleted;
    console.log(`Deleted ${deleted} docs from ${collectionName}`);
  }

  console.log(JSON.stringify({
    projectId: PROJECT_ID,
    cleared: totals,
    kept: [
      "users",
      "phoneLogins",
    ],
  }, null, 2));
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
