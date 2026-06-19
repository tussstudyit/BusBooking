# README App Staff - BusBooking

File nay duoc dat trong `staff-app/src/main/assets` de hien truc tiep trong Android Studio khi dang o che do view `Android`.

## 1. Vai Tro

`staff-app` la ung dung Android danh cho nhan vien nha xe `STAFF`.

Muc tieu:

- Dang nhap bang email Gmail va password cua tai khoan `role='STAFF'`.
- Xem trang chu tong quan chuyen duoc phan cong.
- Xem danh sach chuyen sap toi va lich su.
- Xem chi tiet chuyen, hanh khach, so do ghe.
- Quet QR ve cua khach.
- Xac minh ve hop le/khong hop le.
- Check-in hanh khach len xe.
- Cap nhat trang thai ve thanh `CHECKED_IN`.

## 2. Cau Truc Chinh

```text
staff-app/src/main/java/com/example/busbooking/staff/
  StaffMainActivity.kt
  data/
    api/
      StaffApiClient.kt
      StaffServerConfig.kt
    model/
      StaffModels.kt
    repository/
      StaffRepository.kt
  session/
    StaffSessionManager.kt
  ui/
    login/
    home/
    trip/
    ticket/
    profile/
  utils/
    StaffFormatters.kt
    StaffTripFilters.kt
    StaffViewModelFactory.kt
```

## 3. Man Hinh Chinh

| Man hinh | Fragment/Activity | Chuc nang |
|---|---|---|
| Dang nhap | `LoginFragment` | Nhap server URL, email Gmail, password |
| Trang chu | `HomeFragment` | Tong quan chuyen, so khach da dat/check-in |
| Danh sach chuyen | `TripListFragment` | Chuyen sap toi va lich su |
| Chi tiet chuyen | `TripDetailFragment` | Thong tin trip, hanh khach, so do ghe |
| Quet ve | `TicketScannerFragment` | Mo camera quet QR |
| Camera QR | `StaffQrCaptureActivity` | ZXing capture activity |
| Ket qua ve | `TicketResultFragment` | Hien ve hop le/khong hop le va nut check-in |
| Tai khoan | `ProfileFragment` | Thong tin staff va dang xuat |

## 4. Repository Va API

`StaffRepository` goi cac API backend:

| API | Muc dich |
|---|---|
| `POST /api/staff/auth/login` | Dang nhap staff |
| `GET /api/staff/home?staffId=` | Tong quan trang chu |
| `GET /api/staff/trips?staffId=` | Danh sach chuyen duoc gan |
| `GET /api/staff/trips/{tripId}?staffId=` | Chi tiet chuyen |
| `POST /api/staff/tickets/verify` | Xac minh QR ve |
| `POST /api/staff/tickets/{ticketId}/check-in` | Check-in ve |

## 5. Luong Dang Nhap

```text
Staff nhap server URL
-> Nhap email Gmail va password
-> LoginViewModel validate email @gmail.com
-> StaffRepository goi /api/staff/auth/login
-> Backend kiem tra role STAFF va is_blocked=false
-> Luu StaffUser vao StaffSessionManager
-> Dieu huong sang HomeFragment
```

## 6. Luong Xem Chuyen

```text
HomeFragment goi /api/staff/home
-> Hien so chuyen, khach da dat, khach da check-in
-> TripListFragment goi /api/staff/trips
-> StaffTripFilters chia upcomingTrips va historyTrips
-> TripDetailFragment goi /api/staff/trips/{tripId}
-> Hien hanh khach va so do ghe
```

Staff chi xem duoc chuyen co record active trong `trip_staff_assignments`.

## 7. Luong Quet QR Va Check-in

```text
Staff mo man Quet ve
-> Camera ZXing doc QR
-> TicketScannerViewModel goi /api/staff/tickets/verify
-> Backend lay ticketId tu QR
-> Kiem tra ticket ton tai, staff co quyen voi trip, status ve
-> TicketResultFragment hien ket qua
-> Neu ve hop le, staff bam check-in
-> Goi /api/staff/tickets/{ticketId}/check-in
-> Backend cap nhat ticket CHECKED_IN, trip_seats CHECKED_IN, ticket_checkins
```

## 8. QR Hop Le

Backend co the doc cac dang QR:

- `BUSBOOKING-TICKET:<ticketId>`
- `TICKET:<ticketId>`
- Query co `ticketId=...`
- Payment reference neu map duoc ve ticket.

Ve hop le de check-in phai co status:

- `CONFIRMED`
- `CHECKED_IN`

## 9. Trang Thai Ghe

| Status | Y nghia |
|---|---|
| `AVAILABLE` | Ghe trong |
| `BOOKED` | Ghe da co ve pending/confirmed |
| `CHECKED_IN` | Hanh khach da check-in |

## 10. Cau Hinh Backend

Trong `local.properties`:

```properties
staff.api.baseUrl=http://10.0.2.2:8081
```

- Emulator dung `10.0.2.2`.
- Dien thoai that dung IP LAN cua may chay backend.
- Staff app cho phep nhap server URL ngay tren man dang nhap.
