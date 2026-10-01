# SR Billing App

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20(M3)-4285F4?logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Room DB](https://img.shields.io/badge/Database-Room%20SQLite-FFCA28?logo=sqlite&logoColor=black)](https://developer.android.com/training/data-storage/room)
[![Direct APK Download](https://img.shields.io/badge/Download-billing--app.apk-indigo?logo=android)](./billing-app.apk)

A modern, fast, and offline-first Android billing and invoice management application built with **Jetpack Compose**, **Kotlin Coroutines/Flow**, and **Room Database**. Designed for small and medium retail businesses to manage daily billing, track customer ledgers, generate professional PDF receipts, and share invoices instantly via WhatsApp.

---

## 📱 Quick Download

You can download and install the ready-to-use Android application directly from this repository:

👉 **[Download billing-app.apk](./billing-app.apk)** (Latest Build)

### How to Install:
1. Download `billing-app.apk` on your Android device.
2. Tap on the downloaded APK file.
3. If prompted, enable **"Install from unknown sources"** in your device settings.
4. Tap **Install** and launch the app.

---

## ✨ Features

### 📊 1. Business Dashboard & Analytics
- **Live Sales Tracking**: Immediate overview of today's total bills count and revenue.
- **Customer Stats**: Total customer count and cumulative business revenue.
- **Recent Invoices**: Quick glance at the latest transactions with one-tap inspection.
- **Quick Action Bar**: Shortcuts for creating bills, adding customers, and checking records.

### 🧾 2. Fast Invoice Generation
- **Customer Selection & Quick Add**: Auto-fill customer details or add a new customer directly within the billing flow.
- **Dynamic Line Items**: Add multiple items with real-time rate (`₹`) and quantity calculations.
- **Flexible Discounts**: Apply discounts either as flat currency (`₹`) or percentage (`%`).
- **GST / Tax Calculation**: Automated tax calculations based on configured shop tax rate.
- **Opening / Previous Balance Tracking**: Carries previous customer dues into the current invoice to show accurate `Total Due`.
- **Custom Invoice Dating**: Select any transaction date via the built-in calendar picker.

### 📄 3. PDF Invoices & WhatsApp Sharing
- **A4 PDF Receipts**: Generates clean, formatted invoices featuring shop branding, GSTIN, itemized rows, tax details, and balances.
- **One-Tap WhatsApp Share**: Send PDF invoices directly to customer phone numbers with pre-formatted messaging.
- **Native Android Share Sheet**: Print, save to device storage, or share via Gmail, Drive, or messaging apps.

### 👥 4. Customer Ledger Management
- **Searchable Directory**: Instantly filter customers by name or contact number.
- **Opening Balances**: Track outstanding balances carried over for each customer.
- **Customer Code / Account ID**: Assign custom customer codes for ledger bookkeeping.
- **Edit & Maintain**: Update contact details or remove outdated profiles.

### 🔍 5. Billing History & Filter
- **Detailed History**: Browse past invoices with customer names, timestamps, and total amounts.
- **Date Range Filters**: Filter historical sales by custom `From Date` and `To Date`.
- **Invoice Inspector**: View complete item breakdowns, subtotal, discounts, and edit bill dates if needed.

### ⚙️ 6. Shop & Inventory Configuration
- **Business Profile**: Customize Shop Name, Contact Number, Address, and GSTIN printed on every invoice.
- **Product Catalog**: Add, toggle active/inactive status, and configure measurement units (`kg`, `pcs`, `box`, etc.).
- **Data Protection**: Local storage with reset confirmation to keep business data secure and under your control.

---

## 🛠️ Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Language** | Kotlin 2.0+ |
| **UI Framework** | Jetpack Compose (Material Design 3) |
| **Architecture** | Clean Architecture, MVVM (Model-View-ViewModel), Repository Pattern |
| **Local Database** | Room Database (SQLite with automatic migrations) |
| **Asynchronous** | Kotlin Coroutines & Reactive `StateFlow` |
| **Document Engine** | Android Native `PdfDocument` & `FileProvider` |
| **Build System** | Gradle (Kotlin DSL `build.gradle.kts`) with Version Catalog (`libs.versions.toml`) |
| **Min SDK** | Android 7.0 (API Level 24) |
| **Target SDK** | Android 15 / 16 (API Level 36) |

---

## 📁 Repository Structure

```text
Billing-app/
├── billing-app.apk                   # Pre-compiled ready-to-install Android APK
├── README.md                         # Project documentation
└── android-app/                      # Native Android Studio project
    ├── app/
    │   ├── build.gradle.kts          # Module-level Gradle configuration
    │   └── src/
    │       ├── main/
    │       │   ├── AndroidManifest.xml
    │       │   ├── java/com/example/srchicken/
    │       │   │   ├── MainActivity.kt        # Application entry point
    │       │   │   ├── Navigation.kt          # Compose UI screens & navigation flow
    │       │   │   ├── PdfGenerator.kt        # PDF generation & WhatsApp sharing engine
    │       │   │   ├── Formatters.kt          # Currency & date formatters
    │       │   │   ├── *ViewModel.kt          # MVVM ViewModels for all screens
    │       │   │   ├── data/                  # Room Entities, DAOs, & Database
    │       │   │   └── theme/                 # Material 3 colors, typography, & shapes
    │       │   └── res/                       # Drawables, mipmaps, strings, XML rules
    │       └── test/                          # Unit and UI instrumentation tests
    ├── gradle/                                # Gradle wrapper and version catalog
    ├── build.gradle.kts                      # Root Gradle configuration
    └── settings.gradle.kts                   # Project module settings
```

---

## 🚀 Building From Source

### Prerequisites
- [Android Studio Ladybug | 2024.2+](https://developer.android.com/studio) or newer
- JDK 17
- Android SDK (API 36 & Build-Tools)

### Steps
1. **Clone the repository:**
   ```bash
   git clone https://github.com/bharath1461/billing-app.git
   cd billing-app
   ```
2. **Open in Android Studio:**
   - Launch Android Studio.
   - Choose **Open an Existing Project** and select the `android-app` subfolder.
3. **Build the Debug APK via Gradle:**
   ```bash
   cd android-app
   ./gradlew assembleDebug
   ```
   The generated APK will be located at:
   `android-app/app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 License

This project is licensed under the [MIT License](LICENSE) — feel free to customize and adapt it for your business needs.
