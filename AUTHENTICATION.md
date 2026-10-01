# Login and API protection

Both customers and administrators use POST /api/auth/login. Passwords are checked with BCrypt.
Tokens expire after 15 minutes. Send them as Authorization: Bearer <token>.
The server verifies token signatures, expiration, issuer, and the credential's current role in MongoDB.

## Start the backend

Keep using your existing MongoDB configuration. In the same PowerShell terminal that starts Maven, set:

```powershell
$jwtKeyBytes = New-Object byte[] 32
$jwtRandom = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$jwtRandom.GetBytes($jwtKeyBytes)
$jwtRandom.Dispose()
$env:JWT_SECRET = [Convert]::ToBase64String($jwtKeyBytes)
$env:BANK_ADMIN_USERNAME = 'bank-admin'
$adminPasswordInput = Read-Host 'Choose the initial administrator password' -AsSecureString
$env:BANK_ADMIN_PASSWORD = (New-Object System.Net.NetworkCredential('', $adminPasswordInput)).Password
.\mvnw.cmd spring-boot:run
```

Use a password of at least 10 characters and at most 72 UTF-8 bytes.
The administrator is created once; subsequent starts never overwrite their password.
Remove BANK_ADMIN_PASSWORD from the terminal environment after provisioning.
JWT_SECRET must be a Base64 random key of at least 32 bytes, with no checked-in default.
For a stable development key, put JWT_SECRET in your existing ignored atlas-credentials.env.
Generating a new key on every restart invalidates previously issued tokens.

The application creates a unique username index in user_credentials on startup.
No existing customer is automatically assigned login credentials.

## Requests

POST /api/auth/register (public):
```json
{"username":"new-customer","password":"choose-a-unique-password","name":"New Customer"}
```
Creates a NEW customer and linked credentials atomically, always with CUSTOMER permissions.
Users cannot choose an admin role or attach their credentials to someone else's customer ID.
Usernames are normalized to lowercase. Duplicate usernames return 409.
This uses the existing MongoDB transaction manager and requires a transaction-capable MongoDB deployment (Atlas).

POST /api/auth/login (public):
```json
{"username":"bank-admin","password":"your-initial-admin-password"}
```
Returns token, tokenType, expiresIn, username, role, and customerId.
Incorrect credentials return 401. Password hashes are excluded from JSON.

## Permissions

| Operation | Customer | Admin |
| --- | --- | --- |
| GET /api/accounts | Own accounts only | All accounts or userId filter |
| GET /api/accounts/{id}, /{id}/transactions | Own account only | Any account |
| GET /api/customers/{id} | Own customer only | Any customer |
| GET /api/customerDashboard/{id} | Own customer only | Any customer |
| POST /api/accounts/transfer | Between own accounts only | Any valid account pair |
| Account creation, editing, deletion, deposit, withdrawal, premium query | Forbidden | Allowed |
| Customer creation, editing, deletion, list | Forbidden | Allowed |
| GET /api/admin and all audit endpoints | Forbidden | Allowed |

Customers cannot fetch another customer's accounts by changing a URL, filter, or transfer body.
For now customer transfers are between their own accounts. Transfers to other customers need a separate response that does not expose the recipient's account details.
Unauthenticated/invalid tokens return 401. Authenticated users without permission receive 403.
Unknown endpoints are denied by default.

## Postman

Import postman/Banking Authentication.postman_collection.json.
Set adminUsername and adminPassword locally in Postman, then run Admin Login.
The test script saves the returned adminToken. Customer registration/login similarly saves customerToken and customerId.
Keep passwords and generated tokens out of exported collections and Git.
Existing banking requests now require Authorization -> Bearer Token -> {{adminToken}}.
The old No DB collection is a historical deliverable and is left unchanged.

## Status and tests

Backend login/protection and React login/registration are implemented.
The shared login opens /operations for administrators and /customer-dashboard for customers.
React attaches bearer tokens to protected API requests, handles 401 by signing out, and displays 403 permission errors.
Customer views request only their accounts and transaction history; transfers are restricted to their own accounts.
Tokens stay in memory and expire after 15 minutes. Refreshing the page requires signing in again.
Sign-out clears the browser session; an already issued JWT remains valid on the backend until its expiration or credential deletion.
There is no direct unauthenticated route to the operations dashboard.
Start the frontend from frontend with npm.cmd run dev. Its Vite /api proxy connects to Spring on port 8080.
Frontend API/session tests: node --test src/services/bankingApi.test.js src/services/auth.test.js (run in frontend).
SecurityIntegrationTests uses real Spring Security filters, signed JWTs, and BCrypt, with mocked MongoDB repositories.
Tests cover both roles logging in, invalid passwords, expired/malformed tokens, revoked credentials,
ownership checks, admin-only access, forbidden transfers, registration hashing, and duplicate usernames.
Live Atlas persistence, administrator provisioning, and a browser login against the running backend still need local verification.
