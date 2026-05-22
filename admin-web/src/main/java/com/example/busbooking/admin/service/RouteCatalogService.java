package com.example.busbooking.admin.service;

import com.example.busbooking.admin.model.ProvinceOption;
import com.example.busbooking.admin.model.RouteCatalogEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RouteCatalogService {
    private static final long DEFAULT_PRICE = 200_000L;

    private final List<ProvinceOption> provinces = List.of(
            new ProvinceOption("ha_noi", "Hà Nội"),
            new ProvinceOption("ninh_binh", "Ninh Bình"),
            new ProvinceOption("thai_nguyen", "Thái Nguyên"),
            new ProvinceOption("hai_phong", "Hải Phòng"),
            new ProvinceOption("quang_ninh", "Quảng Ninh"),
            new ProvinceOption("thanh_hoa", "Thanh Hóa"),
            new ProvinceOption("nghe_an", "Nghệ An"),
            new ProvinceOption("ha_tinh", "Hà Tĩnh"),
            new ProvinceOption("quang_binh", "Quảng Bình"),
            new ProvinceOption("quang_tri", "Quảng Trị"),
            new ProvinceOption("thua_thien_hue", "Thừa Thiên Huế"),
            new ProvinceOption("da_nang", "Đà Nẵng"),
            new ProvinceOption("hoi_an", "Hội An"),
            new ProvinceOption("quang_ngai", "Quảng Ngãi"),
            new ProvinceOption("binh_dinh", "Bình Định"),
            new ProvinceOption("phu_yen", "Phú Yên"),
            new ProvinceOption("ninh_thuan", "Ninh Thuận"),
            new ProvinceOption("binh_thuan", "Bình Thuận"),
            new ProvinceOption("khanh_hoa", "Khánh Hòa"),
            new ProvinceOption("lam_dong", "Lâm Đồng"),
            new ProvinceOption("gia_lai", "Gia Lai"),
            new ProvinceOption("dak_lak", "Đắk Lắk"),
            new ProvinceOption("ho_chi_minh", "TP. Hồ Chí Minh"),
            new ProvinceOption("binh_duong", "Bình Dương"),
            new ProvinceOption("binh_phuoc", "Bình Phước"),
            new ProvinceOption("dong_nai", "Đồng Nai"),
            new ProvinceOption("ba_ria_vung_tau", "Bà Rịa - Vũng Tàu")
    );

    private final Map<String, ProvinceOption> provinceById = buildProvinceById();
    private final Map<String, String> provinceIdByName = buildProvinceIdByName();
    private final Map<String, Long> prices = buildPrices();
    private final List<RouteCatalogEntry> routes = buildRoutes();

    public List<ProvinceOption> provinces() {
        return provinces;
    }

    public List<RouteCatalogEntry> routes() {
        return routes;
    }

    public String provinceName(String provinceId) {
        ProvinceOption province = provinceById.get(provinceId);
        if (province == null) {
            throw new IllegalArgumentException("Unknown province id: " + provinceId);
        }
        return province.name();
    }

    public Optional<String> provinceIdByName(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(provinceIdByName.get(normalizeName(name)));
    }

    public Optional<RouteCatalogEntry> findRoute(String originId, String destinationId) {
        return routes.stream()
                .filter(route -> route.originId().equals(originId) && route.destinationId().equals(destinationId))
                .findFirst();
    }

    public long price(String originId, String destinationId) {
        return prices.getOrDefault(routeKey(originId, destinationId), DEFAULT_PRICE);
    }

    private Map<String, ProvinceOption> buildProvinceById() {
        Map<String, ProvinceOption> result = new HashMap<>();
        provinces.forEach(province -> result.put(province.id(), province));
        return Map.copyOf(result);
    }

    private Map<String, String> buildProvinceIdByName() {
        Map<String, String> result = new HashMap<>();
        provinces.forEach(province -> result.put(normalizeName(province.name()), province.id()));
        result.put(normalizeName("Thành phố Hồ Chí Minh"), "ho_chi_minh");
        return Map.copyOf(result);
    }

    private Map<String, Long> buildPrices() {
        Map<String, Long> result = new HashMap<>();
        addPrice(result, "ha_noi", "ninh_binh", 150_000);
        addPrice(result, "ha_noi", "thai_nguyen", 120_000);
        addPrice(result, "ha_noi", "hai_phong", 180_000);
        addPrice(result, "ha_noi", "quang_ninh", 220_000);
        addPrice(result, "ha_noi", "thanh_hoa", 230_000);
        addPrice(result, "ha_noi", "nghe_an", 320_000);
        addPrice(result, "ha_noi", "ha_tinh", 380_000);
        addPrice(result, "ha_noi", "quang_binh", 450_000);
        addPrice(result, "ha_noi", "quang_tri", 520_000);
        addPrice(result, "ha_noi", "thua_thien_hue", 580_000);
        addPrice(result, "ha_noi", "da_nang", 650_000);
        addPrice(result, "ha_noi", "hoi_an", 700_000);
        addPrice(result, "ha_noi", "quang_ngai", 780_000);
        addPrice(result, "ha_noi", "binh_dinh", 850_000);
        addPrice(result, "ha_noi", "phu_yen", 920_000);
        addPrice(result, "ha_noi", "khanh_hoa", 1_000_000);
        addPrice(result, "ha_noi", "ninh_thuan", 1_080_000);
        addPrice(result, "ha_noi", "binh_thuan", 1_180_000);
        addPrice(result, "ha_noi", "lam_dong", 1_250_000);
        addPrice(result, "ha_noi", "gia_lai", 1_150_000);
        addPrice(result, "ha_noi", "dak_lak", 1_100_000);
        addPrice(result, "ha_noi", "ho_chi_minh", 1_400_000);
        addPrice(result, "ha_noi", "binh_duong", 1_420_000);
        addPrice(result, "ha_noi", "dong_nai", 1_380_000);
        addPrice(result, "ha_noi", "ba_ria_vung_tau", 1_500_000);
        addPrice(result, "ninh_binh", "thai_nguyen", 220_000);
        addPrice(result, "ninh_binh", "hai_phong", 250_000);
        addPrice(result, "ninh_binh", "quang_ninh", 320_000);
        addPrice(result, "ninh_binh", "thanh_hoa", 120_000);
        addPrice(result, "ninh_binh", "nghe_an", 220_000);
        addPrice(result, "ninh_binh", "ha_tinh", 300_000);
        addPrice(result, "ninh_binh", "quang_binh", 380_000);
        addPrice(result, "ninh_binh", "quang_tri", 450_000);
        addPrice(result, "ninh_binh", "thua_thien_hue", 520_000);
        addPrice(result, "ninh_binh", "da_nang", 600_000);
        addPrice(result, "thai_nguyen", "hai_phong", 220_000);
        addPrice(result, "thai_nguyen", "quang_ninh", 240_000);
        addPrice(result, "thai_nguyen", "thanh_hoa", 300_000);
        addPrice(result, "thai_nguyen", "nghe_an", 420_000);
        addPrice(result, "hai_phong", "quang_ninh", 120_000);
        addPrice(result, "hai_phong", "thanh_hoa", 320_000);
        addPrice(result, "hai_phong", "nghe_an", 420_000);
        addPrice(result, "hai_phong", "da_nang", 720_000);
        addPrice(result, "quang_ninh", "thanh_hoa", 350_000);
        addPrice(result, "quang_ninh", "nghe_an", 450_000);
        addPrice(result, "quang_ninh", "da_nang", 780_000);
        addPrice(result, "thanh_hoa", "nghe_an", 150_000);
        addPrice(result, "thanh_hoa", "ha_tinh", 250_000);
        addPrice(result, "thanh_hoa", "quang_binh", 320_000);
        addPrice(result, "thanh_hoa", "quang_tri", 380_000);
        addPrice(result, "thanh_hoa", "thua_thien_hue", 450_000);
        addPrice(result, "thanh_hoa", "da_nang", 550_000);
        addPrice(result, "nghe_an", "ha_tinh", 100_000);
        addPrice(result, "nghe_an", "quang_binh", 220_000);
        addPrice(result, "nghe_an", "quang_tri", 300_000);
        addPrice(result, "nghe_an", "thua_thien_hue", 380_000);
        addPrice(result, "nghe_an", "da_nang", 450_000);
        addPrice(result, "ha_tinh", "quang_binh", 180_000);
        addPrice(result, "ha_tinh", "quang_tri", 250_000);
        addPrice(result, "ha_tinh", "thua_thien_hue", 320_000);
        addPrice(result, "ha_tinh", "da_nang", 420_000);
        addPrice(result, "quang_binh", "quang_tri", 120_000);
        addPrice(result, "quang_binh", "thua_thien_hue", 220_000);
        addPrice(result, "quang_binh", "da_nang", 320_000);
        addPrice(result, "quang_tri", "thua_thien_hue", 130_000);
        addPrice(result, "quang_tri", "da_nang", 220_000);
        addPrice(result, "quang_tri", "hoi_an", 260_000);
        addPrice(result, "quang_tri", "quang_ngai", 380_000);
        addPrice(result, "thua_thien_hue", "da_nang", 150_000);
        addPrice(result, "thua_thien_hue", "hoi_an", 200_000);
        addPrice(result, "thua_thien_hue", "quang_ngai", 320_000);
        addPrice(result, "thua_thien_hue", "binh_dinh", 450_000);
        addPrice(result, "da_nang", "hoi_an", 120_000);
        addPrice(result, "da_nang", "quang_ngai", 220_000);
        addPrice(result, "da_nang", "binh_dinh", 350_000);
        addPrice(result, "da_nang", "phu_yen", 450_000);
        addPrice(result, "da_nang", "khanh_hoa", 550_000);
        addPrice(result, "da_nang", "ninh_thuan", 650_000);
        addPrice(result, "da_nang", "binh_thuan", 750_000);
        addPrice(result, "da_nang", "lam_dong", 650_000);
        addPrice(result, "da_nang", "gia_lai", 450_000);
        addPrice(result, "da_nang", "dak_lak", 500_000);
        addPrice(result, "da_nang", "ho_chi_minh", 850_000);
        addPrice(result, "da_nang", "binh_duong", 880_000);
        addPrice(result, "da_nang", "dong_nai", 840_000);
        addPrice(result, "da_nang", "ba_ria_vung_tau", 920_000);
        addPrice(result, "hoi_an", "quang_ngai", 180_000);
        addPrice(result, "hoi_an", "binh_dinh", 320_000);
        addPrice(result, "hoi_an", "phu_yen", 420_000);
        addPrice(result, "hoi_an", "khanh_hoa", 520_000);
        addPrice(result, "hoi_an", "lam_dong", 620_000);
        addPrice(result, "quang_ngai", "binh_dinh", 180_000);
        addPrice(result, "quang_ngai", "phu_yen", 320_000);
        addPrice(result, "quang_ngai", "khanh_hoa", 420_000);
        addPrice(result, "quang_ngai", "ninh_thuan", 520_000);
        addPrice(result, "quang_ngai", "binh_thuan", 650_000);
        addPrice(result, "quang_ngai", "ho_chi_minh", 850_000);
        addPrice(result, "binh_dinh", "phu_yen", 150_000);
        addPrice(result, "binh_dinh", "khanh_hoa", 300_000);
        addPrice(result, "binh_dinh", "ninh_thuan", 420_000);
        addPrice(result, "binh_dinh", "binh_thuan", 520_000);
        addPrice(result, "binh_dinh", "ho_chi_minh", 750_000);
        addPrice(result, "binh_dinh", "gia_lai", 220_000);
        addPrice(result, "phu_yen", "khanh_hoa", 140_000);
        addPrice(result, "phu_yen", "ninh_thuan", 250_000);
        addPrice(result, "phu_yen", "binh_thuan", 380_000);
        addPrice(result, "phu_yen", "ho_chi_minh", 650_000);
        addPrice(result, "khanh_hoa", "ninh_thuan", 120_000);
        addPrice(result, "khanh_hoa", "binh_thuan", 250_000);
        addPrice(result, "khanh_hoa", "lam_dong", 250_000);
        addPrice(result, "khanh_hoa", "ho_chi_minh", 500_000);
        addPrice(result, "ninh_thuan", "binh_thuan", 140_000);
        addPrice(result, "ninh_thuan", "lam_dong", 250_000);
        addPrice(result, "ninh_thuan", "ho_chi_minh", 420_000);
        addPrice(result, "binh_thuan", "lam_dong", 180_000);
        addPrice(result, "binh_thuan", "ho_chi_minh", 280_000);
        addPrice(result, "lam_dong", "gia_lai", 450_000);
        addPrice(result, "lam_dong", "dak_lak", 300_000);
        addPrice(result, "lam_dong", "ho_chi_minh", 320_000);
        addPrice(result, "lam_dong", "binh_duong", 350_000);
        addPrice(result, "lam_dong", "dong_nai", 280_000);
        addPrice(result, "gia_lai", "dak_lak", 220_000);
        addPrice(result, "gia_lai", "ho_chi_minh", 700_000);
        addPrice(result, "gia_lai", "binh_duong", 720_000);
        addPrice(result, "gia_lai", "dong_nai", 680_000);
        addPrice(result, "dak_lak", "ho_chi_minh", 650_000);
        addPrice(result, "dak_lak", "binh_duong", 680_000);
        addPrice(result, "dak_lak", "dong_nai", 620_000);
        addPrice(result, "ho_chi_minh", "binh_duong", 120_000);
        addPrice(result, "ho_chi_minh", "dong_nai", 130_000);
        addPrice(result, "ho_chi_minh", "binh_phuoc", 250_000);
        addPrice(result, "ho_chi_minh", "ba_ria_vung_tau", 180_000);
        addPrice(result, "binh_duong", "dong_nai", 120_000);
        addPrice(result, "binh_duong", "binh_phuoc", 180_000);
        addPrice(result, "binh_duong", "ba_ria_vung_tau", 250_000);
        addPrice(result, "dong_nai", "binh_phuoc", 220_000);
        addPrice(result, "dong_nai", "ba_ria_vung_tau", 150_000);
        addPrice(result, "binh_phuoc", "ba_ria_vung_tau", 350_000);
        return Map.copyOf(result);
    }

    private List<RouteCatalogEntry> buildRoutes() {
        List<RouteCatalogEntry> oneWay = List.of(
                route("da_nang", "hoi_an", 30, hours(1)),
                route("da_nang", "thua_thien_hue", 100, hours(2)),
                route("da_nang", "quang_ngai", 130, hours(2) + minutes(30)),
                route("da_nang", "binh_dinh", 300, hours(5)),
                route("da_nang", "khanh_hoa", 530, hours(9)),
                route("da_nang", "ho_chi_minh", 980, hours(16)),
                route("da_nang", "ha_noi", 765, hours(13)),
                route("da_nang", "gia_lai", 200, hours(4)),
                route("da_nang", "quang_tri", 165, hours(3)),
                route("ha_noi", "hai_phong", 120, hours(2)),
                route("ha_noi", "ninh_binh", 95, hours(2)),
                route("ha_noi", "thanh_hoa", 160, hours(3)),
                route("ha_noi", "nghe_an", 300, hours(5)),
                route("ha_noi", "quang_ninh", 160, hours(3)),
                route("ho_chi_minh", "binh_duong", 30, hours(1)),
                route("ho_chi_minh", "dong_nai", 35, hours(1) + minutes(30)),
                route("ho_chi_minh", "ba_ria_vung_tau", 125, hours(2)),
                route("ho_chi_minh", "binh_phuoc", 120, hours(2) + minutes(30)),
                route("ho_chi_minh", "lam_dong", 300, hours(5)),
                route("ho_chi_minh", "khanh_hoa", 440, hours(7)),
                route("ho_chi_minh", "binh_thuan", 200, hours(3) + minutes(30)),
                route("khanh_hoa", "lam_dong", 200, hours(4)),
                route("khanh_hoa", "binh_dinh", 240, hours(4)),
                route("lam_dong", "dong_nai", 200, hours(4)),
                route("lam_dong", "dak_lak", 200, hours(4)),
                route("binh_dinh", "gia_lai", 170, hours(3)),
                route("nghe_an", "ha_tinh", 50, hours(1))
        );

        List<RouteCatalogEntry> result = new ArrayList<>();
        oneWay.forEach(route -> {
            result.add(route);
            result.add(route(route.destinationId(), route.originId(), route.distanceKm(), route.durationMs()));
        });
        return List.copyOf(result);
    }

    private RouteCatalogEntry route(String originId, String destinationId, int distanceKm, long durationMs) {
        return new RouteCatalogEntry(
                originId,
                provinceName(originId),
                destinationId,
                provinceName(destinationId),
                distanceKm,
                durationMs,
                price(originId, destinationId)
        );
    }

    private void addPrice(Map<String, Long> target, String a, String b, long price) {
        target.put(routeKey(a, b), price);
        target.put(routeKey(b, a), price);
    }

    private String routeKey(String originId, String destinationId) {
        return originId + "->" + destinationId;
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    private long hours(long value) {
        return value * 3_600_000L;
    }

    private long minutes(long value) {
        return value * 60_000L;
    }
}
