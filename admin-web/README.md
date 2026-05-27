# BusBooking Admin Web

Spring Boot admin web for managing BusBooking Firestore data.

## Local setup

1. Put Firebase service account JSON at:

```text
admin-web/config/firebase-service-account.json
```

2. Set Firebase Web API key for admin login:

```powershell
$env:FIREBASE_WEB_API_KEY="your Firebase Web API key"
```

3. Set VNPAY sandbox configuration:

```powershell
$env:VNPAY_TMN_CODE="your VNPAY TMN code"
$env:VNPAY_HASH_SECRET="your VNPAY hash secret"
```

For Android emulator checkout, the default return URL is:

```text
http://10.0.2.2:8081/api/payments/vnpay/return
```

For a physical phone or VNPAY IPN testing, expose admin-web with a public URL such as ngrok and set:

```powershell
$env:VNPAY_RETURN_URL="https://your-public-domain/api/payments/vnpay/return"
```

Use the same public host for the VNPAY merchant IPN URL:

```text
https://your-public-domain/api/payments/vnpay/ipn
```

For local testing from both an Android emulator and another phone, run the project helper instead. It downloads a temporary Cloudflare Tunnel executable on first use, prompts for the sandbox credentials without saving the secret in source control, creates a public return URL, and starts `admin-web`:

```powershell
.\tools\run-vnpay-public.ps1
```

Create a new QR after the script prints the public return URL. The generated tunnel URL changes each time the script is restarted, so an old QR still points to its previous callback URL.

4. Run:

```powershell
.\gradlew.bat :admin-web:bootRun
```

5. Open:

```text
http://localhost:8081
```

The login account must exist in Firebase Auth and have `users/{uid}.role = ADMIN` and `isBlocked = false`.
