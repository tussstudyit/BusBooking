package com.example.busbooking.admin.security;

import com.example.busbooking.admin.service.FirebaseAuthSignInService;
import com.example.busbooking.admin.service.UserAdminService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class FirebaseAdminAuthenticationProvider implements AuthenticationProvider {
    private final FirebaseAuthSignInService signInService;
    private final UserAdminService userAdminService;
    private final Set<String> fallbackAdminEmails;

    public FirebaseAdminAuthenticationProvider(
            FirebaseAuthSignInService signInService,
            UserAdminService userAdminService,
            @Value("${admin.fallback-emails:admin@busbooking.com}") String fallbackAdminEmails
    ) {
        this.signInService = signInService;
        this.userAdminService = userAdminService;
        this.fallbackAdminEmails = List.of(fallbackAdminEmails.split(","))
                .stream()
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(email -> !email.isBlank())
                .collect(Collectors.toSet());
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String login = authentication.getName();
        String password = String.valueOf(authentication.getCredentials());
        String authEmail = resolveAuthEmail(login);

        FirebaseAuthSignInService.SignInResult result = signInService.signIn(authEmail, password);
        boolean allowed;
        try {
            allowed = userAdminService.isActiveAdmin(result.localId());
        } catch (RuntimeException e) {
            allowed = isFallbackAdmin(result.email());
        }
        if (!allowed && isFallbackAdmin(result.email())) {
            allowed = true;
        }
        if (!allowed) {
            throw new BadCredentialsException("Account is not an active admin");
        }

        AdminPrincipal principal = new AdminPrincipal(result.localId(), result.email(), result.idToken());
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private boolean isFallbackAdmin(String email) {
        return email != null && fallbackAdminEmails.contains(email.trim().toLowerCase());
    }

    private String resolveAuthEmail(String login) {
        String trimmed = login == null ? "" : login.trim();
        if (trimmed.contains("@")) {
            return trimmed;
        }

        String normalizedPhone = normalizePhone(trimmed);
        return userAdminService.findAuthEmailByPhone(normalizedPhone)
                .orElseGet(() -> phoneToAuthEmail(normalizedPhone));
    }

    private String normalizePhone(String phone) {
        return phone == null ? "" : phone.trim().replaceAll("\\s+", "");
    }

    private String phoneToAuthEmail(String phone) {
        String token = normalizePhone(phone).chars()
                .filter(Character::isLetterOrDigit)
                .collect(
                        StringBuilder::new,
                        StringBuilder::appendCodePoint,
                        StringBuilder::append
                )
                .toString()
                .toLowerCase();
        if (token.isBlank()) {
            token = "user";
        }
        return "phone-" + token + "@busbooking.local";
    }
}
