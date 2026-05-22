package com.example.busbooking.admin.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query.Direction;
import org.springframework.stereotype.Service;

@Service
public class FirestoreIdService {
    private final Firestore firestore;

    public FirestoreIdService(Firestore firestore) {
        this.firestore = firestore;
    }

    public long nextNumericId(String collection) {
        try {
            return firestore.collection(collection)
                    .orderBy("id", Direction.DESCENDING)
                    .limit(1)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .findFirst()
                    .map(document -> FirestoreMapper.longValue(document, "id"))
                    .map(id -> id + 1)
                    .orElse(1L);
        } catch (Exception e) {
            throw new IllegalStateException("Could not allocate id for " + collection, e);
        }
    }
}
