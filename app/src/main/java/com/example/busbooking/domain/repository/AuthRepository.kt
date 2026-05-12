package com.example.busbooking.domain.repository

import com.example.busbooking.data.dao.UserDAO
import com.example.busbooking.data.entity.User
import com.example.busbooking.domain.models.Result
import com.example.busbooking.utils.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * AuthRepository - User authentication and account management
 *
 * Handles:
 * - Registration (email uniqueness check)
 * - Login (credential verification)
 * - Profile management
 *
 * Uses Dispatchers.IO for database operations (off main thread)
 */
interface IAuthRepository {
    suspend fun registerUser(name: String, email: String, password: String, phone: String): Result<Long>
    suspend fun loginUser(email: String, password: String): Result<User>
    suspend fun getUserProfile(userId: Long): Result<User>
    suspend fun updateUserProfile(user: User): Result<Unit>
    suspend fun logout(): Result<Unit>
}

/**
 * Implementation of AuthRepository
 *
 * @param userDAO Data access object for user queries
 */
class AuthRepository(private val userDAO: UserDAO) : IAuthRepository {

    /**
     * Register new user
     *
     * Process:
     * 1. Check if email already exists
     * 2. If exists, return Error("Email already registered")
     * 3. If not, create user with hashed password (in real app)
     * 4. Return rowId on success
     *
     * @return Result with userId if success, Error if failure
     */
    override suspend fun registerUser(
        name: String,
        email: String,
        password: String,
        phone: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // Check if email exists
            val exists = userDAO.emailExists(email)
            if (exists) {
                return@withContext Result.Error(
                    Exception("Email already registered"),
                    "This email is already associated with an account"
                )
            }

            // In production: hash password using BCrypt or Argon2
            val hashedPassword = PasswordHasher.hash(password)

            val user = User(
                name = name,
                email = email,
                password = hashedPassword,
                phone = phone,
                role = "USER", // Default role
                isBlocked = false,
                createdAt = System.currentTimeMillis()
            )

            val userId = userDAO.registerUser(user)
            return@withContext if (userId > 0) {
                Result.Success(userId)
            } else {
                Result.Error(
                    Exception("Registration failed"),
                    "Could not create account"
                )
            }
        } catch (e: Exception) {
            Result.Error(e, "Registration error: ${e.message}")
        }
    }

     /**
      * Login user
      *
      * Process:
      * 1. Query user by phone
      * 2. Verify password (in real app: compare hashed)
      * 3. Return user object if credentials match
      * 4. Return error if not found or password mismatch
      *
      * @return Result with User if success, Error if failure
      */
     override suspend fun loginUser(email: String, password: String): Result<User> = withContext(Dispatchers.IO) {
         try {
             // Query user by phone
             val user = userDAO.getUserByPhone(email)

            return@withContext if (user != null) {
                if (user.isBlocked) {
                    Result.Error(
                        Exception("Account blocked"),
                        "Your account has been blocked by admin"
                    )
                } else {
                    val storedPassword = user.password
                    val isVerified = if (PasswordHasher.isBcryptHash(storedPassword)) {
                        PasswordHasher.verify(password, storedPassword)
                    } else {
                        storedPassword == password
                    }

                    if (isVerified) {
                        if (!PasswordHasher.isBcryptHash(storedPassword)) {
                            val upgradedHash = PasswordHasher.hash(password)
                            userDAO.updatePassword(user.id, upgradedHash)
                        }
                        Result.Success(user)
                    } else {
                        Result.Error(
                            Exception("Invalid credentials"),
                            "Phone or password is incorrect"
                        )
                    }
                }
            } else {
                Result.Error(
                    Exception("Invalid credentials"),
                    "Phone or password is incorrect"
                )
            }
        } catch (e: Exception) {
            Result.Error(e, "Login error: ${'$'}{e.message}")
        }
    }

    /**
     * Get user profile by ID
     *
     * @return Result with User if found, Error if not found
     */
    override suspend fun getUserProfile(userId: Long): Result<User> = withContext(Dispatchers.IO) {
        try {
            val user = userDAO.getUserByIdSuspend(userId)
            return@withContext if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(
                    Exception("User not found"),
                    "User profile not found"
                )
            }
        } catch (e: Exception) {
            Result.Error(e, "Error fetching profile: ${e.message}")
        }
    }

    /**
     * Update user profile
     *
     * @return Result.Success if update succeeds, Error otherwise
     */
    override suspend fun updateUserProfile(user: User): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userDAO.updateUser(user)
            return@withContext Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Error updating profile: ${e.message}")
        }
    }

    /**
     * Logout (clear session in SessionManager)
     * Note: SessionManager clearing should be done in a service or ViewModel
     */
    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Session is managed in SessionManager, nothing to do in DAO
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Logout error: ${e.message}")
        }
    }
}
