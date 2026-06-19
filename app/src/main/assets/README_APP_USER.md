# README App User - BusBooking

File nay duoc dat trong `app/src/main/assets` de hien truc tiep trong Android Studio khi dang o che do view `Android`.

## 1. Vai Tro

`app` la ung dung Android danh cho khach hang `USER`.

Muc tieu:

- Dang ky va dang nhap tai khoan khach hang.
- Tim tuyen/chuyen xe theo diem di, diem den, ngay di.
- Loc chuyen theo loai xe 24 ghe hoac 34 ghe.
- Xem chi tiet chuyen.
- Chon ghe theo so do ghe backend tra ve.
- Dat mot hoac nhieu ghe.
- Dat ve mot chieu hoac khu hoi.
- Tao QR/link thanh toan VNPAY.
- Xem ve cua toi, lich su ve, chi tiet ve.
- Hien QR ve `BUSBOOKING-TICKET:<ticketId>` sau khi thanh toan thanh cong.
- Cap nhat ho so ca nhan.

## 2. Cau Truc Chinh

```text
app/src/main/java/com/example/busbooking/
  MainActivity.kt
  data/
    entity/       Model local: User, Route, Bus, Trip, Seat, Ticket
    relations/    Model gom quan he: TicketDetails, TripWithRouteAndBus
    session/      Luu session user
  domain/
    models/       Result, SeatDisplay, SeatReservationResult
    repository/   Goi API backend va map DTO
  presentation/
    ui/           Fragment man hinh
    viewmodel/    Xu ly state cho UI
    adapter/      RecyclerView adapter
  utils/          SessionManager, StatusLabels, UiState
```

## 3. Man Hinh Chinh

| Man hinh | Fragment | Chuc nang |
|---|---|---|
| Splash | `SplashFragment` | Kiem tra session va dieu huong |
| Dang nhap | `LoginFragment` | Login bang phone/password |
| Dang ky | `RegisterFragment` | Tao user moi |
| Trang chu | `HomeFragment` | Banner, uu dai, ve sap toi, form tim chuyen |
| Chon diem | `LocationPickerBottomSheet` | Chon diem di/den |
| Tuyen xe | `RouteSearchFragment` | Tim va xem tuyen |
| Danh sach chuyen | `TripListFragment` | Loc ngay, loc xe 24/34, chon chuyen |
| Chi tiet chuyen | `TripDetailsFragment` | Thong tin chuyen va nut chon ghe |
| Chon ghe | `SeatSelectionFragment` | So do ghe 2 tang, chon ghe, tinh tong tien |
| Xac nhan dat ve | `BookingConfirmationFragment` | QR/link VNPAY, poll trang thai payment |
| Ve cua toi | `MyTicketsFragment` | Ve sap toi va ve lich su |
| Chi tiet ve | `TicketDetailsFragment` | Thong tin ve va QR ve |
| Ho so | `UserProfileFragment` | Xem/cap nhat thong tin user |

## 4. Repository Va API

| Repository | API backend | Muc dich |
|---|---|---|
| `AuthRepository` | `/api/mobile/auth/**`, `/api/mobile/users/**` | Dang ky, dang nhap, ho so |
| `RouteRepository` | `/api/mobile/routes/**` | Diem di, diem den, tim tuyen |
| `TripRepository` | `/api/mobile/trips/**` | Tim chuyen, chi tiet, so ghe con |
| `ApiSeatRepository` | `/api/mobile/trips/{id}/seats`, `/api/mobile/tickets/book-batch` | Load ghe va dat ghe |
| `TicketRepository` | `/api/mobile/tickets/**` | Chi tiet ve, ve cua user, huy ve |
| `VnpayRepository` | `/api/payments/vnpay/**` | Tao QR/link, huy payment |

## 5. Luong Dat Ve Mot Chieu

```text
User dang nhap
-> Chon diem di, diem den, ngay di
-> TripListFragment goi API tim chuyen
-> TripDetailsFragment xem chi tiet
-> SeatSelectionFragment load ghe
-> User chon ghe
-> ApiSeatRepository goi /api/mobile/tickets/book-batch
-> Backend tao ticket PENDING_PAYMENT va payment CREATED
-> VnpayRepository tao QR/link thanh toan
-> BookingConfirmationFragment hien QR VNPAY
-> VNPAY callback backend
-> Ticket thanh CONFIRMED
-> App hien QR ve BUSBOOKING-TICKET:<ticketId>
```

## 6. Luong Dat Ve Khu Hoi

```text
Chon chuyen di
-> Chon ghe chieu di
-> App chuyen sang tim chuyen ve voi origin/destination dao nguoc
-> Chon ghe chieu ve
-> Goi /api/mobile/tickets/book-batch voi 2 segment
-> Backend tao nhieu ticket chung mot paymentId
-> User thanh toan mot lan cho tong tien
```

## 7. Quy Tac Ghe

- App khong hardcode so ghe.
- Ghe lay tu backend theo trip.
- Moi ghe co `floor`, `rowIndex`, `columnIndex`, `seatNumber`.
- App tach tang 1 va tang 2.
- Ghe booked thi khong cho chon.
- Tong tien = so ghe chon x gia chuyen.

## 8. Trang Thai Ve

| Status | Y nghia |
|---|---|
| `PENDING_PAYMENT` | Ve dang cho thanh toan |
| `CONFIRMED` | Ve da thanh toan, co QR ve |
| `CHECKED_IN` | Staff da check-in |
| `PAYMENT_FAILED` | Thanh toan that bai |
| `CANCELLED` | Ve da huy |

## 9. Cau Hinh Backend

Trong `local.properties`:

```properties
api.baseUrl=http://10.0.2.2:8081
```

- Emulator dung `10.0.2.2`.
- Dien thoai that dung IP LAN cua may chay backend, vi du `http://192.168.1.199:8081`.
