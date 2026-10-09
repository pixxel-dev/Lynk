# Lynk (Android Application)

[Русский](#russian) | [English](#english)

---

<a name="russian"></a>
## 🇷🇺 Русский

**Lynk** — универсальное Android-приложение, предоставляющее плавающие элементы управления поверх всех окон, многоэтапный водопадный инсталлятор APK-файлов, инструмент мониторинга системной информации и журнала Logcat, управление правами доступа, а также встроенный менеджер авто-обновлений.

### Основные возможности

1. **Настраиваемый оверлей плавающих кнопок**:
   - Плавающий элемент управления, работающий поверх всех приложений.
   - Индивидуальная настройка параметров по долгому нажатию: размер, прозрачность, 6 кастомных форм (звезда, сердечко, круг, квадрат и т.д.) и цветовые палитры. Все изменения применяются в реальном времени!
   - Назначение действий: Навигация («Домой», «Назад»), Быстрый запуск, Полный экран, Обновить.

2. **Беспроводной ADB (Wireless ADB)**:
   - Встроенный менеджер для подключения и выполнения команд ADB по Wi-Fi прямо из приложения без ПК.

3. **Водопадный инсталлятор APK (Waterfall APK Installer)**:
   - Пошаговая диагностика и установка пакетов через цепочку стратегий: Pine (Hook), Shizuku API, Native ADB, Local ADB, PackageInstaller.

4. **WebDAV Файловый менеджер (WebDAV File Manager)**:
   - Интеграция с Яндекс Диском и Облаком Mail.ru: навигация по файлам, скачивание и установка по публичным ссылкам и WebDAV.
   - Локальная навигация, поиск, сортировка и установка APK.
   - **Важно для WebDAV**: Для авторизации в Яндекс Диске и Облаке Mail.ru используйте **Пароль для внешних приложений** (создается в настройках безопасности вашего аккаунта), а не обычный пароль от почты. В Mail.ru логином является полный email.

5. **Системный монитор и Logcat**:
   - Диагностика технических характеристик устройства (ОЗУ, процессор, экраны).
   - Запись, отображение, копирование и **сохранение логов Logcat в файл**.
   - Управление разрешениями и правами приложения.

6. **ОТА Обновления с GitHub (OTA App Update Manager)**:
   - Автоматическая проверка новых релизов через GitHub API, скачивание и вызов установки обновлений.

7. **Многоязычный интерфейс (RU/EN)**:
   - Динамическое переключение языка интерфейса с 100% локализацией.

---

### Архитектура и технологии

- **Язык**: Kotlin, Java
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Модульная структура**:
  - `:app` — Главная активность (`MainActivity`), фоновые службы (`ForegroundOverlayService`, `BootReceiver`).
  - `:core:domain` — Чистая бизнес-логика, модели обновлений (`UpdateInfo`, `AppUpdateManager`), интерфейсы инсталляторов.
  - `:feature:dashboard` — Пользовательский интерфейс, ViewModels (`DashboardViewModel`), утилиты загрузки (`ApkDownloader`).
- **Фоновые службы**: Foreground Service (`ForegroundOverlayService`) с поддержкой Android 14+.
- **Асинхронность**: Kotlin Coroutines & `StateFlow`.

---

### Инструкция по сборке

#### Требования:
- Android Studio Ladybug (2024.2.1) или новее
- JDK 17
- Android SDK Platform API 35 (Android 15)
- Минимальная версия Android: API 26 (Android 8.0)

#### Сборка проекта:
```bash
# Клонирование репозитория
git clone https://github.com/pixxel-dev/Lynk.git
cd Lynk

# Сборка Debug APK
./gradlew assembleDebug

# Установка на подключенное устройство
./gradlew installDebug
```

---

<a name="english"></a>
## 🇬🇧 English

**Lynk** is a multi-functional Android application featuring system-wide floating overlay controls, a multi-stage waterfall APK installer, real-time system & Logcat diagnostics, permission management, and an automated application update system.

### Key Features

1. **Customizable Floating Overlay**:
   - A persistent floating control element displayed over all apps.
   - Quick settings via long-press: size, opacity, 6 custom shapes (star, heart, circle, square, etc.), and color palettes. All applied live in real-time!
   - Assign actions: Home, Back, Quick App Launcher, Fullscreen Mode, Refresh UI.

2. **Wireless ADB**:
   - Built-in manager to connect and execute ADB commands over Wi-Fi directly from the app without a PC.

3. **Waterfall APK Installer**:
   - Sequential diagnostic installation strategies: Pine (Hook), Shizuku API, Native ADB, Local ADB, Android Package Manager.

4. **WebDAV File Manager**:
   - Yandex Disk and Mail.ru Cloud Integration: browse files, download, and install via public links and WebDAV.
   - Local storage navigation, search, sort, and direct APK installation.
   - **WebDAV Important Note**: To authenticate with Yandex Disk and Mail.ru Cloud, use an **App Password** (generated in your account security settings), not your regular email password. For Mail.ru, use your full email address as the username.

5. **System Diagnostics & Logcat Exporter**:
   - Comprehensive hardware & OS system specifications.
   - Real-time Logcat recording, viewer, clipboard copy, and **saving logs directly to a file**.
   - System permission status monitoring.

6. **OTA Updates from GitHub**:
   - Automated release detection via GitHub API, background downloading, and update installation triggering.

7. **Multi-language UI (RU/EN)**:
   - Dynamic language switcher supporting fully localized English and Russian interfaces.

---

### Architecture & Tech Stack

- **Languages**: Kotlin, Java
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Modular Design**:
  - `:app` — Entry point (`MainActivity`), background services (`ForegroundOverlayService`, `BootReceiver`).
  - `:core:domain` — Domain logic, update models (`UpdateInfo`, `AppUpdateManager`), installer contracts.
  - `:feature:dashboard` — Compose UI screens, `DashboardViewModel`, download utilities (`ApkDownloader`).
- **Services**: Foreground Service (`ForegroundOverlayService`) with Android 14+ special use support.
- **Concurrency**: Kotlin Coroutines & `StateFlow`.

---

### Build Instructions

#### Prerequisites:
- Android Studio Ladybug (2024.2.1+)
- JDK 17
- Android SDK API Level 35
- Min SDK: API 26 (Android 8.0+)

#### Build Commands:
```bash
# Clone repository
git clone https://github.com/pixxel-dev/Lynk.git
cd Lynk

# Build Debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```
