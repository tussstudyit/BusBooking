# BusBooking Android

Du an Android Studio gom 2 module ung dung:

- `app`: ung dung khach hang dat ve xe.
- `staff-app`: ung dung nhan vien nha xe kiem ve va theo doi chuyen duoc phan cong.

Backend Spring Boot va web admin nam tai:

```text
C:\Users\ADMIN\IdeaProjects\BusBooking
```

## Cong nghe

- Kotlin
- Android XML layout
- MVVM voi ViewModel, LiveData, Repository
- Navigation Component
- Material Components
- REST API goi toi backend Spring Boot
- MySQL/XAMPP thong qua backend, khong dung Firebase/Room local cho du lieu nghiep vu chinh

## Cau truc chinh

```text
app/          Android app khach hang
staff-app/    Android app nhan vien nha xe
database/     File SQL tao database MySQL dung chung voi backend
gradle/       Gradle wrapper va version catalog
```

## Cau hinh API

Sua `local.properties` tai root project:

```properties
api.baseUrl=http://10.0.2.2:8081
staff.api.baseUrl=http://10.0.2.2:8081
```

Dung emulator thi giu `10.0.2.2`. Dung dien thoai that thi doi thanh IP may tinh dang chay backend, vi du:

```properties
api.baseUrl=http://192.168.1.199:8081
staff.api.baseUrl=http://192.168.1.199:8081
```

## Chay tung app trong Android Studio

Chon Run/Debug Configuration:

- Module `BusBooking.app`: chay app khach hang.
- Module `BusBooking.staff-app`: chay app nhan vien.

Lenh build:

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :staff-app:assembleDebug
```

## Tai khoan mac dinh

Can chay backend Spring Boot va MySQL truoc khi dang nhap app.

- User: `0900000001` / `123`
- Staff: `51B12345` / `123`

## Luong chinh

App khach hang:

- Dang ky/dang nhap bang so dien thoai.
- Tim chuyen, chon ghe, dat ve.
- Thanh toan VNPAY qua backend.
- Ve da thanh toan co QR `BUSBOOKING-TICKET:<ticketId>` de staff quet.

App staff:

- Dang nhap bang bien so xe va mat khau.
- Xem danh sach chuyen duoc phan cong theo xe.
- Quet QR ve, xac nhan hanh khach len xe.
- Xem so do ghe: ghe trong, ghe da dat, ghe da check-in.
