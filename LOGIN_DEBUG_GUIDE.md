# 🔧 LOGIN DEBUG GUIDE - HƯỚNG DẪN KHẮC PHỤC LỖI ĐĂNG NHẬP

## ✅ NHỮNG VẤN ĐỀ ĐÃ SỬA

### 1. ✅ **BusBookingDatabase.kt** 
- **Vấn đề**: `onOpen()` không seed Admin account nếu database đã tồn tại
- **Sửa**: Thêm check - nếu không có admin, gọi `seedUsers()`
```kotlin
val userCursor = db.query("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'")
if (adminCount == 0L) {
    SeedDataProvider.seedUsers(db)  // Seed admin nếu chưa tồn tại
}
```

### 2. ✅ **SeedDataProvider.kt**
- **Vấn đề**: `seedUsers()` là `private`, không thể gọi từ `BusBookingDatabase`
- **Sửa**: Đổi thành `fun` (public)

### 3. ✅ **AuthRepository.kt**
- **Vấn đề 1**: Parameter nhầm `email` thay vì `phone`
- **Sửa**: Đổi `loginUser(email: String)` → `loginUser(phone: String)`

- **Vấn đề 2**: String template sai `${'$'}{e.message}` 
- **Sửa**: Thành `${e.message}`

### 4. ✅ **Logging** 
- **Thêm**: Chi tiết log trong `seedUsers()` và `loginUser()`
- **Giúp debug**: Xem chính xác điều gì xảy ra

---

## 🧪 CÁCH TEST - Làm theo các bước sau:

### **Bước 1: Xóa database cũ**
```bash
# Mở Android Studio → Device Manager
# Chọn emulator → Mở Terminal
adb shell am force-stop com.example.busbooking
adb shell rm /data/data/com.example.busbooking/databases/bus_booking.db
```

### **Bước 2: Rebuild & Run app**
```bash
# Trong Android Studio:
# Build → Rebuild Project
# Run → Run 'app'
```

### **Bước 3: Kiểm tra Logcat**
```
Logcat sẽ hiển thị:
✅ Admin user seeded: ID=1, Phone=0123456789, Role=ADMIN
✅ Database seeded successfully!
```

### **Bước 4: Thử login**
**Tài khoản Admin**:
- **Số điện thoại**: `0123456789`
- **Mật khẩu**: `123` (chính xác là 3 ký tự "123")

**Logcat sẽ hiển thị**:
```
🔍 Login attempt: phone=0123456789
✅ User found: id=1, role=ADMIN, isBlocked=false
🔐 Verifying password...
✅ Password verified! Login success
```

---

## 🐛 NẾU VẪN LỖI - Kiểm tra Logcat theo thứ tự:

### **Lỗi 1: "User not found"**
```
❌ User not found with phone=0123456789
```
**Nguyên nhân**: Admin account chưa được seed
**Cách sửa**:
1. Xóa database: `adb shell rm /data/data/com.example.busbooking/databases/bus_booking.db`
2. Rebuild & Run lại app

---

### **Lỗi 2: "Password mismatch"**
```
❌ Password mismatch
```
**Nguyên nhân**: Mật khẩu sai
**Cách sửa**:
- Đảm bảo nhập **chính xác**: `123` (không phải `1` hay `12`)
- Kiểm tra layout password field có bị lỗi không

---

### **Lỗi 3: "Account blocked"**
```
Your account has been blocked by admin
```
**Nguyên nhân**: Admin account bị đánh dấu `isBlocked=1`
**Cách sửa**:
1. Xóa database
2. Rebuild & Run lại

---

### **Lỗi 4: Login exception**
```
❌ Login exception: [chi tiết lỗi]
```
**Nguyên nhân**: Lỗi trong code
**Cách sửa**:
1. Kiểm tra full stack trace trong Logcat
2. Liên hệ developer

---

## 📊 ADMIN ACCOUNT DETAILS - Thông tin chi tiết

```
┌─────────────────────────────────────┐
│ ADMIN ACCOUNT (DEFAULT SEEDED)      │
├─────────────────────────────────────┤
│ ID:           1                     │
│ Name:         Admin                 │
│ Email:        admin@bus.com         │
│ Phone:        0123456789 ← Login với số này
│ Password:     123 ← Mật khẩu là "123"
│ Password Hash: BCrypt($2...) ← Được hash với BCrypt
│ Role:         ADMIN ← Quy&en admin
│ isBlocked:    false ← Không bị chặn
│ Created:      [System timestamp]   │
└─────────────────────────────────────┘
```

---

## 🔐 PASSWORD HASHING - Cách mật khẩu được lữu

### **Khi Seed (Khởi tạo)**:
```kotlin
val adminPw = PasswordHasher.hash("123")  // Hash "123" → "$2a$12$..."
db.execSQL("INSERT ... VALUES (?,?,?,?,?,?)", 
    arrayOf(..., adminPw, ...))  // Lưu hash vào database
```

### **Khi Login**:
```kotlin
val storedHash = "$2a$12$..."  // Lấy từ database
val inputPassword = "123"      // User nhập
val isVerified = PasswordHasher.verify(inputPassword, storedHash)
// Nếu verify nhận: ĐÚNG → Login success
// Nếu verify nhận: SAI → "Phone or password is incorrect"
```

---

## 📂 FILES CHANGED (Các file đã sửa):

1. ✅ `app/src/main/java/com/example/busbooking/data/db/BusBookingDatabase.kt`
   - Added admin check in `onOpen()`

2. ✅ `app/src/main/java/com/example/busbooking/data/db/SeedDataProvider.kt`
   - Changed `seedUsers()` từ private → public
   - Added logging

3. ✅ `app/src/main/java/com/example/busbooking/domain/repository/AuthRepository.kt`
   - Fixed parameter: `email` → `phone`
   - Fixed string template
   - Added detailed logging

---

## 💡 TIPS & TRICKS

### **Tip 1: Clear app data nhanh**
```bash
adb shell pm clear com.example.busbooking
```
Cách này sẽ xóa database + shared preferences + datastore → app được reset hoàn toàn

### **Tip 2: Xem trực tiếp database**
```bash
adb shell
sqlite3 /data/data/com.example.busbooking/databases/bus_booking.db
SELECT * FROM users;
```

### **Tip 3: Xem Logcat realtime**
```bash
adb logcat | grep -E "(✅|❌|🔍|🔐)"
```

---

## 🎯 EXPECTED FLOW - Quy trình mong đợi:

```
App Start
   ↓
Database onCreate() / onOpen()
   ├─ Check: Admin account tồn tại?
   └─ Nếu không → seedUsers() → ✅ Admin account ready
   ↓
User nhập: phone=0123456789, password=123
   ↓
loginButton.click()
   ├─ Validate: input không blank
   └─ Call: authRepository.loginUser(phone, password)
   ↓
AuthRepository.loginUser()
   ├─ Query: getUserByPhone("0123456789")
   ├─ Found: user ID=1, role=ADMIN
   ├─ Check: isBlocked? NO
   ├─ Verify: password("123", hash) → TRUE
   └─ Return: Result.Success(user)
   ↓
AuthViewModel.login()
   ├─ Result: Success
   └─ SessionManager.saveSession(user)
   ↓
LoginFragment.navigateBasedOnRole()
   ├─ Check: role == "ADMIN"? YES
   └─ Navigate: to AdminDashboardFragment
   ↓
✅ Login Success!
```

---

**Cập nhật: May 20, 2026**

