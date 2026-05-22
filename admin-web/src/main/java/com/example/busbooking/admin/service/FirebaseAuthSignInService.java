package com.example.busbooking.admin.service;

import com.example.busbooking.admin.config.FirebaseProperties;
import java.util.Map;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Service
public class FirebaseAuthSignInService {
    private final FirebaseProperties properties;
    private final RestClient restClient;

    public FirebaseAuthSignInService(FirebaseProperties properties, RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.restClient = restClientBuilder.build();
    }

    public SignInResult signIn(String email, String password) {
        if (!StringUtils.hasText(properties.webApiKey())) {
            throw new BadCredentialsException("FIREBASE_WEB_API_KEY is not configured");
        }

        String url = "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key="
                + properties.webApiKey();
        try {
            Map<?, ?> response = restClient.post()
                    .uri(url)
                    .body(Map.of(
                            "email", email,
                            "password", password,
                            "returnSecureToken", true
                    ))
                    .retrieve()
                    .body(Map.class);

            if (response == null || response.get("localId") == null || response.get("idToken") == null) {
                throw new BadCredentialsException("Invalid Firebase Auth response");
            }

            return new SignInResult(
                    String.valueOf(response.get("localId")),
                    String.valueOf(response.get("email")),
                    String.valueOf(response.get("idToken"))
            );
        } catch (Exception e) {
            throw new BadCredentialsException("Invalid admin credentials", e);
        }
    }

    public record SignInResult(String localId, String email, String idToken) {
    }
}
