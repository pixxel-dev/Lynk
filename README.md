# Lynk (Android Application)

[Русский](#russian) | [English](#english)

---

<a name="russian"></a>
## 🇷🇺 Русский

**Lynk** — универсальное Android-приложение, предоставляющее плавающие элементы управления поверх всех окон, многоэтапный водопадный инсталлятор APK-файлов, инструмент мониторинга системной информации и журнала Logcat, управление правами доступа, а также встроенный менеджер авто-обновлений.

### Основные возможности

1. **Плавающие оверлей-кнопки (Overlay Controls)**:
   - Отображение плавающих элементов управления поверх всех приложений (`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`).
   - Навигация: «Домой», «Назад», «Быстрый запуск приложений», «Полный экран» (с поддержкой второго экрана/дисплея) и «Обновить интерфейс».
   - **Быстрые настройки по долгому нажатию**: удержание любой плавающей кнопки открывает диалоговое окно для настройки размера (dp), прозрачности (%), формы (круглая, скругленная, квадратная) и цвета. Все изменения сразу же применяются в реальном времени!

2. **Водопадный инсталлятор APK (Waterfall APK Installer)**:
   - Пошаговая диагностика и установка пакетов через цепочку стратегий:
     1. **Pine (Hook)**
     2. **Shizuku API**
     3. **Native ADB**
     4. **Local ADB**
     5. **PackageInstaller (Системный инсталлятор)**
   - Поддержка выбора локального APK и групповой работы с установленными приложениями.

3. **Системный монитор и Logcat**:
   - Диагностика технических характеристик устройства (процессор, ОЗУ, экраны, ОС).
   - Запись, отображение, копирование и очистка логов Logcat в реальном времени.
   - Управление разрешениями и правами приложения (Storage, Install Packages, Usage Stats, Overlay, Boot).

4. **Автоматическое обновление приложения (App Update Manager)**:
   - Автоматическая проверка новых релизов через GitHub API.
   - Скачивание APK-файлов с помощью `DownloadManager`.
   - **Автозапуск установки**: по завершению скачивания сразу вызывает системный интент установки через `FileProvider`.
   - Кнопка «Установить обновление» на экране обновлений для повторного вызова установки в любой момент.

5. **Файловый менеджер (File Manager)**:
   - Навигация по файловой системе, поиск, сортировка, просмотр свойств файлов и выбор APK для установки.

6. **Многоязычный интерфейс (RU/EN)**:
   - Динамическое переключение языка между русским и английским.

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
git clone https://github.com/dmitry1010/Lynk.git
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

1. **Floating Overlay Controls**:
   - Floating buttons displayed over all apps (`WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`).
   - Quick navigation: Home, Back, Quick App Launcher, Fullscreen Mode (secondary display supported), and Refresh UI.
   - **Long-Press Quick Settings Dialog**: Long-pressing any floating button opens a dialog to customize size (dp), opacity (%), shape (Circle, Rounded Square, Square), and color palette presets live in real-time!

2. **Waterfall APK Installer**:
   - Sequential diagnostic installation strategies:
     1. **Pine (Hook)**
     2. **Shizuku API**
     3. **Native ADB**
     4. **Local ADB**
     5. **PackageInstaller (Android Package Manager)**
   - Local APK selection and batch app inspection.

3. **System Diagnostics & Logcat**:
   - Comprehensive hardware & OS system specifications.
   - Real-time Logcat recording, viewer, clipboard copy, and clear controls.
   - System permission status monitoring (Storage, Install Packages, Usage Stats, Overlay, Boot).

4. **Automated App Update Manager**:
   - Release detection via GitHub API.
   - Download management using Android's `DownloadManager`.
   - **Auto-Launch Package Installer**: Automatically launches the package installation dialog via `FileProvider` upon download completion.
   - Explicit "Install Update" button on the Update screen for re-triggering installation anytime.

5. **File Manager**:
   - Storage navigation, search, sort, file property inspector, and direct APK installation trigger.

6. **Multi-language UI (RU/EN)**:
   - Dynamic language switcher supporting English and Russian.

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
git clone https://github.com/dmitry1010/Lynk.git
cd Lynk

# Build Debug APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```
