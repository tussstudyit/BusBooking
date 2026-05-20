package com.example.busbooking.data.db

import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.busbooking.utils.PasswordHasher
import com.example.busbooking.data.model.SeatLayoutGenerator
import com.example.busbooking.data.model.SeatLayoutJson
import kotlinx.serialization.json.Json

object SeedDataProvider {

    // -------------------------------------------------------
    // Thời gian helper
    // -------------------------------------------------------
    private fun todayMidnight(): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    /** Trả về timestamp (ms) = ngày D kể từ hôm nay + giờ HH:mm */
    private fun ts(dayOffset: Int, hour: Int, minute: Int): Long {
        return todayMidnight() + dayOffset * 86_400_000L + hour * 3_600_000L + minute * 60_000L
    }

    /** Ngày D kể từ hôm nay (00:00) */
    private fun dayTs(dayOffset: Int): Long = todayMidnight() + dayOffset * 86_400_000L

    // -------------------------------------------------------
    // Bảng giá (VND) — đối xứng, tra cứu theo cặp tỉnh
    // -------------------------------------------------------
    private val PRICE_TABLE: Map<Pair<String,String>, Long> = buildMap {
        fun add(a: String, b: String, price: Long) {
            put(a to b, price); put(b to a, price)
        }
        // Hà Nội
        add("ha_noi","ninh_binh",        150_000)
        add("ha_noi","thai_nguyen",      120_000)
        add("ha_noi","hai_phong",        180_000)
        add("ha_noi","quang_ninh",       220_000)
        add("ha_noi","thanh_hoa",        230_000)
        add("ha_noi","nghe_an",          320_000)
        add("ha_noi","ha_tinh",          380_000)
        add("ha_noi","quang_binh",       450_000)
        add("ha_noi","quang_tri",        520_000)
        add("ha_noi","thua_thien_hue",   580_000)
        add("ha_noi","da_nang",          650_000)
        add("ha_noi","hoi_an",           700_000)
        add("ha_noi","quang_ngai",       780_000)
        add("ha_noi","binh_dinh",        850_000)
        add("ha_noi","phu_yen",          920_000)
        add("ha_noi","khanh_hoa",      1_000_000)
        add("ha_noi","ninh_thuan",     1_080_000)
        add("ha_noi","binh_thuan",     1_180_000)
        add("ha_noi","lam_dong",       1_250_000)
        add("ha_noi","gia_lai",        1_150_000)
        add("ha_noi","dak_lak",        1_100_000)
        add("ha_noi","ho_chi_minh",    1_400_000)
        add("ha_noi","binh_duong",     1_420_000)
        add("ha_noi","dong_nai",       1_380_000)
        add("ha_noi","ba_ria_vung_tau",1_500_000)
        // Miền Bắc nội vùng
        add("ninh_binh","thai_nguyen",   220_000)
        add("ninh_binh","hai_phong",     250_000)
        add("ninh_binh","quang_ninh",    320_000)
        add("ninh_binh","thanh_hoa",     120_000)
        add("ninh_binh","nghe_an",       220_000)
        add("ninh_binh","ha_tinh",       300_000)
        add("ninh_binh","quang_binh",    380_000)
        add("ninh_binh","quang_tri",     450_000)
        add("ninh_binh","thua_thien_hue",520_000)
        add("ninh_binh","da_nang",       600_000)
        add("thai_nguyen","hai_phong",   220_000)
        add("thai_nguyen","quang_ninh",  240_000)
        add("thai_nguyen","thanh_hoa",   300_000)
        add("thai_nguyen","nghe_an",     420_000)
        add("hai_phong","quang_ninh",    120_000)
        add("hai_phong","thanh_hoa",     320_000)
        add("hai_phong","nghe_an",       420_000)
        add("hai_phong","da_nang",       720_000)
        add("quang_ninh","thanh_hoa",    350_000)
        add("quang_ninh","nghe_an",      450_000)
        add("quang_ninh","da_nang",      780_000)
        // Bắc Trung Bộ
        add("thanh_hoa","nghe_an",       150_000)
        add("thanh_hoa","ha_tinh",       250_000)
        add("thanh_hoa","quang_binh",    320_000)
        add("thanh_hoa","quang_tri",     380_000)
        add("thanh_hoa","thua_thien_hue",450_000)
        add("thanh_hoa","da_nang",       550_000)
        add("nghe_an","ha_tinh",         100_000)
        add("nghe_an","quang_binh",      220_000)
        add("nghe_an","quang_tri",       300_000)
        add("nghe_an","thua_thien_hue",  380_000)
        add("nghe_an","da_nang",         450_000)
        add("ha_tinh","quang_binh",      180_000)
        add("ha_tinh","quang_tri",       250_000)
        add("ha_tinh","thua_thien_hue",  320_000)
        add("ha_tinh","da_nang",         420_000)
        add("quang_binh","quang_tri",    120_000)
        add("quang_binh","thua_thien_hue",220_000)
        add("quang_binh","da_nang",      320_000)
        add("quang_tri","thua_thien_hue",130_000)
        add("quang_tri","da_nang",       220_000)
        add("quang_tri","hoi_an",        260_000)
        add("quang_tri","quang_ngai",    380_000)
        add("thua_thien_hue","da_nang",  150_000)
        add("thua_thien_hue","hoi_an",   200_000)
        add("thua_thien_hue","quang_ngai",320_000)
        add("thua_thien_hue","binh_dinh",450_000)
        // Miền Trung
        add("da_nang","hoi_an",          120_000)
        add("da_nang","quang_ngai",      220_000)
        add("da_nang","binh_dinh",       350_000)
        add("da_nang","phu_yen",         450_000)
        add("da_nang","khanh_hoa",       550_000)
        add("da_nang","ninh_thuan",      650_000)
        add("da_nang","binh_thuan",      750_000)
        add("da_nang","lam_dong",        650_000)
        add("da_nang","gia_lai",         450_000)
        add("da_nang","dak_lak",         500_000)
        add("da_nang","ho_chi_minh",     850_000)
        add("da_nang","binh_duong",      880_000)
        add("da_nang","dong_nai",        840_000)
        add("da_nang","ba_ria_vung_tau", 920_000)
        add("hoi_an","quang_ngai",       180_000)
        add("hoi_an","binh_dinh",        320_000)
        add("hoi_an","phu_yen",          420_000)
        add("hoi_an","khanh_hoa",        520_000)
        add("hoi_an","lam_dong",         620_000)
        add("quang_ngai","binh_dinh",    180_000)
        add("quang_ngai","phu_yen",      320_000)
        add("quang_ngai","khanh_hoa",    420_000)
        add("quang_ngai","ninh_thuan",   520_000)
        add("quang_ngai","binh_thuan",   650_000)
        add("quang_ngai","ho_chi_minh",  850_000)
        add("binh_dinh","phu_yen",       150_000)
        add("binh_dinh","khanh_hoa",     300_000)
        add("binh_dinh","ninh_thuan",    420_000)
        add("binh_dinh","binh_thuan",    520_000)
        add("binh_dinh","ho_chi_minh",   750_000)
        add("binh_dinh","gia_lai",       220_000)
        add("phu_yen","khanh_hoa",       140_000)
        add("phu_yen","ninh_thuan",      250_000)
        add("phu_yen","binh_thuan",      380_000)
        add("phu_yen","ho_chi_minh",     650_000)
        add("khanh_hoa","ninh_thuan",    120_000)
        add("khanh_hoa","binh_thuan",    250_000)
        add("khanh_hoa","lam_dong",      250_000)
        add("khanh_hoa","ho_chi_minh",   500_000)
        add("ninh_thuan","binh_thuan",   140_000)
        add("ninh_thuan","lam_dong",     250_000)
        add("ninh_thuan","ho_chi_minh",  420_000)
        add("binh_thuan","lam_dong",     180_000)
        add("binh_thuan","ho_chi_minh",  280_000)
        // Tây Nguyên
        add("lam_dong","gia_lai",        450_000)
        add("lam_dong","dak_lak",        300_000)
        add("lam_dong","ho_chi_minh",    320_000)
        add("lam_dong","binh_duong",     350_000)
        add("lam_dong","dong_nai",       280_000)
        add("gia_lai","dak_lak",         220_000)
        add("gia_lai","ho_chi_minh",     700_000)
        add("gia_lai","binh_duong",      720_000)
        add("gia_lai","dong_nai",        680_000)
        add("dak_lak","ho_chi_minh",     650_000)
        add("dak_lak","binh_duong",      680_000)
        add("dak_lak","dong_nai",        620_000)
        // Miền Nam
        add("ho_chi_minh","binh_duong",  120_000)
        add("ho_chi_minh","dong_nai",    130_000)
        add("ho_chi_minh","binh_phuoc",  250_000)
        add("ho_chi_minh","ba_ria_vung_tau",180_000)
        add("binh_duong","dong_nai",     120_000)
        add("binh_duong","binh_phuoc",   180_000)
        add("binh_duong","ba_ria_vung_tau",250_000)
        add("dong_nai","binh_phuoc",     220_000)
        add("dong_nai","ba_ria_vung_tau",150_000)
        add("binh_phuoc","ba_ria_vung_tau",350_000)
    }

    private fun price(origin: String, dest: String): Double =
        (PRICE_TABLE[origin to dest] ?: 200_000L).toDouble()

    // -------------------------------------------------------
    // Thông tin tuyến: (originKey, originName, destKey, destName, distanceKm, durationMs)
    // -------------------------------------------------------
    data class RouteInfo(
        val originKey: String,
        val originName: String,
        val destKey: String,
        val destName: String,
        val distanceKm: Int,
        val durationMs: Long          // thời gian di chuyển ước tính
    )

    /**
     * Generate bidirectional routes automatically
     * Input: One-way routes (A->B)
     * Output: Bidirectional routes (A->B AND B->A) without duplicates
     *
     * Example:
     * Input:  [Đà Nẵng -> Hà Nội], [Hà Nội -> TP. Hồ Chí Minh]
     * Output: [Đà Nẵng -> Hà Nội, Hà Nội -> Đà Nẵng], [Hà Nội -> TP. Hồ Chí Minh, TP. Hồ Chí Minh -> Hà Nội]
     */
    private fun generateBidirectionalRoutes(oneWayRoutes: List<RouteInfo>): List<RouteInfo> {
        val result = mutableListOf<RouteInfo>()
        val seen = mutableSetOf<Pair<String, String>>()

        oneWayRoutes.forEach { route ->
            val key = route.originKey to route.destKey
            val reverseKey = route.destKey to route.originKey

            // Add forward route if not already present
            if (key !in seen) {
                result.add(route)
                seen.add(key)
            }

            // Add reverse route if not already present
            if (reverseKey !in seen) {
                result.add(
                    route.copy(
                        originKey = route.destKey,
                        originName = route.destName,
                        destKey = route.originKey,
                        destName = route.originName
                    )
                )
                seen.add(reverseKey)
            }
        }

        return result
    }

    // ONE_WAY routes - minimal definition, will be auto-generated bidirectionally
    private val ONE_WAY_ROUTES: List<RouteInfo> = listOf(
        // ── Đà Nẵng ──────────────────────────────────────────
        RouteInfo("da_nang",       "Đà Nẵng",          "hoi_an",         "Hội An",               30,    1*3600_000),
        RouteInfo("da_nang",       "Đà Nẵng",          "thua_thien_hue", "Thừa Thiên Huế",       100,   2*3600_000),
        RouteInfo("da_nang",       "Đà Nẵng",          "quang_ngai",     "Quảng Ngãi",           130,   2*3600_000+30*60_000),
        RouteInfo("da_nang",       "Đà Nẵng",          "binh_dinh",      "Bình Định",            300,   5*3600_000),
        RouteInfo("da_nang",       "Đà Nẵng",          "khanh_hoa",      "Khánh Hòa",            530,   9*3600_000),
        RouteInfo("da_nang",       "Đà Nẵng",          "ho_chi_minh",    "TP. Hồ Chí Minh",      980,  16*3600_000),
        RouteInfo("da_nang",       "Đà Nẵng",          "ha_noi",         "Hà Nội",               765,  13*3600_000),
        RouteInfo("da_nang",       "Đà Nẵng",          "gia_lai",        "Gia Lai",              200,   4*3600_000),
        RouteInfo("da_nang",       "Đà Nẵng",          "quang_tri",      "Quảng Trị",            165,   3*3600_000),
        // ── Hà Nội ───────────────────────────────────────────
        RouteInfo("ha_noi",        "Hà Nội",            "hai_phong",      "Hải Phòng",            120,   2*3600_000),
        RouteInfo("ha_noi",        "Hà Nội",            "ninh_binh",      "Ninh Bình",             95,   2*3600_000),
        RouteInfo("ha_noi",        "Hà Nội",            "thanh_hoa",      "Thanh Hóa",            160,   3*3600_000),
        RouteInfo("ha_noi",        "Hà Nội",            "nghe_an",        "Nghệ An",              300,   5*3600_000),
        RouteInfo("ha_noi",        "Hà Nội",            "quang_ninh",     "Quảng Ninh",           160,   3*3600_000),
        // ── TP. Hồ Chí Minh ──────────────────────────────────
        RouteInfo("ho_chi_minh",   "TP. Hồ Chí Minh",  "binh_duong",     "Bình Dương",            30,    1*3600_000),
        RouteInfo("ho_chi_minh",   "TP. Hồ Chí Minh",  "dong_nai",       "Đồng Nai",              35,   1*3600_000+30*60_000),
        RouteInfo("ho_chi_minh",   "TP. Hồ Chí Minh",  "ba_ria_vung_tau","Bà Rịa - Vũng Tàu",   125,   2*3600_000),
        RouteInfo("ho_chi_minh",   "TP. Hồ Chí Minh",  "binh_phuoc",     "Bình Phước",           120,   2*3600_000+30*60_000),
        RouteInfo("ho_chi_minh",   "TP. Hồ Chí Minh",  "lam_dong",       "Lâm Đồng",             300,   5*3600_000),
        RouteInfo("ho_chi_minh",   "TP. Hồ Chí Minh",  "khanh_hoa",      "Khánh Hòa",            440,   7*3600_000),
        RouteInfo("ho_chi_minh",   "TP. Hồ Chí Minh",  "binh_thuan",     "Bình Thuận",           200,   3*3600_000+30*60_000),
        // ── Khánh Hòa ────────────────────────────────────────
        RouteInfo("khanh_hoa",     "Khánh Hòa",         "lam_dong",       "Lâm Đồng",             200,   4*3600_000),
        RouteInfo("khanh_hoa",     "Khánh Hòa",         "binh_dinh",      "Bình Định",            240,   4*3600_000),
        // ── Lâm Đồng ─────────────────────────────────────────
        RouteInfo("lam_dong",      "Lâm Đồng",          "dong_nai",       "Đồng Nai",             200,   4*3600_000),
        RouteInfo("lam_dong",      "Lâm Đồng",          "dak_lak",        "Đắk Lắk",              200,   4*3600_000),
        // ── Bình Định ─────────────────────────────────────────
        RouteInfo("binh_dinh",     "Bình Định",          "gia_lai",        "Gia Lai",              170,   3*3600_000),
        // ── Nghệ An ──────────────────────────────────────────
        RouteInfo("nghe_an",       "Nghệ An",            "ha_tinh",        "Hà Tĩnh",               50,   1*3600_000),
    )

    // ROUTES: Auto-generated bidirectional routes from ONE_WAY_ROUTES
    private val ROUTES: List<RouteInfo> by lazy { generateBidirectionalRoutes(ONE_WAY_ROUTES) }

    // -------------------------------------------------------
    // Khung giờ xuất phát điển hình cho mỗi loại tuyến
    // -------------------------------------------------------
    /** Khung giờ (hour, minute) cho tuyến ngắn < 3h */
    private val SHORT_SLOTS = listOf(
        6 to 0, 7 to 0, 8 to 30, 10 to 0, 11 to 30,
        13 to 0, 14 to 30, 16 to 0, 17 to 30, 19 to 0
    )
    /** Khung giờ cho tuyến vừa 3–7h */
    private val MID_SLOTS = listOf(
        5 to 0, 6 to 30, 8 to 0, 10 to 0, 12 to 0,
        14 to 0, 16 to 0, 18 to 0, 20 to 0, 22 to 0
    )
    /** Khung giờ cho tuyến dài > 7h (nhiều chuyến đêm) */
    private val LONG_SLOTS = listOf(
        7 to 0, 9 to 0, 12 to 0, 15 to 0,
        18 to 0, 20 to 0, 22 to 0, 23 to 30
    )

    private fun slotsFor(durationMs: Long) = when {
        durationMs < 3 * 3600_000  -> SHORT_SLOTS
        durationMs < 7 * 3600_000  -> MID_SLOTS
        else                        -> LONG_SLOTS
    }

    // -------------------------------------------------------
    // Tên bus (chỉ dùng Tân Quang Dũng) + biển số giả
    // -------------------------------------------------------
    private val BUS_PLATES = listOf(
        "43A-12345", "43A-23456", "43A-34567", "43A-45678",
        "43A-56789", "43B-11111", "43B-22222", "43B-33333",
        "43B-44444", "43B-55555", "43C-10001", "43C-20002",
        "43C-30003", "43C-40004", "43C-50005"
    )

    // -------------------------------------------------------
    // Seed entry point
    // -------------------------------------------------------
    fun seedDatabase(db: SupportSQLiteDatabase) {
        try {
            seedUsers(db)
            val routeIds = seedRoutes(db)
            val busIds   = seedBuses(db)
            seedSeats(db, busIds)
            seedTrips(db, routeIds, busIds)
            seedTickets(db)
            println("✅ Database seeded successfully!")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // -------------------------------------------------------
    // Users
    // -------------------------------------------------------
    fun seedUsers(db: SupportSQLiteDatabase) {
        val adminPw = PasswordHasher.hash("123")
        val userPw  = PasswordHasher.hash("123")
        val now = System.currentTimeMillis()

        try {
            db.execSQL(
                "INSERT OR REPLACE INTO users (id,name,email,password,phone,role,isBlocked,createdAt) VALUES (?,?,?,?,?,?,?,?)",
                arrayOf<Any?>(1, "Admin", "admin@bus.com", adminPw, "0123456789", "ADMIN", 0, now)
            )
            println("✅ Admin user seeded: ID=1, Phone=0123456789, Role=ADMIN")

            db.execSQL(
                "INSERT OR REPLACE INTO users (id,name,email,password,phone,role,isBlocked,createdAt) VALUES (?,?,?,?,?,?,?,?)",
                arrayOf<Any?>(2, "Nguyễn Văn A", "user@gmail.com", userPw, "0987654321", "USER", 0, now)
            )

            db.execSQL(
                "INSERT OR REPLACE INTO users (id,name,email,password,phone,role,isBlocked,createdAt) VALUES (?,?,?,?,?,?,?,?)",
                arrayOf<Any?>(3, "Trần Thị B", "userb@gmail.com", userPw, "0912345678", "USER", 0, now)
            )
        } catch (e: Exception) {
            println("❌ Error seeding users: ${e.message}")
            e.printStackTrace()
        }
    }

    // -------------------------------------------------------
    // Routes  →  trả về Map<index, routeId>
    // -------------------------------------------------------
    fun seedRoutes(db: SupportSQLiteDatabase): Map<Int, Int> {
        val now = System.currentTimeMillis()
        val idMap = mutableMapOf<Int, Int>()
        ROUTES.forEachIndexed { idx, r ->
            val id = idx + 1
            idMap[idx] = id
            db.execSQL(
                "INSERT OR REPLACE INTO routes (id,origin,destination,distance,isActive,createdAt) VALUES (?,?,?,?,?,?)",
                arrayOf<Any?>(id, r.originName, r.destName, r.distanceKm, 1, now)
            )
        }
        return idMap
    }

    // -------------------------------------------------------
    // Buses  →  trả về List<busId>
    // -------------------------------------------------------
    fun seedBuses(db: SupportSQLiteDatabase): List<Int> {
        val now = System.currentTimeMillis()
        val ids = mutableListOf<Int>()
        BUS_PLATES.forEachIndexed { idx, plate ->
            val id = idx + 1
            ids += id

            // Generate 34-seat layout JSON
            val layoutJson = SeatLayoutGenerator.generate34SeatLayout(id.toLong())
            val layoutJsonString = Json.encodeToString(SeatLayoutJson.serializer(), layoutJson)

            db.execSQL(
                "INSERT OR REPLACE INTO buses (id,busName,totalSeats,licensePlate,seatLayoutJson,isActive,createdAt) VALUES (?,?,?,?,?,?,?)",
                arrayOf<Any?>(id, "Xe Tân Quang Dũng", 34, plate, layoutJsonString, 1, now)
            )
        }
        return ids
    }

    // -------------------------------------------------------
    // Seats - Production-like structure with floor, row, column
    // Layout: 34 seats per bus (17 per floor)
    // Floor 1 (A): 1, 2, 3, ... 17
    // Floor 2 (B): 1, 2, 3, ... 17
    // -------------------------------------------------------
    fun seedSeats(db: SupportSQLiteDatabase, busIds: List<Int>) {
        var seatId = 1L

        busIds.forEach { busId ->
            // Get the layout for this bus
            val layoutJson = SeatLayoutGenerator.generate34SeatLayout(busId.toLong())

            layoutJson.floors.forEach { floor ->
                floor.sections.forEach { section ->
                    section.rows.forEach { row ->
                        row.seats.forEach { seat ->
                            db.execSQL(
                                """INSERT OR REPLACE INTO seats 
                                   (id, busId, seatNumber, floor, rowIndex, columnIndex, 
                                    isWindow, isAisle, seatType, createdAt) 
                                   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)""",
                                arrayOf<Any?>(
                                    seatId,
                                    busId,
                                    seat.seatNumber,
                                    seat.floor,
                                    seat.rowIndex,
                                    seat.columnIndex,
                                    if (seat.isWindow) 1 else 0,
                                    if (seat.isAisle) 1 else 0,
                                    seat.seatType,
                                    System.currentTimeMillis()
                                )
                            )
                            seatId++
                        }
                    }
                }
            }
        }

        println("✅ Seeded $seatId seats (${busIds.size} buses × 34 seats)")
    }

    // -------------------------------------------------------
    // Trips  — 3 ngày tiếp theo, mỗi tuyến nhiều khung giờ
    // -------------------------------------------------------
    fun seedTrips(db: SupportSQLiteDatabase, routeIds: Map<Int, Int>, busIds: List<Int>) {
        val now     = System.currentTimeMillis()
        var tripId  = 1
        var busIdx  = 0   // round-robin qua fleet

        for (dayOffset in 0..5) {
            ROUTES.forEachIndexed { routeIdx, route ->
                val routeId  = routeIds[routeIdx] ?: return@forEachIndexed
                val slots    = slotsFor(route.durationMs)
                val tripPrice = price(route.originKey, route.destKey)

                slots.forEach { (hour, minute) ->
                    val depart  = ts(dayOffset, hour, minute)
                    val arrive  = depart + route.durationMs
                    val dayMidnight = dayTs(dayOffset)
                    val busId   = busIds[busIdx % busIds.size]
                    busIdx++

                    db.execSQL(
                        """INSERT OR REPLACE INTO trips
                            (id,routeId,busId,departureTime,arrivalTime,price,tripDate,status,createdAt)
                           VALUES (?,?,?,?,?,?,?,?,?)""",
                        arrayOf<Any?>(
                            tripId, routeId, busId,
                            depart, arrive,
                            tripPrice,
                            dayMidnight,
                            "SCHEDULED",
                            now
                        )
                    )
                    tripId++
                }
            }
        }
        println("✅ Seeded ${tripId - 1} trips across 3 days for ${ROUTES.size} routes.")
    }

    // -------------------------------------------------------
    // Sample tickets
    // -------------------------------------------------------
    private fun seedTickets(db: SupportSQLiteDatabase) {
        val now = System.currentTimeMillis()
        // Ghế 1 & 2 trên chuyến đầu tiên của ngày đầu tiên (tripId = 1)
        db.execSQL("INSERT OR REPLACE INTO tickets (id,userId,tripId,seatId,bookingTime,status) VALUES (?,?,?,?,?,?)",
            arrayOf<Any?>(1, 2, 1, 1, now, "CONFIRMED"))
        db.execSQL("INSERT OR REPLACE INTO tickets (id,userId,tripId,seatId,bookingTime,status) VALUES (?,?,?,?,?,?)",
            arrayOf<Any?>(2, 2, 1, 2, now, "CONFIRMED"))
        db.execSQL("INSERT OR REPLACE INTO tickets (id,userId,tripId,seatId,bookingTime,status) VALUES (?,?,?,?,?,?)",
            arrayOf<Any?>(3, 3, 2, 41, now, "CONFIRMED"))
    }
}