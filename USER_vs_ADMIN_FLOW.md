# User vs Admin Flow

Cap nhat: 2026-05-21, bo sung seat sync

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
  -> Tao tickets/payments/tripSeats PENDING_PAYMENT
  -> Goi admin-web xin VNPAY payment payload/QR
  -> Hien QR/link thanh toan
  -> admin-web callback cap nhat Firestore
```

Da sang Firebase:

- Login/register.
- Profile update.
- Route search.
- Trip list/details.
- Seat selection.
- Seat selection co fallback 34 ghe neu Firestore thieu/loi subcollection seats.
- UI ghe chi con: ghe trong, da ban, dang chon; khong hien trang thai dang giu.
- Chong dat trung trong transaction: `CONFIRMED/USED` luon khoa; `PENDING_PAYMENT` chi khoa khi con `holdExpiresAt`.
- Payment record va goi admin-web xin VNPAY URL + QR payload.
- My Tickets va Ticket Details doc ticket Firestore, fallback Room neu Firestore loi.

VNPAY hien tai:

- Admin-web tra `paymentUrl`, `qrContent`, `qrImageBase64`, `qrMimeType`, `amount`, `expiresAt`.
- Android hien QR do admin-web tra ve va mo link thanh toan that, khong con QR demo/local confirm.
- Chua test sandbox end-to-end bang public return/IPN URL.
- Chua co man reload payment/ticket status sau khi user quay lai app.

Con local/Room:

- Home upcoming tickets.
- Home popular routes.
- Booking History chua query lich su day du tat ca status.
- Cancel ticket user.

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
- VNPAY create/return/ipn tao signed URL + QR payload cho Android; chua test sandbox end-to-end bang public return/IPN URL.
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

1. Chuyen Home upcoming/popular routes sang Firestore.
2. Them reload payment/ticket status sau khi user quay lai app.
3. Test VNPAY sandbox end-to-end bang ngrok/public URL.
4. Them expire hold/payment.
5. Khoa Firestore rules.
6. Them admin audit/validate.
7. Xoa/tach legacy admin Android va Room neu khong dung offline cache.
