package com.example.busbooking.admin.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.cloud.FirestoreClient;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({FirebaseProperties.class, VnpayProperties.class})
public class FirebaseConfig {

    @Bean
    FirebaseApp firebaseApp(FirebaseProperties properties) throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return FirebaseApp.getInstance();
        }

        File serviceAccountFile = resolveServiceAccountFile(
                properties.serviceAccountPath(),
                properties.projectId()
        );
        try (FileInputStream serviceAccount = new FileInputStream(serviceAccountFile)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setProjectId(properties.projectId())
                    .build();
            return FirebaseApp.initializeApp(options);
        }
    }

    @Bean(destroyMethod = "")
    Firestore firestore(FirebaseApp firebaseApp) {
        return FirestoreClient.getFirestore(firebaseApp);
    }

    @Bean
    FirebaseAuth firebaseAuth(FirebaseApp firebaseApp) {
        return FirebaseAuth.getInstance(firebaseApp);
    }

    private File resolveServiceAccountFile(String configuredPath, String projectId) throws IOException {
        List<File> candidates = new ArrayList<>();
        File configuredFile = new File(configuredPath == null ? "" : configuredPath);
        candidates.add(configuredFile);

        if (!configuredFile.isAbsolute()) {
            File workingDir = new File(System.getProperty("user.dir"));
            candidates.add(new File(workingDir, configuredPath));
            File parentDir = workingDir.getParentFile();
            if (parentDir != null) {
                candidates.add(new File(parentDir, configuredPath));
                candidates.add(new File(parentDir, "admin-web/" + configuredPath));
            }
        }

        for (File candidate : candidates) {
            if (candidate != null && candidate.isFile()) {
                return candidate;
            }
        }

        String triedPaths = candidates.stream()
                .filter(candidate -> candidate != null)
                .map(File::getAbsolutePath)
                .distinct()
                .reduce((left, right) -> left + ", " + right)
                .orElse(configuredPath);
        throw new IOException("Firebase service account file not found for project "
                + projectId
                + ". Put the key at admin-web/config/firebase-service-account.json "
                + "or set FIREBASE_SERVICE_ACCOUNT_PATH. Tried: " + triedPaths);
    }
}
