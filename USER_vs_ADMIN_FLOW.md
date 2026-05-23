# User vs Admin Flow

Cap nhat: 2026-05-22, bo sung VNPAY pending payment 5 phut va mo lai QR tu Ve cua toi

## Nguyen tac

- User dung Android app.
- Admin dung web rieng `admin-web/`.
- Firebase project hien tai: `busbooking-f44f162d`.
- Firestore la source of truth cho du lieu van hanh.
- Room trong Android chi con la local/legacy cho mot so man, chua nen coi la data chinh.
- Seat availability tren app va admin dung chung rule: chi `tripSeats.status` = `CONFIRMED` hoac `USED` moi tinh la da ban.

## User Android

```text
Login/Register bang so dien thoai
  -> Firebase Auth voi authEmail noi bo
  -> users/{uid} + phoneLogins/{phone}
  -> Home
  -> Search route/trip Firestore
  -> Trip details
  -> Seat selection Firestore
  -> Tao tickets PENDING_PAYMENT + payment CREATED
  -> Goi admin-web xin VNPAY payment payload/QR
  -> Hien QR/link thanh toan
  -> admin-web callback cap nhat Firestore
  -> Neu chua thanh toan: Ve cua toi -> bam ve cho thanh toan -> mo lai QR
  -> Neu qua 5 phut: tickets PAYMENT_FAILED, payment EXPIRED
```

Da sang Firebase:

- Login/register.
- Profile update.
- Route search.
- Trip list/details.
- Seat selection.
- Seat selection co fallback 34 ghe neu Firestore thieu/loi subcollection seats.
- UI ghe chi con: ghe trong, da ban, dang chon; khong hien trang thai dang giu.
- Chong dat trung trong transaction bang `tripSeats.CONFIRMED/USED`; `PENDING_PAYMENT` khong ghi vao `tripSeats` va khong khoa ghe.
- Payment record va goi admin-web xin VNPAY URL + QR payload.
- My Tickets va Ticket Details doc ticket Firestore, fallback Room neu Firestore loi.
- Home upcoming tickets doc cung nguon Firestore voi My Tickets.
- My Tickets: bam ve `PENDING/PENDING_PAYMENT` se mo lai man thanh toan, tao lai QR/link VNPAY neu payment con han.
- Man thanh toan co nut huy payment cho ve dang cho thanh toan.

VNPAY hien tai:

- Admin-web tra `paymentUrl`, `qrContent`, `qrImageBase64`, `qrMimeType`, `amount`, `expiresAt`.
- Android hien QR do admin-web tra ve va mo link thanh toan that, khong con QR demo/local confirm.
- Payment het han sau 5 phut tinh tu `payments.createdAt`.
- Qua han: ticket `PAYMENT_FAILED`, payment `EXPIRED`, bien khoi danh sach active.
- BookingConfirmation reload ticket status khi user quay lai app tu browser/VNPAY.
- Emulator dung `10.0.2.2:8081`; dien thoai that/IPN public van can ngrok/public URL.

Con local/Room:

- Home popular routes.
- Booking History chua query lich su day du tat ca status.
- Cancel/refund ve da thanh toan.

## Admin Web

```text
/login
  -> email hoac phone
  -> Firebase Auth REST
  -> users/{uid}.role = ADMIN
  -> Dashboard
  -> Routes / Buses / Trips / Users / Tickets / Payments
```

Da co:

- Login admin.
- Dashboard.
- Routes CRUD.
- Buses CRUD.
- Seats theo bus.
- Trips CRUD/cancel/list upcoming.
- Trip edit hien seat panel; ghe da ban to den theo `tripSeats.CONFIRMED/USED`, co fallback 34 ghe neu thieu seat layout.
- Users list/block.
- Tickets list.
- Payments list.
- VNPAY create/return/ipn tao signed URL + QR payload cho Android.
- VNPAY URL/QR het han sau 5 phut; admin-web dong payment qua han thanh `EXPIRED` va tickets thanh `PAYMENT_FAILED`.
- VNPAY success tao/ghi `tripSeats.CONFIRMED`; payment dang cho thanh toan khong ghi `tripSeats`.
- Routes/Buses/Trips da vao duoc va doc du lieu Firestore that.

Con thieu:

- Audit log.
- Validate nghiep vu khi cancel/disable/refund/block.
- Ticket detail/refund/cancel transaction.
- Payment detail/doi soat.
- Custom claims admin.
- Pagination/filter nang cao.
- Production security rules.

## Data hien tai

Seed nho tren `busbooking-f44f162d`:

```text
routes = 4
buses = 2
seats = 68
trips = 16
tickets = 0
payments = 0
tripSeats = 0
```

Tuyen dang giu:

- Ha Noi -> Da Nang
- Da Nang -> Ha Noi
- Da Nang -> TP. Ho Chi Minh
- TP. Ho Chi Minh -> Da Nang

## Viec nen lam tiep

1. Chuyen Home popular routes sang Firestore.
2. Khoa Firestore rules.
3. Them job cleanup pending payment qua han neu khong co user/admin cham vao record.
4. Booking History query day du tat ca status.
5. Test VNPAY bang dien thoai that/IPN public URL/ngrok.
6. Them admin audit/validate.
7. Xoa/tach legacy admin Android va Room neu khong dung offline cache.
