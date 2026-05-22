package com.example.busbooking.admin.security;

public record AdminPrincipal(
        String uid,
        String email,
        String idToken
) {
}
