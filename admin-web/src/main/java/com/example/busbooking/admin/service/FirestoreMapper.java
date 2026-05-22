package com.example.busbooking.admin.service;

import com.google.cloud.firestore.DocumentSnapshot;

final class FirestoreMapper {
    private FirestoreMapper() {
    }

    static Long longValue(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    static Integer intValue(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    static Double doubleValue(DocumentSnapshot document, String field) {
        Object value = document.get(field);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return null;
    }
}
