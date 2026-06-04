package com.example.busbooking.domain.repository

import com.example.busbooking.data.entity.User
import com.example.busbooking.domain.models.Result

interface IAuthRepository {
    suspend fun registerUser(name: String, email: String, password: String, phone: String): Result<Long>
    suspend fun loginUser(phone: String, password: String): Result<User>
    suspend fun getUserProfile(userId: Long): Result<User>
    suspend fun updateUserProfile(user: User): Result<Unit>
    suspend fun logout(): Result<Unit>
}

class AuthRepository : IAuthRepository {
    override suspend fun registerUser(name: String, email: String, password: String, phone: String): Result<Long> = try {
        val response: RegisterResponse = ApiClient.post("/api/mobile/auth/register", RegisterRequest(name, email, password, phone))
        Result.Success(response.id)
    } catch (e: Exception) {
        Result.Error(e, e.message ?: "KhÃ´ng thá»ƒ Ä‘Äƒng kÃ½")
    }

    override suspend fun loginUser(phone: String, password: String): Result<User> = try {
        val user: UserApi = ApiClient.post("/api/mobile/auth/login", LoginRequest(phone, password))
        Result.Success(user.toEntity())
    } catch (e: Exception) {
        Result.Error(e, e.message ?: "KhÃ´ng thá»ƒ Ä‘Äƒng nháº­p")
    }

    override suspend fun getUserProfile(userId: Long): Result<User> = try {
        val user: UserApi = ApiClient.get("/api/mobile/users/$userId")
        Result.Success(user.toEntity())
    } catch (e: Exception) {
        Result.Error(e, e.message ?: "KhÃ´ng thá»ƒ táº£i há»“ sÆ¡")
    }

    override suspend fun updateUserProfile(user: User): Result<Unit> = try {
        val updated: UserApi = ApiClient.put("/api/mobile/users/${user.id}", UpdateUserRequest(user.name, user.email, user.phone))
        Result.Success(Unit)
    } catch (e: Exception) {
        Result.Error(e, e.message ?: "KhÃ´ng thá»ƒ cáº­p nháº­t há»“ sÆ¡")
    }

    override suspend fun logout(): Result<Unit> = Result.Success(Unit)
}

