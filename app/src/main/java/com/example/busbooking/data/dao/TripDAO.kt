package com.example.busbooking.data.dao

import androidx.room.*
import com.example.busbooking.data.entity.Trip
import com.example.busbooking.data.relations.TripWithRouteAndBus
import kotlinx.coroutines.flow.Flow

@Dao
interface TripDAO {

    // ================= CRUD =================

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTrip(trip: Trip): Long

    @Update
    suspend fun updateTrip(trip: Trip)

    @Query("UPDATE trips SET status = 'CANCELLED' WHERE id = :tripId")
    suspend fun cancelTrip(tripId: Long)

    @Query("DELETE FROM trips WHERE id = :tripId")
    suspend fun deleteTrip(tripId: Long)

    // ================= USER =================

    /**
     * 🔥 Lấy chuyến theo route + ngày
     */
    @Transaction
    @Query("""
        SELECT t.* FROM trips t
        WHERE t.routeId = :routeId 
          AND t.tripDate = :tripDate 
          AND t.status = 'SCHEDULED'
        ORDER BY t.departureTime
    """)
    suspend fun getTripsForRouteAndDate(
        routeId: Long,
        tripDate: Long
    ): List<TripWithRouteAndBus>

    @Transaction
    @Query("SELECT * FROM trips WHERE id = :tripId")
    suspend fun getTripById(tripId: Long): TripWithRouteAndBus?

    /**
     * 🔥 Upcoming trips theo route
     */
    @Transaction
    @Query("""
        SELECT t.* FROM trips t
        WHERE t.routeId = :routeId
          AND t.tripDate >= :fromDate
          AND t.status = 'SCHEDULED'
        ORDER BY t.tripDate, t.departureTime
    """)
    suspend fun getUpcomingTripsForRoute(
        routeId: Long,
        fromDate: Long
    ): List<TripWithRouteAndBus>

    /**
     * 🔥 Upcoming trips global
     */
    @Transaction
    @Query("""
        SELECT t.* FROM trips t
        WHERE t.tripDate >= :fromDate
          AND t.status = 'SCHEDULED'
        ORDER BY t.tripDate, t.departureTime
        LIMIT :limit
    """)
    fun getUpcomingTrips(
        fromDate: Long,
        limit: Int = 20
    ): Flow<List<TripWithRouteAndBus>>

    // ================= ADMIN =================

    @Transaction
    @Query("""
        SELECT t.* FROM trips t
        WHERE t.status = 'SCHEDULED'
        ORDER BY t.tripDate DESC, t.departureTime
    """)
    fun getAllActiveTrips(): Flow<List<TripWithRouteAndBus>>

    @Transaction
    @Query("""
        SELECT t.* FROM trips t
        ORDER BY t.tripDate DESC, t.departureTime
    """)
    fun getAllTrips(): Flow<List<TripWithRouteAndBus>>

    // ================= SIMPLE =================

    @Query("""
        SELECT * FROM trips 
        WHERE tripDate = :date 
        ORDER BY departureTime
    """)
    suspend fun getTripsByDate(date: Long): List<Trip>

    @Query("SELECT COUNT(*) FROM trips WHERE status = 'SCHEDULED'")
    suspend fun getScheduledTripCount(): Long
}