package com.example.busbooking.admin.service;

import com.example.busbooking.admin.model.UserDto;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class UserAdminService {
    private static final int DEFAULT_USER_LIMIT = 200;

    private final Firestore firestore;

    public UserAdminService(Firestore firestore) {
        this.firestore = firestore;
    }

    public boolean isActiveAdmin(String uid) {
        try {
            DocumentSnapshot document = firestore.collection("users").document(uid).get().get();
            if (!document.exists()) {
                return false;
            }
            return "ADMIN".equals(document.getString("role"))
                    && !Boolean.TRUE.equals(document.getBoolean("isBlocked"));
        } catch (Exception e) {
            throw new IllegalStateException("Could not verify admin role", e);
        }
    }

    public List<UserDto> findAll(String query) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        try {
            return firestore.collection("users")
                    .limit(DEFAULT_USER_LIMIT)
                    .get()
                    .get()
                    .getDocuments()
                    .stream()
                    .map(this::toDto)
                    .filter(user -> normalized.isBlank()
                            || contains(user.name(), normalized)
                            || contains(user.email(), normalized)
                            || contains(user.phone(), normalized))
                    .sorted((a, b) -> safe(a.name()).compareToIgnoreCase(safe(b.name())))
                    .toList();
        } catch (Exception e) {
            throw new IllegalStateException("Could not load users", e);
        }
    }

    public void setBlocked(String uid, boolean blocked) {
        try {
            firestore.collection("users").document(uid).update("isBlocked", blocked).get();
        } catch (Exception e) {
            throw new IllegalStateException("Could not update user status", e);
        }
    }

    public Optional<String> findAuthEmailByPhone(String phone) {
        String normalized = normalizePhone(phone);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        try {
            DocumentSnapshot document = firestore.collection("phoneLogins")
                    .document(normalized)
                    .get()
                    .get();
            if (!document.exists()) {
                return Optional.empty();
            }

            String authEmail = document.getString("authEmail");
            if (authEmail == null || authEmail.isBlank()) {
                authEmail = document.getString("email");
            }
            return authEmail == null || authEmail.isBlank()
                    ? Optional.empty()
                    : Optional.of(authEmail);
        } catch (Exception e) {
            throw new IllegalStateException("Could not resolve phone login", e);
        }
    }

    private UserDto toDto(DocumentSnapshot document) {
        return new UserDto(
                document.getString("uid") == null ? document.getId() : document.getString("uid"),
                document.getString("name"),
                document.getString("email"),
                document.getString("phone"),
                document.getString("role"),
                document.getBoolean("isBlocked"),
                FirestoreMapper.longValue(document, "createdAt")
        );
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase().contains(query);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.trim().replaceAll("\\s+", "");
    }
}
