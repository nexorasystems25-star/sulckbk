# M-KOPA App Project Blueprint

## App Overview
- **Package**: `com.mkopa`
- **Purpose**: Pay-as-you-go (PAYG) solar & asset financing customer app for M-KOPA (Kenya/Nigeria/Ghana markets)
- **Core Domain**: Customer account management, loan/repayment tracking, product catalog, offers, in-app messaging & support

---

## Main Data Models (from `.proto` definitions)

| Model | Key Fields | Purpose |
|-------|------------|---------|
| **AccountDto** | `accountId`, `number`, `balance`, `amountPaid`, `totalPrice`, `creditsAvailable`, `status`, `loans[]`, `paymentPlans[]`, `allowsPayments` | Customer's financial account with credit balance, active loans, and payment plans |
| **LoanDto** | `id`, `displayName`, `type`, `paymentCadenceInDays`, `startDate`, `expectedFinishedDate`, `loanCollateralAssetDto` | Individual loan (solar system, phone, cash loan) with collateral asset |
| **AccountLoanDto** | `id`, `status`, `totalAmount`, `paidAmount`, `remainingAmount`, `dailyAmount` | Loan repayment tracking per account |
| **CustomerProfileDto** | `id`, `firstName`, `surname`, `primaryPhone` | KYC profile |
| **RepaymentOnboardingStateDto** | `loan_terms`, `missed_payments`, `pending_blocking` (each with `CompletionStatusDto`) | Guided repayment setup flow |
| **OffersFeedDto** | `offers[]` (OfferDto with `type`, `status`, `details` incl. `max_amount`, `recovery_details`, `bundle_size`) | Promotional/upgrade offers |
| **PhoneCatalogDto** | `header`, `phones[]` (PhoneCatalogItemDto: `brand`, `title`, `subtitle`, `image_url`, `badges[]`) | Product catalog for device upgrades |
| **HomeConfigDto** | `headerWidgets[]`, `contentWidgets[]`, `footerWidgets[]` | Dynamic home screen composition |

---

## Navigation / Primary Screens (from `res/layout/`)

| Layout File | Screen / Fragment | Notes |
|-------------|-------------------|-------|
| `activity_login.xml` | **Login / Entry** | First screen (Main/Launcher) |
| `inbox_activity.xml` | **Inbox / Messages** | Central messaging hub |
| `inbox_list_view.xml` | Inbox list | Message list with icons |
| `inbox_carousel_*.xml` | Carousel views | Promotional/announcement carousel |
| `layout_pinpad.xml` | **PIN Entry** | Secure PIN input for transactions |
| `inapp_cover.xml`, `inapp_interstitial.xml`, `inapp_half_interstitial.xml` | **In-App Messages** | Full/half-screen interstitials, covers |
| `freshchat_activity_*.xml` (15+ files) | **Customer Support Chat** | Freshchat integration (FAQ, conversations, calendar, attachments) |
| `design_navigation_*.xml`, `m3_navigation_*.xml` | **Navigation Drawer / Bottom Bar** | Material 3 nav rail, menu items, subheaders |
| `activity_*` (Paystack) | Payment WebView screens | Auth, AVS, Card, OTP, PIN for Paystack gateway |

---

## Key Workflows

1. **Login → Home** (`activity_login` → dynamic `HomeConfigDto` widgets)
2. **Account Dashboard** → Shows `AccountDto` with balances, loans, payment plans
3. **Repayment Onboarding** → Multi-step: Loan Terms → Missed Payments → Pending Blocking (tracked in `RepaymentOnboardingStateDto`)
4. **Make Payment** → `layout_pinpad` + Paystack web flow (`co_paystack_android____activity_*.xml`)
5. **Offers / Upgrades** → `OffersFeedDto` rendered in home content widgets
6. **Product Catalog** → `PhoneCatalogDto` for device purchases
7. **In-App Messaging** → Interstitials/carousels (`inapp_*.xml`, `inbox_carousel_*.xml`)
8. **Customer Support** → Freshchat full suite (conversations, FAQ, calendar booking, file sharing)

---

## Network / Endpoints (inferred)

| Feature | Expected Endpoint Pattern |
|---------|---------------------------|
| Account sync | `GET /api/accounts/{accountId}` → `AccountInformationDto` |
| Loans | `GET /api/loans` → `LoansDto` |
| Repayment onboarding state | `GET/POST /api/repayment-onboarding/{loanId}` |
| Offers feed | `GET /api/offers/feed` → `OffersFeedDto` |
| Phone catalog | `GET /api/catalog/phones` → `PhoneCatalogDto` |
| Home config | `GET /api/home/config` → `HomeConfigDto` |
| Customer profile | `GET /api/profile` → `CustomerProfileDto` |
| Payments (Paystack) | WebView callbacks to `co_paystack_android` activities |
| Push notifications | Firebase Cloud Messaging (`messaging_event.proto`) |
| In-app messages | `GET /api/inapp/messages` |
| Freshchat | Freshchat SDK endpoints (chat, FAQ, calendar) |
| Network security | `res/xml/charles_network_security_config.xml` (cert pinning / domain config) |

---

## Third-Party Integrations
- **Paystack** — Payment gateway (card, bank, mobile money)
- **Freshchat** — Customer messaging & support (FAQ, live chat, calendar)
- **Firebase** — Analytics, Crashlytics, Cloud Messaging, Auth, Performance
- **Google Play Services** — Ads, Auth, Location, Maps
- **Charles Proxy** — Network security config (certificate pinning)

---

## Build / Tech Stack Signals
- **Language**: Kotlin (package `kotlin/` present), Java (`java/`, `com/`), Protobuf for data contracts
- **Architecture**: Modular data layers (`data.accounts`, `data.loans`, `data.repayments`, `data.personalisation`, `data.customerProfile`, `data.repeatPhones`)
- **UI**: Material 3 (`m3_*` layouts), Jetpack Navigation (`design_navigation_*`), Freshchat SDK
- **Serialization**: Protobuf (`*.proto` files with `java_multiple_files = true`)
- **DI / Modules**: `core-common`, `app-update`, `core` packages with versioned clients

---

## Recreation Priority Order
1. **Auth + Account Sync** — Login, account list, balances
2. **Loan Dashboard** — Active loans, repayment status, daily amounts
3. **Repayment Flow** — PIN entry + Paystack integration
4. **Home Screen** — Dynamic widget composition from `HomeConfigDto`
5. **Offers / Catalog** — Feed + phone catalog
6. **Inbox / In-App Messages** — Carousel, interstitials, push handling
7. **Support Chat** — Freshchat integration
8. **Settings / Profile** — KYC, notification prefs, security

---

## Files to Reference for Implementation
- `D:\Desktop\AppHub\M-KOPA_apk_extracted\*.proto` — Complete data contracts
- `D:\Desktop\AppHub\M-KOPA_apk_extracted\res\layout\activity_login.xml` — Entry screen
- `D:\Desktop\AppHub\M-KOPA_apk_extracted\res\layout\inbox_activity.xml` — Messaging hub
- `D:\Desktop\AppHub\M-KOPA_apk_extracted\res\layout\layout_pinpad.xml` — Payment auth
- `D:\Desktop\AppHub\M-KOPA_apk_extracted\res\xml\charles_network_security_config.xml` — Cert pinning rules
- `D:\Desktop\AppHub\M-KOPA_apk_extracted\res\xml\provider_paths.xml` — FileProvider config