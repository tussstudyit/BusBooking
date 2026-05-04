package com.example.busbooking.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.busbooking.data.entity.User
import kotlinx.coroutines.flow.Flow

/**
 * UserDAO - Complete user management queries
 *
 * Supports:
 * - Registration: Insert with email uniqueness check
 * - Authentication: Login by email & password
 * - Profile: Get/update user info
 * - Admin: List users, change roles, search, block/unblock
 *
 * Conflict Strategy: IGNORE on insert (email unique at DB level)
 */
@Dao
interface UserDAO {

    // ========================================================================
    // REGISTRATION & AUTHENTICATION
    // ========================================================================

    /**
     * Register new user
     * Conflict: IGNORE if email exists (unique constraint at DB level)
     * Return: rowId if success, 0 if conflict
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun registerUser(user: User): Long

    /**
     * Check if email already registered
     */
    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    suspend fun emailExists(email: String): Boolean

    /**
     * Login: Get user by email and password
     */
    @Query("SELECT * FROM users WHERE email = :email AND password = :password LIMIT 1")
    suspend fun loginUser(email: String, password: String): User?

    // ========================================================================
    // USER PROFILE
    // ========================================================================

    /**
     * Get user by ID (LiveData for reactive binding)
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserById(userId: Long): LiveData<User>

    /**
     * Get user by ID (suspend for immediate access)
     */
    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserByIdSuspend(userId: Long): User?

    /**
     * Update user profile
     */
    @Update
    suspend fun updateUser(user: User)

    /**
     * Soft delete: Block user account
     */
    @Query("UPDATE users SET isBlocked = 1 WHERE id = :userId")
    suspend fun blockUser(userId: Long)

    /**
     * Unblock user account
     */
    @Query("UPDATE users SET isBlocked = 0 WHERE id = :userId")
    suspend fun unblockUser(userId: Long)

    // ========================================================================
    // ADMIN: USER MANAGEMENT
    // ========================================================================

    /**
     * Get all active users (paginated)
     */
    @Query("""
        SELECT * FROM users 
        WHERE isBlocked = 0 
        ORDER BY createdAt DESC 
        LIMIT :limit OFFSET :offset
    """)
    fun getAllUsers(limit: Int, offset: Int): Flow<List<User>>

    /**
     * Get users by role
     */
    @Query("SELECT * FROM users WHERE role = :role AND isBlocked = 0 ORDER BY createdAt DESC")
    fun getUsersByRole(role: String): Flow<List<User>>

    /**
     * Search users by name or email
     */
    @Query("""
        SELECT * FROM users 
        WHERE (name LIKE '%' || :searchQuery || '%' 
           OR email LIKE '%' || :searchQuery || '%')
          AND isBlocked = 0
        ORDER BY createdAt DESC
    """)
    fun searchUsers(searchQuery: String): Flow<List<User>>

    /**
     * Change user role
     */
    @Query("UPDATE users SET role = :newRole WHERE id = :userId")
    suspend fun changeUserRole(userId: Long, newRole: String)

    /**
     * Hard delete user and all cascaded data
     */
    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUser(userId: Long)

    /**
     * Get total user count
     */
    @Query("SELECT COUNT(*) FROM users WHERE isBlocked = 0")
    suspend fun getUserCount(): Long

    /**
     * Get user count by role
     */
    @Query("SELECT COUNT(*) FROM users WHERE role = :role AND isBlocked = 0")
    suspend fun getUserCountByRole(role: String): Long
}
