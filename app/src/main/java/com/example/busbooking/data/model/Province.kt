package com.example.busbooking.data.model

data class Province(
    val id: String,
    val name: String,
    val stops: List<Stop> = emptyList()
)

data class Stop(
    val id: String,
    val name: String,
    val province: String
)

/**
 * Singleton object chứa danh sách tỉnh thành phố Việt Nam
 * Được optimize để tìm kiếm nhanh
 */
object VietnameseProvinces {
    
    private val _provinces = listOf(
        // Miền Bắc
        Province(
            "ha_noi",
            "Hà Nội",
            listOf(
                Stop("stop_ha_noi_my_dinh",    "Bến xe Mỹ Đình",    "Hà Nội"),
                Stop("stop_ha_noi_giap_bat",   "Bến xe Giáp Bát",   "Hà Nội"),
                Stop("stop_ha_noi_nuoc_ngam",  "Bến xe Nước Ngầm",  "Hà Nội"),
                Stop("stop_ha_noi_gia_lam",    "Bến xe Gia Lâm",    "Hà Nội"),
                Stop("stop_ha_noi_yen_nghia",  "Bến xe Yên Nghĩa",  "Hà Nội"),
                Stop("stop_ha_noi_son_tay",    "Bến xe Sơn Tây",    "Hà Nội"),
                Stop("stop_ha_noi_troi",       "Bến xe Trôi",       "Hà Nội"),
                Stop("stop_ha_noi_phung",      "Bến xe Phùng",      "Hà Nội"),
                Stop("stop_ha_noi_kim_ma",     "Bến xe Kim Mã",     "Hà Nội")
            )
        ),
        Province(
            "ninh_binh",
            "Ninh Bình",
            listOf(
                Stop("stop_ninh_binh_trung_tam",   "Bến xe Ninh Bình",      "Ninh Bình"),
                Stop("stop_ninh_binh_tam_diep",    "Bến xe Tam Điệp",       "Ninh Bình"),
                Stop("stop_ninh_binh_kim_son",     "Bến xe Kim Sơn",        "Ninh Bình"),
                Stop("stop_ninh_binh_yen_khanh",   "Bến xe Yên Khánh",      "Ninh Bình"),
                Stop("stop_ninh_binh_nho_quan",    "Bến xe Nho Quan",       "Ninh Bình"),
                Stop("stop_ninh_binh_gian_khau",   "Bến xe Gián Khẩu",      "Ninh Bình")
            )
        ),

        Province(
            "thai_nguyen",
            "Thái Nguyên",
            listOf(
                Stop("stop_thai_nguyen_trung_tam",  "Bến xe Trung tâm Thái Nguyên", "Thái Nguyên"),
                Stop("stop_thai_nguyen_song_cong",  "Bến xe Sông Công",             "Thái Nguyên"),
                Stop("stop_thai_nguyen_dai_tu",     "Bến xe Đại Từ",                "Thái Nguyên"),
                Stop("stop_thai_nguyen_pho_yen",    "Bến xe Phổ Yên",               "Thái Nguyên"),
                Stop("stop_thai_nguyen_dong_hy",    "Bến xe Đồng Hỷ",               "Thái Nguyên"),
                Stop("stop_thai_nguyen_vo_nhai",    "Bến xe Võ Nhai",               "Thái Nguyên"),
                Stop("stop_thai_nguyen_phu_binh",   "Bến xe Phú Bình",              "Thái Nguyên"),
                Stop("stop_thai_nguyen_phu_luong",  "Bến xe Phú Lương",             "Thái Nguyên")
            )
        ),
        Province(
            "hai_phong",
            "Hải Phòng",
            listOf(
                Stop("stop_hai_phong_trung_tam",   "Bến xe Trung tâm Hải Phòng", "Hải Phòng"),
                Stop("stop_hai_phong_niem_nghia",  "Bến xe Niệm Nghĩa",          "Hải Phòng"),
                Stop("stop_hai_phong_vinh_niem",   "Bến xe Vĩnh Niệm",           "Hải Phòng"),
                Stop("stop_hai_phong_thuong_ly",   "Bến xe Thượng Lý",           "Hải Phòng"),
                Stop("stop_hai_phong_do_son",      "Bến xe Đồ Sơn",              "Hải Phòng"),
                Stop("stop_hai_phong_cat_ba",      "Bến xe Cát Bà",              "Hải Phòng"),
                Stop("stop_hai_phong_tien_lang",   "Bến xe Tiên Lãng",           "Hải Phòng"),
                Stop("stop_hai_phong_vinh_bao",    "Bến xe Vĩnh Bảo",            "Hải Phòng")
            )
        ),

        Province(
            "quang_ninh",
            "Quảng Ninh",
            listOf(
                Stop("stop_quang_ninh_bai_chay",    "Bến xe Bãi Cháy",            "Quảng Ninh"),
                Stop("stop_quang_ninh_cua_ong",     "Bến xe Cửa Ông",             "Quảng Ninh"),
                Stop("stop_quang_ninh_cam_pha",     "Bến xe Cẩm Phả",             "Quảng Ninh"),
                Stop("stop_quang_ninh_mong_cai",    "Bến xe Móng Cái",            "Quảng Ninh"),
                Stop("stop_quang_ninh_uong_bi",     "Bến xe Uông Bí",             "Quảng Ninh"),
                Stop("stop_quang_ninh_dong_trieu",  "Bến xe Đông Triều",          "Quảng Ninh"),
                Stop("stop_quang_ninh_quang_yen",   "Bến xe Quảng Yên",           "Quảng Ninh"),
                Stop("stop_quang_ninh_van_don",     "Bến xe Vân Đồn",             "Quảng Ninh")
            )
        ),

        Province(
            "thanh_hoa",
            "Thanh Hóa",
            listOf(
                Stop("stop_thanh_hoa_phia_bac",    "Bến xe phía Bắc Thanh Hóa",  "Thanh Hóa"),
                Stop("stop_thanh_hoa_phia_nam",    "Bến xe phía Nam Thanh Hóa",  "Thanh Hóa"),
                Stop("stop_thanh_hoa_bim_son",     "Bến xe Bỉm Sơn",             "Thanh Hóa"),
                Stop("stop_thanh_hoa_sam_son",     "Bến xe Sầm Sơn",             "Thanh Hóa"),
                Stop("stop_thanh_hoa_ngoc_lac",    "Bến xe Ngọc Lặc",            "Thanh Hóa"),
                Stop("stop_thanh_hoa_tinh_gia",    "Bến xe Tĩnh Gia",            "Thanh Hóa"),
                Stop("stop_thanh_hoa_tho_xuan",    "Bến xe Thọ Xuân",            "Thanh Hóa")
            )
        ),

        Province(
            "nghe_an",
            "Nghệ An",
            listOf(
                Stop("stop_nghe_an_vinh",          "Bến xe Vinh",                "Nghệ An"),
                Stop("stop_nghe_an_bac_vinh",      "Bến xe Bắc Vinh",            "Nghệ An"),
                Stop("stop_nghe_an_cua_lo",        "Bến xe Cửa Lò",              "Nghệ An"),
                Stop("stop_nghe_an_dien_chau",     "Bến xe Diễn Châu",           "Nghệ An"),
                Stop("stop_nghe_an_thai_hoa",      "Bến xe Thái Hòa",            "Nghệ An"),
                Stop("stop_nghe_an_quynh_luu",     "Bến xe Quỳnh Lưu",           "Nghệ An"),
                Stop("stop_nghe_an_do_luong",      "Bến xe Đô Lương",            "Nghệ An")
            )
        ),

        Province(
            "ha_tinh",
            "Hà Tĩnh",
            listOf(
                Stop("stop_ha_tinh_trung_tam",     "Bến xe Hà Tĩnh",             "Hà Tĩnh"),
                Stop("stop_ha_tinh_hong_linh",     "Bến xe Hồng Lĩnh",           "Hà Tĩnh"),
                Stop("stop_ha_tinh_ky_anh",        "Bến xe Kỳ Anh",              "Hà Tĩnh"),
                Stop("stop_ha_tinh_huong_son",     "Bến xe Hương Sơn",           "Hà Tĩnh"),
                Stop("stop_ha_tinh_nghi_xuan",     "Bến xe Nghi Xuân",           "Hà Tĩnh"),
                Stop("stop_ha_tinh_can_loc",       "Bến xe Can Lộc",             "Hà Tĩnh")
            )
        ),

        // Miền Trung
        Province(
            "da_nang",
            "Đà Nẵng",
            listOf(
                Stop("stop_da_nang_trung_tam",   "Bến xe Trung tâm Đà Nẵng", "Đà Nẵng"),
                Stop("stop_da_nang_duc_long",    "Bến xe Đức Long",          "Đà Nẵng")
            )
        ),

        Province(
            "thua_thien_hue",
            "Thừa Thiên Huế",
            listOf(
                Stop("stop_hue_phia_nam",        "Bến xe phía Nam Huế",      "Thừa Thiên Huế"),
                Stop("stop_hue_phia_bac",        "Bến xe phía Bắc Huế",      "Thừa Thiên Huế"),
                Stop("stop_hue_an_cuu",          "Bến xe An Cựu",            "Thừa Thiên Huế"),
                Stop("stop_hue_phu_bai",         "Bến xe Phú Bài",           "Thừa Thiên Huế"),
                Stop("stop_hue_lang_co",         "Bến xe Lăng Cô",           "Thừa Thiên Huế"),
                Stop("stop_hue_a_luoi",          "Bến xe A Lưới",            "Thừa Thiên Huế")
            )
        ),

        Province(
            "quang_tri",
            "Quảng Trị",
            listOf(
                Stop("stop_quang_tri_dong_ha",    "Bến xe Đông Hà",          "Quảng Trị"),
                Stop("stop_quang_tri_quang_tri",  "Bến xe Quảng Trị",        "Quảng Trị"),
                Stop("stop_quang_tri_lao_bao",    "Bến xe Lao Bảo",          "Quảng Trị"),
                Stop("stop_quang_tri_gio_linh",   "Bến xe Gio Linh",         "Quảng Trị"),
                Stop("stop_quang_tri_vinh_linh",  "Bến xe Vĩnh Linh",        "Quảng Trị")
            )
        ),

        Province(
            "quang_binh",
            "Quảng Bình",
            listOf(
                Stop("stop_quang_binh_dong_hoi",   "Bến xe Đồng Hới",        "Quảng Bình"),
                Stop("stop_quang_binh_ba_don",     "Bến xe Ba Đồn",          "Quảng Bình"),
                Stop("stop_quang_binh_hoan_lao",   "Bến xe Hoàn Lão",        "Quảng Bình"),
                Stop("stop_quang_binh_le_thuy",    "Bến xe Lệ Thủy",         "Quảng Bình"),
                Stop("stop_quang_binh_minh_hoa",   "Bến xe Minh Hóa",        "Quảng Bình")
            )
        ),

        Province(
            "hoi_an",
            "Hội An",
            listOf(
                Stop("stop_hoi_an_trung_tam",    "Bến xe Hội An",            "Hội An"),
                Stop("stop_hoi_an_dien_ban",     "Bến xe Điện Bàn",          "Hội An"),
                Stop("stop_hoi_an_cua_dai",      "Bến xe Cửa Đại",           "Hội An"),
                Stop("stop_hoi_an_tay_giang",    "Hội An - Tây Giang",       "Hội An")
            )
        ),

        Province(
            "quang_ngai",
            "Quảng Ngãi",
            listOf(
                Stop("stop_quang_ngai_trung_tam",  "Bến xe Quảng Ngãi",      "Quảng Ngãi"),
                Stop("stop_quang_ngai_duc_pho",    "Bến xe Đức Phổ",         "Quảng Ngãi"),
                Stop("stop_quang_ngai_sa_huynh",   "Bến xe Sa Huỳnh",        "Quảng Ngãi"),
                Stop("stop_quang_ngai_ly_son",     "Bến xe Lý Sơn",          "Quảng Ngãi"),
                Stop("stop_quang_ngai_binh_son",   "Bến xe Bình Sơn",        "Quảng Ngãi")
            )
        ),

        Province(
            "binh_dinh",
            "Bình Định",
            listOf(
                Stop("stop_binh_dinh_quy_nhon",    "Bến xe Quy Nhơn",        "Bình Định"),
                Stop("stop_binh_dinh_an_nhon",     "Bến xe An Nhơn",         "Bình Định"),
                Stop("stop_binh_dinh_phu_my",      "Bến xe Phù Mỹ",          "Bình Định"),
                Stop("stop_binh_dinh_tay_son",     "Bến xe Tây Sơn",         "Bình Định"),
                Stop("stop_binh_dinh_hoai_nhon",   "Bến xe Hoài Nhơn",       "Bình Định"),
                Stop("stop_binh_dinh_bong_son",    "Bến xe Bồng Sơn",        "Bình Định")
            )
        ),

        Province(
            "phu_yen",
            "Phú Yên",
            listOf(
                Stop("stop_phu_yen_tuy_hoa",       "Bến xe Tuy Hòa",         "Phú Yên"),
                Stop("stop_phu_yen_song_cau",      "Bến xe Sông Cầu",        "Phú Yên"),
                Stop("stop_phu_yen_tuy_an",        "Bến xe Tuy An",          "Phú Yên"),
                Stop("stop_phu_yen_dong_hoa",      "Bến xe Đông Hòa",        "Phú Yên"),
                Stop("stop_phu_yen_son_hoa",       "Bến xe Sơn Hòa",         "Phú Yên")
            )
        ),

        Province(
            "ninh_thuan",
            "Ninh Thuận",
            listOf(
                Stop("stop_ninh_thuan_phan_rang",   "Bến xe Phan Rang",              "Ninh Thuận"),
                Stop("stop_ninh_thuan_ninh_son",    "Bến xe Ninh Sơn",               "Ninh Thuận"),
                Stop("stop_ninh_thuan_ninh_hai",    "Bến xe Ninh Hải",               "Ninh Thuận"),
                Stop("stop_ninh_thuan_thap_cham",   "Bến xe Tháp Chàm",              "Ninh Thuận"),
                Stop("stop_ninh_thuan_ca_na",       "Bến xe Cà Ná",                  "Ninh Thuận")
            )
        ),

        Province(
            "binh_thuan",
            "Bình Thuận",
            listOf(
                Stop("stop_binh_thuan_phan_thiet",  "Bến xe Phan Thiết",             "Bình Thuận"),
                Stop("stop_binh_thuan_lagi",        "Bến xe La Gi",                  "Bình Thuận"),
                Stop("stop_binh_thuan_ham_tan",     "Bến xe Hàm Tân",                "Bình Thuận"),
                Stop("stop_binh_thuan_bac_binh",    "Bến xe Bắc Bình",               "Bình Thuận"),
                Stop("stop_binh_thuan_tuy_phong",   "Bến xe Tuy Phong",              "Bình Thuận"),
                Stop("stop_binh_thuan_phan_ri",     "Bến xe Phan Rí",                "Bình Thuận")
            )
        ),

        Province(
            "khanh_hoa",
            "Khánh Hòa",
            listOf(
                Stop("stop_khanh_hoa_nha_trang",   "Bến xe Nha Trang",       "Khánh Hòa"),
                Stop("stop_khanh_hoa_phia_nam",    "Bến xe phía Nam Nha Trang", "Khánh Hòa"),
                Stop("stop_khanh_hoa_cam_ranh",    "Bến xe Cam Ranh",        "Khánh Hòa"),
                Stop("stop_khanh_hoa_ninh_hoa",    "Bến xe Ninh Hòa",        "Khánh Hòa"),
                Stop("stop_khanh_hoa_van_gia",     "Bến xe Vạn Giã",         "Khánh Hòa")
            )
        ),

        // Tây Nguyên
        Province(
            "lam_dong",
            "Lâm Đồng",
            listOf(
                Stop("stop_lam_dong_da_lat",        "Bến xe Đà Lạt",              "Lâm Đồng"),
                Stop("stop_lam_dong_bao_loc",       "Bến xe Bảo Lộc",             "Lâm Đồng"),
                Stop("stop_lam_dong_duc_trong",     "Bến xe Đức Trọng",           "Lâm Đồng"),
                Stop("stop_lam_dong_di_linh",       "Bến xe Di Linh",             "Lâm Đồng"),
                Stop("stop_lam_dong_don_duong",     "Bến xe Đơn Dương",           "Lâm Đồng"),
                Stop("stop_lam_dong_lien_nghia",    "Bến xe Liên Nghĩa",          "Lâm Đồng"),
                Stop("stop_lam_dong_lac_duong",     "Bến xe Lạc Dương",           "Lâm Đồng")
            )
        ),

        Province(
            "gia_lai",
            "Gia Lai",
            listOf(
                Stop("stop_gia_lai_pleiku",         "Bến xe Đức Long Pleiku",     "Gia Lai"),
                Stop("stop_gia_lai_ankhe",          "Bến xe An Khê",              "Gia Lai"),
                Stop("stop_gia_lai_ayun_pa",        "Bến xe Ayun Pa",             "Gia Lai"),
                Stop("stop_gia_lai_chu_se",         "Bến xe Chư Sê",              "Gia Lai"),
                Stop("stop_gia_lai_chu_prong",      "Bến xe Chư Prông",           "Gia Lai"),
                Stop("stop_gia_lai_dak_doa",        "Bến xe Đắk Đoa",             "Gia Lai"),
                Stop("stop_gia_lai_mang_yang",      "Bến xe Mang Yang",           "Gia Lai")
            )
        ),

        Province(
            "dak_lak",
            "Đắk Lắk",
            listOf(
                Stop("stop_dak_lak_buon_ma_thuot",  "Bến xe phía Bắc Buôn Ma Thuột", "Đắk Lắk"),
                Stop("stop_dak_lak_phia_nam",       "Bến xe phía Nam Buôn Ma Thuột", "Đắk Lắk"),
                Stop("stop_dak_lak_ea_hleo",        "Bến xe Ea H'leo",               "Đắk Lắk"),
                Stop("stop_dak_lak_krong_nang",     "Bến xe Krông Năng",             "Đắk Lắk"),
                Stop("stop_dak_lak_buon_ho",        "Bến xe Buôn Hồ",                "Đắk Lắk"),
                Stop("stop_dak_lak_eakar",          "Bến xe Ea Kar",                 "Đắk Lắk"),
                Stop("stop_dak_lak_krong_pac",      "Bến xe Krông Pắc",              "Đắk Lắk")
            )
        ),

        // Miền Nam
        Province(
            "ho_chi_minh",
            "Thành phố Hồ Chí Minh",
            listOf(
                Stop("stop_hcm_mien_dong_cu",  "Bến xe Miền Đông",     "TP. Hồ Chí Minh"),
                Stop("stop_hcm_mien_dong_moi", "Bến xe Miền Đông Mới", "TP. Hồ Chí Minh"),
                Stop("stop_hcm_mien_tay",      "Bến xe Miền Tây",      "TP. Hồ Chí Minh"),
                Stop("stop_hcm_nga_tu_ga",     "Bến xe Ngã Tư Ga",     "TP. Hồ Chí Minh"),
                Stop("stop_hcm_an_suong",      "Bến xe An Sương",      "TP. Hồ Chí Minh"),
                Stop("stop_hcm_cho_lon",       "Bến xe Chợ Lớn",       "TP. Hồ Chí Minh"),
                Stop("stop_hcm_quan_8",        "Bến xe Quận 8",        "TP. Hồ Chí Minh"),
                Stop("stop_hcm_saigon",        "Bến xe Sài Gòn",       "TP. Hồ Chí Minh"),
            )
        ),

        Province(
            "binh_duong",
            "Bình Dương",
            listOf(
                Stop("stop_binh_duong_thu_dau_mot", "Bến xe Thủ Dầu Một",            "Bình Dương"),
                Stop("stop_binh_duong_ben_cat",     "Bến xe Bến Cát",                "Bình Dương"),
                Stop("stop_binh_duong_di_an",       "Bến xe Dĩ An",                  "Bình Dương"),
                Stop("stop_binh_duong_thuan_an",    "Bến xe Thuận An",               "Bình Dương"),
                Stop("stop_binh_duong_tan_uyen",    "Bến xe Tân Uyên",               "Bình Dương"),
                Stop("stop_binh_duong_bau_bang",    "Bến xe Bàu Bàng",               "Bình Dương")
            )
        ),

        Province(
            "binh_phuoc",
            "Bình Phước",
            listOf(
                Stop("stop_binh_phuoc_dong_xoai",   "Bến xe Đồng Xoài",              "Bình Phước"),
                Stop("stop_binh_phuoc_bu_dang",     "Bến xe Bù Đăng",                "Bình Phước"),
                Stop("stop_binh_phuoc_bu_dop",      "Bến xe Bù Đốp",                 "Bình Phước"),
                Stop("stop_binh_phuoc_chon_thanh",  "Bến xe Chơn Thành",             "Bình Phước"),
                Stop("stop_binh_phuoc_loc_ninh",    "Bến xe Lộc Ninh",               "Bình Phước")
            )
        ),

        Province(
            "dong_nai",
            "Đồng Nai",
            listOf(
                Stop("stop_dong_nai_bien_hoa",      "Bến xe Biên Hòa",               "Đồng Nai"),
                Stop("stop_dong_nai_trang_bom",     "Bến xe Trảng Bom",              "Đồng Nai"),
                Stop("stop_dong_nai_long_khanh",    "Bến xe Long Khánh",             "Đồng Nai"),
                Stop("stop_dong_nai_dinh_quan",     "Bến xe Định Quán",              "Đồng Nai"),
                Stop("stop_dong_nai_xuan_loc",      "Bến xe Xuân Lộc",               "Đồng Nai"),
                Stop("stop_dong_nai_nhon_trach",    "Bến xe Nhơn Trạch",             "Đồng Nai")
            )
        ),

        Province(
            "ba_ria_vung_tau",
            "Bà Rịa - Vũng Tàu",
            listOf(
                Stop("stop_brvt_vung_tau",  "Bến xe Vũng Tàu",  "Bà Rịa - Vũng Tàu"),
                Stop("stop_brvt_ba_ria",    "Bến xe Bà Rịa",    "Bà Rịa - Vũng Tàu"),
                Stop("stop_brvt_long_hai",  "Bến xe Long Hải",  "Bà Rịa - Vũng Tàu"),
                Stop("stop_brvt_xuyen_moc", "Bến xe Xuyên Mộc", "Bà Rịa - Vũng Tàu"),
                Stop("stop_brvt_con_dao",   "Bến xe Côn Đảo",   "Bà Rịa - Vũng Tàu"),
            )
        ),
    )

    // Public accessor - list này được load một lần duy nhất
    val provinces: List<Province>
        get() = _provinces

    // Cache cho search results (tối ưu performance)
    private val searchCache = mutableMapOf<String, List<Province>>()

    /**
     * Tìm kiếm tỉnh/điểm dừng theo query (case-insensitive)
     * Kết quả được cache để lần tìm kiếm tiếp theo nhanh hơn
     */
    fun searchProvinces(query: String): List<Province> {
        val trimmedQuery = query.trim()
        
        // Trả về toàn bộ list nếu query rỗng
        if (trimmedQuery.isEmpty()) {
            return _provinces
        }

        // Kiểm tra cache
        searchCache[trimmedQuery]?.let { return it }

        // Thực hiện tìm kiếm
        val results = _provinces.filter { province ->
            province.name.contains(trimmedQuery, ignoreCase = true) ||
            province.stops.any { stop ->
                stop.name.contains(trimmedQuery, ignoreCase = true) ||
                stop.province.contains(trimmedQuery, ignoreCase = true)
            }
        }

        // Lưu vào cache
        searchCache[trimmedQuery] = results
        return results
    }

    /**
     * Lấy tỉnh theo ID
     */
    fun getProvinceById(id: String): Province? {
        return _provinces.find { it.id == id }
    }

    /**
     * Lấy tỉnh theo tên
     */
    fun getProvinceByName(name: String): Province? {
        return _provinces.find { it.name == name }
    }

    /**
     * Lấy điểm dừng theo ID
     */
    fun getStopById(stopId: String): Stop? {
        return _provinces.asSequence()
            .flatMap { it.stops }
            .find { it.id == stopId }
    }

    /**
     * Clear cache (gọi khi app resume hoặc nếu cần refresh)
     */
    fun clearSearchCache() {
        searchCache.clear()
    }
}
