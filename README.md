# Sulck Platform

A complete savings platform with device-lock enforcement, consisting of:

- **Sulck Android App** (Kotlin + Jetpack Compose) - `SulckProject/`
- **Sulck Backend API** (Ktor + Exposed + PostgreSQL) - `SulckBackend/`
- **Sulck Admin Portal** (Next.js + Refine + Tailwind) - `SulckAdmin/`

---

## Architecture

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│  Android App    │◄───►│  Backend API    │◄───►│  PostgreSQL     │
│  (Kotlin)       │     │  (Ktor)         │     │  (PostgreSQL 16)│
└─────────────────┘     └─────────────────┘     └─────────────────┘
                              ▲
                              │
                     ┌─────────────────┐
                     │  Admin Portal   │
                     │  (Next.js)      │
                     └─────────────────┘
```

---

## Quick Start

### Prerequisites
- JDK 17+
- Node.js 18+
- Docker & Docker Compose
- PostgreSQL 16 (or use Docker)

### 1. Start Infrastructure
```bash
cd SulckBackend
docker-compose up -d
```

### 2. Run Backend
```bash
cd SulckBackend/ktor-app
./gradlew run
# or: ./gradlew shadowJar && java -jar build/libs/ktor-app.jar
```

### 3. Run Admin Portal
```bash
cd SulckAdmin
npm install
npm run dev
# Open http://localhost:3000
```

### 4. Build Android App
```bash
cd SulckProject
./gradlew assembleDebug
```

---

## Project Structure

```
SulckPlatform/
├── SulckProject/              # Android App
│   ├── app/
│   │   ├── src/main/java/com/sulck/app/
│   │   │   ├── ui/screens/    # Compose screens
│   │   │   ├── ui/navigation/ # Navigation
│   │   │   ├── ui/onboarding/ # Onboarding flow
│   │   │   ├── domain/        # Domain models
│   │   │   ├── data/          # Repository, API, DB
│   │   │   └── platform/      # Device lock manager
│   │   └── build.gradle.kts
│   └── settings.gradle.kts
│
├── SulckBackend/              # Ktor Backend
│   ├── ktor-app/
│   │   ├── src/main/kotlin/com/sulck/backend/
│   │   │   ├── config/        # Configuration
│   │   │   ├── domain/        # Entities, repositories, use cases
│   │   │   ├── infrastructure/# DB, repositories, services
│   │   │   ├── presentation/  # Routes, DTOs
│   │   │   └── di/            # Koin modules
│   │   └── src/main/resources/application.conf
│   ├── docker-compose.yml
│   └── Dockerfile
│
└── SulckAdmin/                # Next.js Admin Portal
    ├── src/
    │   ├── app/               # Next.js App Router
    │   │   ├── page.tsx       # Login
    │   │   └── dashboard/     # Dashboard pages
    │   └── components/        # Reusable components
    ├── tailwind.config.js
    └── package.json
```

---

## Key Features

### Android App
- **Savings Goals** with dynamic tiers (Short/Medium/Long term)
- **Device Lock Enforcement** via DeviceAdminReceiver
- **Daily Streak** tracking with lockout after 3 missed days
- **Paystack Integration** for mobile money deposits
- **Material 3 UI** with Sulck branding (Charcoal + Neon Mint)

### Backend API
- **Ktor 2.3** + **Exposed ORM** + **PostgreSQL**
- **JWT Auth** with access/refresh tokens
- **Paystack Integration** for mobile money
- **Streak Management** with device lock enforcement
- **Admin Endpoints** for platform management

### Admin Portal
- **Next.js 14** + **Tailwind CSS** + **Refine**
- **Dashboard** with platform metrics
- **User/Goal/Transaction Management**
- **Sulck Brand Theme** (Charcoal + Neon Mint)

---

## Environment Variables

### Backend (`.env`)
```env
DATABASE_URL=jdbc:postgresql://localhost:5432/sulck
DATABASE_USER=postgres
DATABASE_PASSWORD=postgres
JWT_SECRET=your-32-char-secret
PAYSTACK_SECRET_KEY=sk_test_...
PAYSTACK_PUBLIC_KEY=pk_test_...
PAYSTACK_WEBHOOK_SECRET=...
```

### Admin (`.env.local`)
```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

---

## API Endpoints

### Auth
- `POST /auth/register` - Register user
- `POST /auth/login` - Login
- `POST /auth/refresh` - Refresh tokens
- `GET /auth/me` - Current user

### Goals
- `POST /goals` - Create goal
- `GET /goals` - List goals
- `GET /goals/{id}` - Get goal

### Transactions
- `POST /transactions/deposit` - Initialize deposit
- `GET /transactions` - Transaction history

### Streak
- `GET /streak` - Current streak

### Webhooks
- `POST /webhook/paystack` - Paystack callbacks

### Admin (Protected)
- `GET /admin/stats` - Platform statistics

---

## Deployment

### Backend (Render/Fly.io/Railway)
```bash
# Build
./gradlew :SulckBackend/ktor-app:shadowJar

# Run
java -jar SulckBackend/ktor-app/build/libs/ktor-app.jar
```

### Admin (Vercel)
```bash
cd SulckAdmin
vercel deploy
```

### Android (Play Store)
```bash
cd SulckProject
./gradlew bundleRelease
```

---

## License
MIT