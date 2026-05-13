package com.example.busbooking.data.model

import kotlinx.serialization.Serializable

/**
 * Production-ready Seat Layout JSON Models
 * Used for:
 * - Serializing/deserializing Bus.seatLayoutJson
 * - Frontend rendering
 * - Dynamic layout from backend API
 *
 * Example usage:
 * val layout = SeatLayoutJson.parse(bus.seatLayoutJson)
 * layout.floors.forEach { floor ->
 *     floor.sections.forEach { section ->
 *         section.rows.forEach { row ->
 *             row.seats.forEach { seat ->
 *                 // Render seat UI
 *             }
 *         }
 *     }
 * }
 */

@Serializable
data class SeatLayoutJson(
    val busId: Long,
    val busTotalSeats: Int,
    val busType: BusType = BusType.LIMOUSINE_34,  // LIMOUSINE_34, LIMOUSINE_40, etc.
    val floors: List<FloorLayout>
) {
    companion object {
        /**
         * Parse JSON string to SeatLayoutJson object
         */
        fun parse(jsonString: String?): SeatLayoutJson? {
            if (jsonString.isNullOrEmpty()) return null
            return try {
                // Using Gson or kotlinx.serialization
                kotlinx.serialization.json.Json.decodeFromString(jsonString)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Convert to JSON string
         */
        fun toJsonString(layout: SeatLayoutJson): String {
            return kotlinx.serialization.json.Json.encodeToString(layout)
        }
    }
}

@Serializable
enum class BusType {
    LIMOUSINE_34,    // 34 seats, 2 floors
    LIMOUSINE_40,    // 40 seats, 2 floors
    STANDARD_45,     // 45 seats, regular bus
}

@Serializable
data class FloorLayout(
    val floorNumber: Int,           // 1 or 2
    val floorLabel: String,         // "Tầng 1", "Tầng 2"
    val totalSeatsOnFloor: Int,     // 17
    val sections: List<SectionLayout>
)

@Serializable
data class SectionLayout(
    val sectionId: String,          // "A", "B", "C"
    val rows: List<RowLayout>
)

@Serializable
data class RowLayout(
    val rowIndex: Int,              // 0-indexed: 0, 1, 2, ...
    val rowNumber: String,          // "Row 1", "Row 2"
    val seats: List<SeatLayout>
)

@Serializable
data class SeatLayout(
    val id: Long = 0,
    val seatNumber: String,         // "A1", "A2", "B1", etc.
    val floor: Int,
    val rowIndex: Int,
    val columnIndex: Int,
    val isWindow: Boolean = false,
    val isAisle: Boolean = false,
    val seatType: String = "NORMAL", // NORMAL, PREMIUM, DISABLED, AISLE_PASSAGE
    val isBooked: Boolean = false,
    val isLocked: Boolean = false   // Real-time seat locking
)

@Serializable
data class SeatGridPosition(
    val rowIndex: Int,
    val columnIndex: Int,
    val type: String   // SEAT, AISLE, SPACE
)

/**
 * Helper object to generate seat layouts
 */
object SeatLayoutGenerator {

    /**
     * Generate 34-seat limousine layout (2 floors, 17 seats each)
     * Layout pattern:
     * Floor 1:
     *   1  2
     *   3  4  5
     *   6  7  8
     *   ...
     *   16 17
     */
    fun generate34SeatLayout(busId: Long): SeatLayoutJson {
        val floors = mutableListOf<FloorLayout>()

        // Floor 1 (A) and Floor 2 (B)
        for (floorNum in 1..2) {
            val floorLabel = when (floorNum) {
                1 -> "Tầng 1"
                else -> "Tầng 2"
            }

            val floorLetter = when (floorNum) {
                1 -> 'A'
                else -> 'B'
            }

            val floorLayout = generateSingleFloor(
                floorNumber = floorNum,
                floorLabel = floorLabel,
                floorLetter = floorLetter.toString(),
                seatsPerFloor = 17
            )
            floors.add(floorLayout)
        }

        return SeatLayoutJson(
            busId = busId,
            busTotalSeats = 34,
            busType = BusType.LIMOUSINE_34,
            floors = floors
        )
    }

    /**
     * Generate 40-seat limousine layout (2 floors, 20 seats each)
     */
    fun generate40SeatLayout(busId: Long): SeatLayoutJson {
        val floors = mutableListOf<FloorLayout>()

        for (floorNum in 1..2) {
            val floorLabel = when (floorNum) {
                1 -> "Tầng 1"
                else -> "Tầng 2"
            }

            val floorLetter = when (floorNum) {
                1 -> 'A'
                else -> 'B'
            }

            val floorLayout = generateSingleFloor(
                floorNumber = floorNum,
                floorLabel = floorLabel,
                floorLetter = floorLetter.toString(),
                seatsPerFloor = 20
            )
            floors.add(floorLayout)
        }

        return SeatLayoutJson(
            busId = busId,
            busTotalSeats = 40,
            busType = BusType.LIMOUSINE_40,
            floors = floors
        )
    }

    /**
     * Generate single floor layout
     * Pattern:
     * Row 0: [Seat, Seat]           + [Aisle] + [Aisle]
     * Row 1: [Seat, Seat, Seat]     + [Aisle]
     * Row 2: [Seat, Seat, Seat]     + [Aisle]
     * ...
     */
    private fun generateSingleFloor(
        floorNumber: Int,
        floorLabel: String,
        floorLetter: String,
        seatsPerFloor: Int
    ): FloorLayout {
        val rows = mutableListOf<RowLayout>()
        var seatCounter = 1

        // Generate rows based on limousine layout
        // Pattern: 2, 3, 3, 3, 3, (rows repeat)
        val rowPattern = listOf(2, 3, 3, 3, 3) // 2+3+3+3+3 = 14 seats (pattern repeats)

        var rowIndex = 0
        while (seatCounter <= seatsPerFloor) {
            val seatsInThisRow = rowPattern[(rowIndex % rowPattern.size)]
            val seats = mutableListOf<SeatLayout>()

            if (seatsInThisRow == 2) {
                // 2-seat row: 1 window seat, aisle, 1 window seat
                seats.add(
                    SeatLayout(
                        seatNumber = "$floorLetter${seatCounter++}",
                        floor = floorNumber,
                        rowIndex = rowIndex,
                        columnIndex = 0,
                        isWindow = true,
                        seatType = "NORMAL",
                        isAisle = false
                    )
                )
                seats.add(
                    SeatLayout(
                        seatNumber = "$floorLetter${seatCounter++}",
                        floor = floorNumber,
                        rowIndex = rowIndex,
                        columnIndex = 2,  // Column 2 (skip column 1 for aisle)
                        isWindow = true,
                        seatType = "NORMAL",
                        isAisle = false
                    )
                )
            } else {
                // 3-seat row: 1 window, aisle, 2 window
                for (col in 0 until seatsInThisRow) {
                    seats.add(
                        SeatLayout(
                            seatNumber = "$floorLetter${seatCounter++}",
                            floor = floorNumber,
                            rowIndex = rowIndex,
                            columnIndex = col,
                            isWindow = (col == 0 || col == seatsInThisRow - 1),
                            seatType = "NORMAL",
                            isAisle = false
                        )
                    )
                }
            }

            rows.add(
                RowLayout(
                    rowIndex = rowIndex,
                    rowNumber = "Row ${rowIndex + 1}",
                    seats = seats
                )
            )

            rowIndex++
        }

        return FloorLayout(
            floorNumber = floorNumber,
            floorLabel = floorLabel,
            totalSeatsOnFloor = seatsPerFloor,
            sections = listOf(
                SectionLayout(
                    sectionId = floorLetter,
                    rows = rows
                )
            )
        )
    }

    /**
     * Generate grid positions for UI rendering
     * Useful for RecyclerView or GridLayout
     */
    fun generateSeatGrid(layout: SeatLayoutJson, floor: Int): List<List<SeatGridPosition>> {
        val floorLayout = layout.floors.find { it.floorNumber == floor } ?: return emptyList()
        val grid = mutableListOf<List<SeatGridPosition>>()

        floorLayout.sections.forEach { section ->
            section.rows.forEach { row ->
                val rowPositions = mutableListOf<SeatGridPosition>()
                row.seats.forEach { seat ->
                    rowPositions.add(
                        SeatGridPosition(
                            rowIndex = row.rowIndex,
                            columnIndex = seat.columnIndex,
                            type = if (seat.isAisle) "AISLE" else "SEAT"
                        )
                    )
                }
                grid.add(rowPositions)
            }
        }

        return grid
    }
}

