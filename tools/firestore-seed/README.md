# Firestore Seed Tool

One-time development seed for Firebase project `busbooking-f44f162d`.

## Setup

1. Open Firebase Console.
2. Go to Project settings > Service accounts.
3. Generate a new private key.
4. Save it locally as:

```text
tools/firestore-seed/service-account.json
```

Do not commit this file.

## Run

Trim large operational collections, then reseed a small dataset:

```powershell
cd C:\Users\ADMIN\AndroidStudioProjects\BusBooking\tools\firestore-seed
npm.cmd run reset-small
```

Or run the steps separately:

```powershell
cd C:\Users\ADMIN\AndroidStudioProjects\BusBooking\tools\firestore-seed
npm.cmd install
npm.cmd run trim
npm.cmd run seed
```

`trim` clears these collections:

- `routes`
- `buses` and `buses/{busId}/seats`
- `trips`
- `tickets`
- `payments`
- `tripSeats`

It intentionally keeps:

- `users`
- `phoneLogins`

`seed` writes a small demo dataset:

- `routes`
- `buses`
- `buses/{busId}/seats`
- only a few `trips` for common demo routes

It does not write users, tickets, payments, or trip seat holds.
