# BusBooking Current Development Flow

Cap nhat: 2026-05-22, bo sung flow VNPAY pending payment 5 phut va mo lai QR tu Ve cua toi

Tai lieu nay la flow hien tai sau khi doc lai project Android app, admin-web, Firebase config, Functions va tools. Muc tieu la ghi ro cai gi da lam, cai gi chua lam, va nen tiep tuc theo thu tu nao.

## Ket luan nhanh

Project dang di theo huong dung:

- Android app la app cho user dat ve.
- Admin quan ly bang web rieng trong `admin-web/`.
- Firebase project chinh hien tai la `busbooking-f44f162d`.
- Firestore la nguon du lieu chinh cho routes, buses, seats, trips, tickets, payments, tripSeats.
- Room van con trong Android, hien dang lam local data cho mot so man cu va demo fallback.
- VNPAY da co luong tao payment URL va QR payload tu `admin-web`; Android hien QR PNG base64 do server tra ve, reload status khi user quay lai app, va co the mo lai QR tu ve dang cho thanh toan.
- Seat availability hien theo mot rule duy nhat tren app va admin: chi `tripSeats.status` = `CONFIRMED` hoac `USED` moi tinh la da ban/ghe den. `PENDING_PAYMENT` khong lam giam so ghe trong tren UI.
- Ve `PENDING_PAYMENT` het han sau 5 phut. Khi qua han, app/admin-web chuyen ticket sang `PAYMENT_FAILED`, payment sang `EXPIRED`, va ve bien khoi danh sach active.

## Cau truc project

```text
BusBooking/
  app/                 Android app Kotlin/XML cho user
  admin-web/           Spring Boot + Thymeleaf admin web
  functions/           Firebase Functions scaffold cho VNPAY/admin claim
  tools/               Script seed/reset/admin account Firestore
  firestore.rules      Firestore rules hien dang mo cho dev
  firestore.indexes.json
  firebase.json
  .firebaserc          default project: busbooking-f44f162d
```

## Firebase project va du lieu

Dang cau hinh ve project:

```text
busbooking-f44f162d
```

Cac file config dang tro ve `busbooking-f44f162d`:

- `app/google-services.json`
- `admin-web/src/main/resources/application.yml`
- `.firebaserc`
- `tools/firebase-admin-account.js`
- `tools/firebase-admin-data.js`
- `tools/firestore-seed/*.js`

Du lieu Firestore sau khi reset nho de tranh quota:

```text
routes: 4
  Ha Noi -> Da Nang
  Da Nang -> Ha Noi
  Da Nang -> TP. Ho Chi Minh
  TP. Ho Chi Minh -> Da Nang

buses: 2
seats: 68
trips: 16
tickets: 0
payments: 0
tripSeats: 0
```

`users` va `phoneLogins` duoc giu lai khi trim data. Admin chinh:

```text
email: admin@busbooking.com
password: Admin@123456
role: ADMIN
project: busbooking-f44f162d
```

## Flow Android user hien tai

```text
Splash
  -> Login/Register
  -> Home
  -> Chon diem di/diem den/ngay
  -> Trip list
  -> Trip details
  -> Seat selection
  -> Firestore transaction tao tickets PENDING_PAYMENT + payments CREATED
  -> admin-web tao VNPAY payment URL + QR payload
  -> App hien QR/link tu payload nhan ve
  -> Mo browser thanh toan neu co URL that
  -> VNPAY return/IPN ve admin-web
  -> Admin-web cap nhat payments/tickets/tripSeats
  -> Neu chua thanh toan: user vao Ve cua toi, bam ve cho thanh toan de mo lai QR
  -> Neu qua 5 phut: payment EXPIRED, tickets PAYMENT_FAILED
```

### Login/Register

Da lam:

- Login/register Android dung `FirebaseAuthRepository`.
- User login bang so dien thoai.
- Firebase Auth van can email/password, nen app tao email noi bo dang:

```text
phone-{phone}@busbooking.local
```

- `phoneLogins/{phone}` map so dien thoai sang `authEmail`.
- `users/{uid}` luu profile user:

```text
uid
name
email       public email, co the rong
authEmail   email noi bo dung cho Firebase Auth
phone
role
isBlocked
createdAt
updatedAt
```

- Session Android da tach `name`, `email`, `phone`; header/profile dung `name`, khong bi phone chiem name neu data dung.
- Admin Android khong con duoc dieu huong tu login/splash.

Chua lam:

- Splash hien van chay delay roi dua ve login, chua restore Firebase/Auth session de vao thang Home.
- Chua co forgot password/OTP phone auth that.
- Chua khoa schema Firestore de chan user tu sua role/status.

### Home

Da lam:

- Home UI co search, banner, upcoming ticket, popular route, promotion.
- Ten user lay tu `SessionManager`.
- Upcoming tickets tren Home doc cung nguon Firestore voi `Ve cua toi` qua `FirebaseTicketRepository`, fallback Room neu Firestore loi.
- Home tu refresh khi quay lai man hinh de ve vua thanh toan/huy/het han duoc cap nhat.

Chua lam:

- Popular routes tren Home van lay tu Room route data.
- Banner/promotion la data tinh trong app.

### Route/Trip search

Da lam:

- `RouteSearchFragment` dung `FirebaseRouteRepository`.
- `TripListFragment` va `TripDetailsFragment` dung `FirebaseTripRepository`.
- App doc Firestore collections:

```text
routes
trips
buses
tripSeats
```

- Trip details hien so ghe con trong qua `tripSeats`.

Chua lam:

- Query hien con fallback demo data neu Firestore loi/khong co data.
- Mot so query dang doc rong roi filter client-side de tranh index phuc tap, can toi uu neu data lon.

### Seat selection va booking hold

Da lam:

- `SeatSelectionFragment` dung `FirebaseSeatRepository`.
- Doc ghe tu `buses/{busId}/seats`; neu subcollection ghe thieu/loi thi dung fallback 34 ghe mac dinh de UI khong bi trang.
- Doc trang thai ghe tu `tripSeats`.
- App chi hien 3 trang thai UI:
  - ghe trong
  - da ban
  - dang chon
- Da bo trang thai UI "dang giu".
- Chi `CONFIRMED` va `USED` duoc map thanh `BOOKED`; cac status khac, bao gom `PENDING_PAYMENT`, khong lam den ghe tren UI.
- Tao checkout bang Firestore transaction. Hien tai transaction khong ghi `tripSeats` khi dang cho thanh toan; ghe chi duoc ghi vao `tripSeats` sau khi VNPAY thanh cong.

```text
tickets/{ticketDoc}
  status: PENDING_PAYMENT
  userId: Firebase uid
  userNumericId: app stable long id
  tripId, seatId, busId
  paymentId

payments/{paymentDoc}
  status: CREATED
  provider: VNPAY
  ticketDocumentIds[]
  tripSeatIds[] = []
  amount
  createdAt
  updatedAt
```

- Chan dat trung ghe neu `tripSeats` dang `CONFIRMED` hoac `USED`.
- `PENDING_PAYMENT` khong khoa ghe tren UI va khong giam so ghe trong; neu user khong thanh toan trong 5 phut thi tickets/payment het han.
- Multi-seat booking tao mot payment tong tien cho nhieu ticket.

Da lam them:

- Khi Firestore transaction loi, repository tra `Failure`, khong tao success demo payment id.
- Man chon ghe da sua UTF-8 de het loi font va co fallback 34 ghe khi Firebase khong tra seat layout.

Chua lam:

- Chua co job nen dinh ky tren server de cleanup toan bo payment qua han neu khong co user nao mo app/admin-web cham vao payment do.

### Booking confirmation, My Tickets, Ticket Details

Da lam:

- BookingConfirmation co the hien thong tin ticketIds/seatNumbers/paymentUrl vua nhan tu seat selection.
- BookingConfirmation hien QR PNG base64 do admin-web tra ve, kem nut mo URL thanh toan VNPAY.
- Da bo luong xac nhan demo/local khi khong tao duoc VNPAY URL.
- `MyTicketsFragment`, `BookingHistoryFragment`, `TicketDetailsFragment` co duong doc ticket tu Firestore qua `FirebaseTicketRepository`, fallback ve Room neu Firestore loi.
- Khi user quay lai app tu browser/VNPAY, BookingConfirmation reload ticket status tu Firestore.
- Neu ve dang `PENDING_PAYMENT` nam trong `Ve cua toi`, user bam vao ve se mo lai BookingConfirmation, app goi lai admin-web de tao QR/link VNPAY neu payment con han.
- Man thanh toan co nut huy. Huy thanh toan chuyen cac ticket cung `paymentId` sang `CANCELLED` va payment sang `CANCELLED`.
- Ve cho thanh toan qua 5 phut bi chuyen `PAYMENT_FAILED` va khong con hien trong danh sach active.

Chua lam:

- BookingHistory hien tai van dung chung active-ticket loader, chua co query lich su day du tat ca trang thai.
- Huy ve da thanh toan/refund user chua co flow Firestore day du.
- VNPAY local tu emulator dung `10.0.2.2:8081`; test tren dien thoai that/IPN public van can ngrok/public URL.

## Flow Admin Web hien tai

```text
/login
  -> Firebase Auth REST sign-in
  -> Neu login la phone: phoneLogins/{phone} -> authEmail
  -> Kiem tra users/{uid}.role == ADMIN va isBlocked != true
  -> Dashboard
  -> Routes / Buses / Trips / Users / Tickets / Payments
```

### Cau hinh admin-web

Admin web can file service account local:

```text
admin-web/config/firebase-service-account.json
```

File nay khong duoc commit. Web doc config qua:

```text
FIREBASE_PROJECT_ID
FIREBASE_SERVICE_ACCOUNT_PATH
FIREBASE_WEB_API_KEY
ADMIN_WEB_PORT
VNPAY_TMN_CODE
VNPAY_HASH_SECRET
VNPAY_RETURN_URL
```

### Da lam trong admin-web

- Spring Boot 3.3.5 + Java 21.
- Thymeleaf server-rendered UI.
- Spring Security form login.
- Firebase Admin SDK doc/ghi Firestore.
- Login bang email hoac so dien thoai.
- Fallback admin email `admin@busbooking.com` cho dev khi Firestore doc role loi.
- Dashboard thong ke co ban.
- Routes:
  - list
  - create
  - edit
  - active/inactive
  - combobox tinh/thanh
  - suggested price/duration
- Buses:
  - list
  - create
  - edit
  - active/inactive
  - generate seats
  - view seats
- Trips:
  - list upcoming trips
  - create
  - edit
  - cancel status
  - form route/bus
  - seat status panel trong edit trip
  - trip seat panel co fallback 34 ghe neu bus chua co seats subcollection
- Users:
  - list/search
  - block/unblock
- Tickets:
  - list co ban
- Payments:
  - list co ban
- VNPAY:
  - `POST /api/payments/vnpay/create?paymentId=...`
  - `GET /api/payments/vnpay/return`
  - `GET /api/payments/vnpay/ipn`
  - create tra `paymentId`, `paymentUrl`, `qrContent`, `qrImageBase64`, `qrMimeType`, `amount`, `expiresAt`
  - payment URL/QR het han sau 5 phut tinh tu `payments.createdAt`
  - neu payment qua han khi tao QR lai: payment `EXPIRED`, tickets `PAYMENT_FAILED`
  - verify HMACSHA512
  - success: payment SUCCESS, tickets CONFIRMED, tripSeats CONFIRMED
  - failed: payment FAILED, tickets PAYMENT_FAILED
  - callback den payment `EXPIRED`/`CANCELLED` duoc coi la closed, khong confirm ve
- Seat admin sync:
  - Trip list/detail admin dem ghe da ban bang `tripSeats` status `CONFIRMED/USED`
  - Trip edit admin to ghe den khi status `CONFIRMED/USED`
  - `PENDING_PAYMENT` khong hien la ghe da ban tren admin
- Da fix loi khong vao duoc Routes/Buses/Trips:
  - controller set `demoMode=false`
  - template khong con ep null boolean
  - Trips hien upcoming trips thay vi chi hom nay

Da verify runtime sau fix:

```text
/routes -> 200, 4 rows, DEMO=false
/buses  -> 200, 2 rows, DEMO=false
/trips  -> 200, 16 rows, DEMO=false
```

### Chua lam trong admin-web

- Chua co audit log cho thao tac admin.
- Chua co validate nghiep vu day du:
  - disable route khi co trip active
  - disable bus khi co trip active
  - cancel trip khi da co ticket/payment
  - block user dang co ticket active
  - cancel/refund ticket dung transaction
- Tickets chua co trang chi tiet/chuc nang refund/cancel dung Firestore batch/transaction.
- Payments chua co trang chi tiet/doi soat VNPAY day du.
- Chua co pagination/filter nang cao cho trips/tickets/payments.
- Fallback demo data van con trong list pages de dev; production nen hien error that hoac retry, khong dung demo.
- VNPAY local can public URL/ngrok de test return/IPN that voi sandbox.

## Firebase Functions hien tai

Da co trong `functions/index.js`:

- `createVnpayPaymentUrl`
- `vnpayIpnHandler`
- `vnpayReturnUrlHandler`
- `expirePendingPayments`
- `setAdminClaim`

Trang thai:

- Chua phai runtime chinh hien tai, Android dang goi admin-web.
- Functions VNPAY van xu ly theo model 1 ticket/1 seat (`ticketId`, `seatId`), trong khi Android/admin-web hien da ho tro multi-seat qua `ticketDocumentIds` va `tripSeatIds`.
- `expirePendingPayments` query `payments.holdExpiresAt`, nhung runtime hien tai dung rule 5 phut tu `payments.createdAt` trong Android/admin-web. Android hien khong ghi `holdExpiresAt`.

Can lam neu muon deploy Functions thay admin-web:

- Dong bo multi-seat/multi-ticket.
- Doi expire job sang rule `payments.createdAt + 5 phut`, hoac them field `expiresAt` khi tao payment.
- Cau hinh secrets VNPAY.
- Deploy va doi Android goi callable/HTTPS Functions.

## Firestore data model hien tai

```text
users/{uid}
phoneLogins/{phone}
routes/{routeId}
buses/{busId}
buses/{busId}/seats/{seatId}
trips/{tripId}
tickets/{ticketDocId}
payments/{paymentDocId}
tripSeats/{tripId}_{seatId}
```

### routes

```text
id
originId
destinationId
origin
destination
distance
suggestedPrice
durationMs
isActive
createdAt
```

### buses

```text
id
busName
totalSeats
licensePlate
seatLayoutJson
isActive
createdAt
```

### seats

```text
id
busId
seatNumber
floor
rowIndex
columnIndex
isWindow
isAisle
seatType
createdAt
```

### trips

```text
id
routeId
busId
departureTime
arrivalTime
price
tripDate
status
createdAt
```

### tickets

```text
id
userId              Firebase uid
userNumericId       Android stable long id
tripId
seatId
busId
paymentId
bookingTime
status
cancellationReason
refundAmount
refundStatus
createdAt
updatedAt
```

### payments

```text
id
ticketId
ticketIds[]
ticketDocumentIds[]
tripSeatIds[]
userId
userNumericId
tripId
seatId
seatIds[]
amount
provider
status
vnpTxnRef
vnpCreateDate
vnpExpireDate
expiresAt
paymentUrl
qrContent
vnpResponseCode
vnpTransactionStatus
vnpTransactionNo
vnpBankCode
vnpPayDate
createdAt
updatedAt
```

### tripSeats

```text
tripId
seatId
ticketId
ticketDocumentId
paymentId
userId
userNumericId
status
createdAt
updatedAt
```

Quy uoc status ghe:

```text
CONFIRMED, USED      -> da ban, giam availableSeats, UI to den
PENDING_PAYMENT      -> khong ghi vao tripSeats trong runtime hien tai
PAYMENT_FAILED/null  -> coi nhu ghe trong
```

## Rules va security

Hien tai `firestore.rules` dang mo:

```text
allow read, write: if true;
```

Trang thai nay chi phu hop dev/demo. Truoc khi public/test nghiem tuc can:

- User chi doc public route/bus/seat/trip active.
- User chi doc ticket/payment cua chinh minh.
- User khong duoc tu sua:
  - `role`
  - `isBlocked`
  - `ticket.status`
  - `payment.status`
  - `tripSeats.status`
- Ghi status thanh toan/ghe nen qua server Admin SDK.
- Admin nen dung custom claims `admin=true`, khong chi dua vao `users.role`.

## Tools va data reset

### Reset data nho

```powershell
cd C:\Users\ADMIN\AndroidStudioProjects\BusBooking\tools\firestore-seed
npm.cmd run reset-small
```

Lenh nay:

- Xoa `routes`, `buses`, seats subcollection, `trips`, `tickets`, `payments`, `tripSeats`.
- Giu `users`, `phoneLogins`.
- Seed lai 4 routes, 2 buses, 68 seats, 16 trips.

### Admin account

```powershell
node tools\firebase-admin-account.js
```

Lenh nay tao/cap nhat:

```text
admin@busbooking.com / Admin@123456
users/{uid}.role = ADMIN
```

## Lenh chay

### Build Android

```powershell
.\gradlew.bat :app:assembleDebug
```

### Run admin web

```powershell
$env:VNPAY_TMN_CODE="..."
$env:VNPAY_HASH_SECRET="..."
$env:VNPAY_RETURN_URL="http://10.0.2.2:8081/api/payments/vnpay/return"
.\gradlew.bat :admin-web:bootRun
```

Neu port 8081 ban:

```powershell
$env:ADMIN_WEB_PORT="18081"
.\gradlew.bat :admin-web:bootRun
```

### Build admin web

```powershell
.\gradlew.bat :admin-web:clean :admin-web:bootJar
```

Dung `clean` neu Gradle incremental build bi lech sau khi them/xoa nhieu file admin-web.

## Da lam

### Firebase/config

- Chuyen project ve `busbooking-f44f162d`.
- Xoa config lien quan project Firebase cu khoi source runtime.
- Them service account path cho admin-web.
- Them script reset data nho de tranh Firestore quota.
- Xoa mapping phone login cu bi tao nham va dong bo admin account `admin@busbooking.com`.

### Android

- Cau hinh Firebase Auth/Firestore.
- Login/register app dung Firebase Auth.
- Login bang so dien thoai thong qua `phoneLogins`.
- Tach `authEmail` noi bo va public `email`.
- Session luu dung `name`, `email`, `phone`.
- Profile update dung FirebaseAuthRepository.
- Route search dung Firestore.
- Trip list/details dung Firestore.
- Seat selection dung Firestore.
- Booking checkout dung Firestore transaction: tao tickets `PENDING_PAYMENT` va payment `CREATED`, khong ghi `tripSeats` cho trang thai cho thanh toan.
- Goi admin-web tao VNPAY URL + QR payload; BookingConfirmation hien QR server tra ve.
- MyTickets/TicketDetails doc ticket Firestore sau khi VNPAY callback cap nhat status.
- Home upcoming tickets doc Firestore cung nguon voi MyTickets.
- Ve dang cho thanh toan trong MyTickets mo lai QR/link VNPAY neu con han; co nut huy thanh toan.
- Pending payment qua 5 phut: tickets `PAYMENT_FAILED`, payment `EXPIRED`.
- Bo success demo fallback khi Firestore reserve seats loi.
- Admin route tu Android login/splash da bi chan.
- Bo trang thai ghe dang giu tren UI.
- Seat selection fallback 34 ghe khi thieu/loi seat layout tu Firestore.
- Dong bo rule ghe da ban giua trip list, trip detail, seat selection va trip admin: chi `CONFIRMED/USED`.
- Sua font UTF-8 man chon ghe va mot so text booking/payment.

### Admin web

- Tao module Spring Boot `admin-web`.
- Login Firebase Auth + role ADMIN.
- Dashboard.
- Routes/Buses/Trips CRUD co ban.
- Seats theo bus.
- Users/Tickets/Payments list co ban.
- VNPAY create/return/ipn co luong tao signed URL + QR payload, gioi han payment 5 phut va dong payment het han.
- Fix Routes/Buses/Trips tra 500 do `demoMode` null.
- Routes/Buses/Trips da doc du lieu Firestore that sau reset nho.

### Functions/tools

- Scaffold Firebase Functions.
- Them VNPAY Functions ban dau.
- Them setAdminClaim Function ban dau.
- Them scripts:
  - `tools/firebase-admin-account.js`
  - `tools/firebase-admin-data.js`
  - `tools/firestore-seed/trim-firestore.js`
  - `tools/firestore-seed/seed-firestore.js`

## Chua lam / can lam tiep

Uu tien cao:

1. Chuyen Home popular routes sang Firestore.
2. Viet lai Firestore rules theo role/schema, khong de `allow read, write: if true`.
3. Test VNPAY sandbox bang dien thoai that/IPN public URL/ngrok neu khong dung emulator.
4. Them job cleanup nen cho pending payments qua han.
5. Them lich su ve day du tat ca status.

Uu tien trung binh:

6. Them audit log admin.
7. Them validate nghiep vu khi admin disable/cancel/refund/block.
8. Them ticket detail/payment detail va refund/cancel dung transaction.
9. Dong bo hoac loai bo Firebase Functions VNPAY neu khong dung.
10. Xoa/tach code admin Android cu sau khi web admin on dinh.
11. Quyet dinh giu Room lam offline cache hay xoa dan.
12. Them pagination/filter server-side de giam Firestore reads.

Bao mat:

13. Rotate Firebase service account key vi private key da tung duoc paste trong chat.
14. Dua VNPAY secret/Firebase web API key sang env khi dong goi/deploy.
15. Dung custom claims `admin=true` cho admin thay vi chi check `users.role`.

## Thu tu lam tiep de it rui ro

```text
1. Firestore rules chat lai
2. Job cleanup pending payment qua han
3. BookingHistory query day du tat ca status
4. VNPAY ngrok/public URL cho dien thoai that va IPN
5. Admin audit + validate nghiep vu
6. Don Room/admin Android legacy
```

## Ghi chu van hanh

- Khi test admin-web can dam bao file `admin-web/config/firebase-service-account.json` ton tai.
- `app/google-services.json` la config Android client, khong thay the duoc Firebase Admin SDK service account.
- VNPAY env vars khong thay the service account; admin-web can ca hai neu vua doc Firestore vua tao payment URL.
- Neu thay data khong dung, uu tien check project id trong `.firebaserc`, `app/google-services.json`, `application.yml`, va service account.
