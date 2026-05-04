package com.example.busbooking.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.busbooking.data.entity.Route
import com.example.busbooking.data.relations.RouteWithTrips
import kotlinx.coroutines.flow.Flow

/**
 * RouteDAO - Route search and management
 *
 * Supports:
 * - Route search by origin/destination (partial match)
 * - Get all origins/destinations
 * - Admin CRUD operations
 *
 * Conflict Strategy: IGNORE on insert (potential duplicate handling)
 */
@Dao
interface RouteDAO {

    // ========================================================================
    // INSERT/UPDATE/DELETE
    // ========================================================================

    /**
     * Create new route
     * Conflict: IGNORE if duplicate exists
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRoute(route: Route): Long

    /**
     * Update route details
     */
    @Update
    suspend fun updateRoute(route: Route)

    /**
     * Soft delete: Mark route as inactive
     */
    @Query("UPDATE routes SET isActive = 0 WHERE id = :routeId")
    suspend fun deactivateRoute(routeId: Long)

    /**
     * Hard delete route (admin only)
     */
    @Query("DELETE FROM routes WHERE id = :routeId")
    suspend fun deleteRoute(routeId: Long)

    // ========================================================================
    // USER: SEARCH ROUTES
    // ========================================================================

    /**
     * Get all origins (unique list for UI dropdown)
     */
    @Query("""
        SELECT DISTINCT origin FROM routes 
        WHERE isActive = 1 
        ORDER BY origin
    """)
    suspend fun getAllOrigins(): List<String>

    /**
     * Get all destinations (unique list for UI dropdown)
     */
    @Query("""
        SELECT DISTINCT destination FROM routes 
        WHERE isActive = 1 
        ORDER BY destination
    """)
    suspend fun getAllDestinations(): List<String>

    /**
     * Search routes: origin/destination with partial match
     */
    @Query("""
        SELECT * FROM routes 
        WHERE isActive = 1
          AND (origin LIKE '%' || :searchQuery || '%'
            OR destination LIKE '%' || :searchQuery || '%')
        ORDER BY origin, destination
    """)
    suspend fun searchRoutes(searchQuery: String): List<Route>

    /**
     * Get route by exact origin and destination
     */
    @Query("""
        SELECT * FROM routes 
        WHERE origin = :origin 
          AND destination = :destination 
          AND isActive = 1
        LIMIT 1
    """)
    suspend fun getRouteByOriginDestination(origin: String, destination: String): Route?

    // ========================================================================
    // ADMIN: ROUTE MANAGEMENT
    // ========================================================================

    /**
     * Get route by ID
     */
    @Query("SELECT * FROM routes WHERE id = :routeId")
    suspend fun getRouteById(routeId: Long): Route?

    /**
     * Get route by ID (LiveData)
     */
    @Query("SELECT * FROM routes WHERE id = :routeId")
    fun getRouteByIdLive(routeId: Long): LiveData<Route>

    /**
     * Get all active routes
     */
    @Query("""
        SELECT * FROM routes 
        WHERE isActive = 1 
        ORDER BY origin, destination
    """)
    fun getAllActiveRoutes(): Flow<List<Route>>

    /**
     * Get all routes including inactive (admin)
     */
    @Query("""
        SELECT * FROM routes 
        ORDER BY isActive DESC, origin, destination
    """)
    fun getAllRoutes(): Flow<List<Route>>

    /**
     * Get route with all its trips
     */
    @Transaction
    @Query("SELECT * FROM routes WHERE id = :routeId")
    suspend fun getRouteWithTrips(routeId: Long): RouteWithTrips?

    /**
     * Get route count
     */
    @Query("SELECT COUNT(*) FROM routes WHERE isActive = 1")
    suspend fun getActiveRouteCount(): Long
}
