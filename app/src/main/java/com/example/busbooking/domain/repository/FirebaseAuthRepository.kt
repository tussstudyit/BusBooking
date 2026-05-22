package com.example.busbooking.domain.repository

import com.example.busbooking.data.entity.User
import com.example.busbooking.domain.models.Result
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirebaseAuthRepository(
    private val authProvider: () -> FirebaseAuth = { FirebaseAuth.getInstance() },
    private val firestoreProvider: () -> FirebaseFirestore = { FirebaseFirestore.getInstance() }
) : IAuthRepository {

    private val auth: FirebaseAuth by lazy(LazyThreadSafetyMode.NONE) { authProvider() }
    private val firestore: FirebaseFirestore by lazy(LazyThreadSafetyMode.NONE) { firestoreProvider() }
    private val usersCollection get() = firestore.collection("users")
    private val phoneLoginsCollection get() = firestore.collection("phoneLogins")

    override suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        phone: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val contactEmail = email.trim().lowercase()
            val normalizedPhone = normalizePhone(phone)
            if (normalizedPhone.isBlank()) {
                return@withContext Result.Error(
                    Exception("Missing phone"),
                    "Vui lòng nhập số điện thoại"
                )
            }

            val authEmail = phoneToAuthEmail(normalizedPhone)

            val firebaseUser = try {
                auth.createUserWithEmailAndPassword(authEmail, password).await().user
            } catch (e: FirebaseAuthUserCollisionException) {
                auth.signInWithEmailAndPassword(authEmail, password).await().user
            }
                ?: return@withContext Result.Error(
                    Exception("Registration failed"),
                    "Không thể tạo tài khoản"
                )

            val createdAt = System.currentTimeMillis()
            val profile = hashMapOf(
                "uid" to firebaseUser.uid,
                "name" to name.trim(),
                "email" to contactEmail,
                "authEmail" to authEmail,
                "phone" to normalizedPhone,
                "role" to "USER",
                "isBlocked" to false,
                "createdAt" to createdAt
            )

            val phoneLogin = hashMapOf(
                "uid" to firebaseUser.uid,
                "authEmail" to authEmail,
                "email" to authEmail,
                "phone" to normalizedPhone,
                "createdAt" to createdAt
            )

            runCatching {
                usersCollection.document(firebaseUser.uid).set(profile).await()
            }
            runCatching {
                phoneLoginsCollection.document(normalizedPhone).set(phoneLogin).await()
            }

            Result.Success(firebaseUser.uid.toStableLongId())
        } catch (e: Exception) {
            if (e.message.orEmpty().contains("PERMISSION_DENIED", ignoreCase = true)) {
                val fallbackUser = runCatching {
                    auth.signInWithEmailAndPassword(
                        phoneToAuthEmail(normalizePhone(phone)),
                        password
                    ).await().user
                }.getOrNull()

                if (fallbackUser != null) {
                    return@withContext Result.Success(fallbackUser.uid.toStableLongId())
                }
            }
            Result.Error(e, readableAuthError(e, "Kh\u00f4ng th\u1ec3 \u0111\u0103ng k\u00fd"))
        }
    }

    override suspend fun loginUser(phone: String, password: String): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val loginPhone = normalizePhone(phone)
                val authEmail = findEmailByPhone(loginPhone)
                    ?: phoneToAuthEmail(loginPhone)

                val authResult = auth.signInWithEmailAndPassword(authEmail, password).await()
                val uid = authResult.user?.uid
                    ?: return@withContext Result.Error(
                        Exception("Invalid credentials"),
                        "So dien thoai hoac mat khau khong dung"
                    )

                val user = try {
                    val profileDocument = usersCollection.document(uid).get().await()
                    if (profileDocument.exists()) {
                        profileDocument.toAppUser()
                    } else {
                        fallbackUser(uid, authEmail, loginPhone)
                    }
                } catch (e: Exception) {
                    fallbackUser(uid, authEmail, loginPhone)
                }

                if (user.isBlocked) {
                    auth.signOut()
                    return@withContext Result.Error(
                        Exception("Account blocked"),
                        "Your account has been blocked by admin"
                    )
                }

                Result.Success(user)
            } catch (e: Exception) {
                Result.Error(e, readableAuthError(e, "Kh\u00f4ng th\u1ec3 \u0111\u0103ng nh\u1eadp"))
            }
        }

    private suspend fun findEmailByPhone(phone: String): String? {
        return try {
            val phoneLogin = phoneLoginsCollection
                .document(phone)
                .get()
                .await()

            phoneLogin.getString("authEmail")?.takeIf { it.isNotBlank() }
                ?: phoneLogin.getString("email")?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    private fun normalizePhone(phone: String): String {
        return phone.trim().replace("\\s".toRegex(), "")
    }

    private fun phoneToAuthEmail(phone: String): String {
        val token = phone
            .filter { it.isLetterOrDigit() }
            .lowercase()
            .ifBlank { "user" }

        return "phone-$token@busbooking.local"
    }

    private fun readableAuthError(error: Exception, fallback: String): String {
        val raw = error.message.orEmpty()
        return when {
            raw.contains("PERMISSION_DENIED", ignoreCase = true) -> {
                "$fallback do Firestore rules \u0111ang ch\u1eb7n. T\u00e0i kho\u1ea3n Firebase Auth v\u1eabn \u0111\u01b0\u1ee3c x\u1eed l\u00fd, h\u00e3y th\u1eed \u0111\u0103ng nh\u1eadp l\u1ea1i."
            }
            raw.contains("EMAIL_EXISTS", ignoreCase = true) -> "S\u1ed1 \u0111i\u1ec7n tho\u1ea1i n\u00e0y \u0111\u00e3 \u0111\u01b0\u1ee3c \u0111\u0103ng k\u00fd"
            raw.contains("INVALID_PASSWORD", ignoreCase = true) -> "M\u1eadt kh\u1ea9u kh\u00f4ng \u0111\u00fang"
            raw.contains("EMAIL_NOT_FOUND", ignoreCase = true) -> "S\u1ed1 \u0111i\u1ec7n tho\u1ea1i ch\u01b0a \u0111\u01b0\u1ee3c \u0111\u0103ng k\u00fd"
            else -> "$fallback: ${error.message.orEmpty()}"
        }
    }

    override suspend fun getUserProfile(userId: Long): Result<User> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser
                    ?: return@withContext Result.Error(
                        Exception("Not authenticated"),
                        "Please login again"
                    )

                val snapshot = usersCollection.document(currentUser.uid).get().await()
                if (!snapshot.exists()) {
                    return@withContext Result.Error(
                        Exception("User not found"),
                        "User profile not found"
                    )
                }

                Result.Success(snapshot.toAppUser())
            } catch (e: Exception) {
                Result.Error(e, "Error fetching profile: ${e.message}")
            }
        }

    override suspend fun updateUserProfile(user: User): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val currentUser = auth.currentUser
                    ?: return@withContext Result.Error(
                        Exception("Not authenticated"),
                        "Please login again"
                    )

                val userRef = usersCollection.document(currentUser.uid)
                val existing = runCatching { userRef.get().await() }.getOrNull()
                val oldPhone = normalizePhone(existing?.getString("phone").orEmpty())
                val newPhone = normalizePhone(user.phone)
                val authEmail = existing?.getString("authEmail")
                    ?.takeIf { it.isNotBlank() }
                    ?: currentUser.email
                    ?: if (oldPhone.isNotBlank()) phoneToAuthEmail(oldPhone) else ""

                val updates = mutableMapOf<String, Any>(
                    "name" to user.name.trim(),
                    "phone" to newPhone,
                    "email" to publicEmail(user.email),
                    "updatedAt" to System.currentTimeMillis()
                )
                if (authEmail.isNotBlank()) {
                    updates["authEmail"] = authEmail
                }

                userRef.update(updates).await()

                if (newPhone.isNotBlank() && authEmail.isNotBlank()) {
                    if (oldPhone.isNotBlank() && oldPhone != newPhone) {
                        runCatching { phoneLoginsCollection.document(oldPhone).delete().await() }
                    }
                    phoneLoginsCollection.document(newPhone).set(
                        mapOf(
                            "uid" to currentUser.uid,
                            "authEmail" to authEmail,
                            "email" to authEmail,
                            "phone" to newPhone,
                            "updatedAt" to System.currentTimeMillis()
                        )
                    ).await()
                }
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e, "Error updating profile: ${e.message}")
            }
        }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        auth.signOut()
        Result.Success(Unit)
    }

    private fun com.google.firebase.firestore.DocumentSnapshot.toAppUser(): User {
        val uid = getString("uid") ?: id
        val phone = getString("phone").orEmpty()
        val rawName = getString("name").orEmpty()
        return User(
            id = uid.toStableLongId(),
            name = publicName(rawName, phone),
            email = publicEmail(getString("email")),
            password = "",
            phone = phone,
            role = getString("role") ?: "USER",
            isBlocked = getBoolean("isBlocked") ?: false,
            createdAt = getLong("createdAt") ?: 0L
        )
    }

    private fun fallbackUser(uid: String, email: String, login: String): User {
        return User(
            id = uid.toStableLongId(),
            name = "Kh\u00e1ch",
            email = publicEmail(email),
            password = "",
            phone = login,
            role = "USER",
            isBlocked = false,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun String.toStableLongId(): Long {
        return fold(1125899906842597L) { hash, char -> 31 * hash + char.code }
            .let { if (it == Long.MIN_VALUE) 0L else kotlin.math.abs(it) }
    }

    private fun publicName(name: String, phone: String): String {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return ""
        if (phone.isNotBlank() && normalizePhone(trimmed) == normalizePhone(phone)) return ""
        if (isInternalAuthEmail(trimmed)) return ""
        return trimmed
    }

    private fun publicEmail(email: String?): String {
        val trimmed = email.orEmpty().trim()
        return if (isInternalAuthEmail(trimmed)) "" else trimmed
    }

    private fun isInternalAuthEmail(email: String): Boolean {
        return email.startsWith("phone-", ignoreCase = true)
                && email.endsWith("@busbooking.local", ignoreCase = true)
    }
}
