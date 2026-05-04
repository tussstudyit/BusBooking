package com.example.busbooking.domain.repository

import com.example.busbooking.data.dao.UserDAO
import com.example.busbooking.data.entity.User
import com.example.busbooking.domain.models.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val userDAO: UserDAO) {

    /**
     * Get all active users with pagination
     *
     * @param limit Number of users per page (default 20)
     * @param offset Number of records to skip (default 0)
     */
    suspend fun getAllUsers(limit: Int = 20, offset: Int = 0): Result<List<User>> = 
        withContext(Dispatchers.IO) {
            try {
                // Using Flow - collect first value
                var userList: List<User> = emptyList()
                userDAO.getAllUsers(limit, offset).collect { users ->
                    userList = users
                }
                Result.Success(userList)
            } catch (e: Exception) {
                Result.Error(e, "Error fetching users: ${e.message}")
            }
        }

    /**
     * Block user account
     */
    suspend fun blockUser(userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userDAO.blockUser(userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Error blocking user: ${e.message}")
        }
    }

    /**
     * Unblock user account
     */
    suspend fun unblockUser(userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userDAO.unblockUser(userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Error unblocking user: ${e.message}")
        }
    }

    /**
     * Get users by role
     */
    suspend fun getUsersByRole(role: String): Result<List<User>> = 
        withContext(Dispatchers.IO) {
            try {
                var userList: List<User> = emptyList()
                userDAO.getUsersByRole(role).collect { users ->
                    userList = users
                }
                Result.Success(userList)
            } catch (e: Exception) {
                Result.Error(e, "Error fetching users by role: ${e.message}")
            }
        }

    /**
     * Change user role
     */
    suspend fun changeUserRole(userId: Long, newRole: String): Result<Unit> = 
        withContext(Dispatchers.IO) {
            try {
                userDAO.changeUserRole(userId, newRole)
                Result.Success(Unit)
            } catch (e: Exception) {
                Result.Error(e, "Error changing user role: ${e.message}")
            }
        }

    /**
     * Delete user permanently
     */
    suspend fun deleteUser(userId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            userDAO.deleteUser(userId)
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, "Error deleting user: ${e.message}")
        }
    }

    /**
     * Get user by ID (suspend version)
     */
    suspend fun getUserById(userId: Long): Result<User> = withContext(Dispatchers.IO) {
        try {
            val user = userDAO.getUserByIdSuspend(userId)
            if (user != null) {
                Result.Success(user)
            } else {
                Result.Error(Exception("Not found"), "User not found")
            }
        } catch (e: Exception) {
            Result.Error(e, "Error fetching user: ${e.message}")
        }
    }

    /**
     * Search users by name or email
     */
    suspend fun searchUsers(searchQuery: String): Result<List<User>> = 
        withContext(Dispatchers.IO) {
            try {
                var userList: List<User> = emptyList()
                userDAO.searchUsers(searchQuery).collect { users ->
                    userList = users
                }
                Result.Success(userList)
            } catch (e: Exception) {
                Result.Error(e, "Error searching users: ${e.message}")
            }
        }
}