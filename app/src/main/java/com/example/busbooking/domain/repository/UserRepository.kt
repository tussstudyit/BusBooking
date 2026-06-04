package com.example.busbooking.domain.repository

import com.example.busbooking.data.entity.User
import com.example.busbooking.domain.models.Result

class UserRepository {
    suspend fun getUserById(userId: Long): Result<User> = AuthRepository().getUserProfile(userId)
    suspend fun getAllUsers(limit: Int = 20, offset: Int = 0): Result<List<User>> = Result.Error(UnsupportedOperationException(), "Quáº£n lÃ½ user thá»±c hiá»‡n trÃªn web admin")
    suspend fun blockUser(userId: Long): Result<Unit> = Result.Error(UnsupportedOperationException(), "Quáº£n lÃ½ user thá»±c hiá»‡n trÃªn web admin")
    suspend fun unblockUser(userId: Long): Result<Unit> = Result.Error(UnsupportedOperationException(), "Quáº£n lÃ½ user thá»±c hiá»‡n trÃªn web admin")
    suspend fun getUsersByRole(role: String): Result<List<User>> = Result.Error(UnsupportedOperationException(), "Quáº£n lÃ½ user thá»±c hiá»‡n trÃªn web admin")
    suspend fun changeUserRole(userId: Long, newRole: String): Result<Unit> = Result.Error(UnsupportedOperationException(), "Quáº£n lÃ½ user thá»±c hiá»‡n trÃªn web admin")
    suspend fun deleteUser(userId: Long): Result<Unit> = Result.Error(UnsupportedOperationException(), "Quáº£n lÃ½ user thá»±c hiá»‡n trÃªn web admin")
    suspend fun searchUsers(searchQuery: String): Result<List<User>> = Result.Error(UnsupportedOperationException(), "Quáº£n lÃ½ user thá»±c hiá»‡n trÃªn web admin")
}

