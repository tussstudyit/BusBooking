package com.example.busbooking.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.busbooking.data.entity.Bus
import com.example.busbooking.data.relations.BusWithSeats
import kotlinx.coroutines.flow.Flow

/**
 * BusDAO - Bus fleet management
 *
 * Supports:
 * - Get bus with all seats (for seat selection screen)
 * - Admin CRUD operations
 *
 * Conflict Strategy: IGNORE on insert (licensePlate unique at DB level)
 */
@Dao
interface BusDAO {

    // ========================================================================
    // INSERT/UPDATE/DELETE
    // ========================================================================

    /**
     * Register new bus
     * Conflict: IGNORE if licensePlate exists (unique constraint)
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBus(bus: Bus): Long

    /**
     * Update bus details
     */
    @Update
    suspend fun updateBus(bus: Bus)

    /**
     * Soft delete: Mark bus as inactive
     */
    @Query("UPDATE buses SET isActive = 0 WHERE id = :busId")
    suspend fun deactivateBus(busId: Long)

    /**
     * Hard delete bus (cascades delete seats and tickets)
     */
    @Query("DELETE FROM buses WHERE id = :busId")
    suspend fun deleteBus(busId: Long)

    // ========================================================================
    // USER: SEAT SELECTION
    // ========================================================================

    /**
     * Get bus with all its seats (for seat selection UI)
     *
     * @return Complete BusWithSeats relation object
     */
    @Transaction
    @Query("SELECT * FROM buses WHERE id = :busId")
    suspend fun getBusWithSeats(busId: Long): BusWithSeats?

    // ========================================================================
    // ADMIN: BUS MANAGEMENT
    // ========================================================================

    /**
     * Get bus by ID
     */
    @Query("SELECT * FROM buses WHERE id = :busId")
    suspend fun getBusById(busId: Long): Bus?

    /**
     * Get bus by ID (LiveData)
     */
    @Query("SELECT * FROM buses WHERE id = :busId")
    fun getBusbyIdLive(busId: Long): LiveData<Bus>

    /**
     * Get bus by license plate (unique lookup)
     */
    @Query("SELECT * FROM buses WHERE licensePlate = :licensePlate LIMIT 1")
    suspend fun getBusByLicensePlate(licensePlate: String): Bus?

    /**
     * Get all active buses
     */
    @Query("""
        SELECT * FROM buses 
        WHERE isActive = 1 
        ORDER BY busName
    """)
    fun getAllActiveBuses(): Flow<List<Bus>>

    /**
     * Get all buses including inactive (admin)
     */
    @Query("""
        SELECT * FROM buses 
        ORDER BY isActive DESC, busName
    """)
    fun getAllBuses(): Flow<List<Bus>>

    /**
     * Search buses by name or license plate
     */
    @Query("""
        SELECT * FROM buses 
        WHERE (busName LIKE '%' || :searchQuery || '%'
            OR licensePlate LIKE '%' || :searchQuery || '%')
          AND isActive = 1
        ORDER BY busName
    """)
    suspend fun searchBuses(searchQuery: String): List<Bus>

    /**
     * Get bus count
     */
    @Query("SELECT COUNT(*) FROM buses WHERE isActive = 1")
    suspend fun getActiveBusCount(): Long
}
