# Architecture Rules / Архитектурные правила

This document defines the architectural guidelines and agent operating procedures for the `Lynk` project.
Данный документ определяет архитектурные принципы и правила работы агентов для проекта `Lynk`.

---

## 🇬🇧 English

### 1. Fundamental Agent Operating Rules
- **Change Approval & Scoping:** Agents must not perform unapproved cross-module refactoring or breaking architectural changes without explicit user or manager consent. Agents must operate strictly within their assigned scope.
- **File Manipulation Constraints:** File modification via shell utilities (`sed`, `awk`, `echo >`, etc.) is strictly prohibited. Agents must exclusively use designated IDE file editing tools (`write_file`, `replace_file_content`, `multi_replace_file_content`) to prevent unsaved IDE buffer desynchronization.
- **Mandatory Local Release Verification before Git Push**: Before pushing any commit to GitHub (`git push`), the Coder Agent must successfully execute a local release build `./gradlew :app:assembleRelease` in 100% of cases. Pushing non-compiling code or code that breaks CI/CD is strictly forbidden.

### 2. Architecture & Technology Stack
- **Multi-Module Clean Architecture:**
  - `:app`: Android application module. Handles dependency wiring, top-level UI, and navigation using **Kotlin** and Jetpack Compose.
  - `:core:domain`: Pure business logic, use cases, and entities. Written **STRICTLY in Java 11+** (`java-library` plugin) with **ZERO Android framework dependencies**.
  - `:feature:*`: Isolated feature modules (e.g., `:feature:dashboard`) containing Compose UI and presentation logic in **Kotlin**.
- **Tech Stack Requirements:**
  - **UI:** Jetpack Compose exclusively (strictly no XML layouts).
  - **Navigation:** Jetpack Navigation 3 (`androidx.navigation3`).
  - **Minimum SDK:** 31 (Android 12).
  - **Dependency Management:** Centralized via Gradle Version Catalog (`gradle/libs.versions.toml`).
- **APK Size Optimization (R8/ProGuard):** Agents must apply aggressive shrinking for release builds (`isMinifyEnabled = true`, `isShrinkResources = true`). It is forbidden to use overly broad keep rules (e.g., `-keep class androidx.compose.** { *; }`) that bloat the app size. The release APK size must not exceed ~5-10 MB.

### 3. Documentation & Bilingual Standards
- **Dual Language Requirement:** All primary architectural, design, and project documentation must maintain full English (EN) and Russian (RU) versions.
- **100% Key Parity:** String resources in `res/values/strings.xml` (EN) and `res/values-ru/strings.xml` (RU) must maintain 100% key parity at all times. Parity is verified via `./gradlew :checkLocalization`.
- **No Hardcoded UI Strings:** Using hardcoded string literals directly in Compose UI components is strictly prohibited. All user-facing text must use `stringResource(R.string...)`.

### 4. QA, Testing & UX Optimization Protocol
- **Full Interactive Controls Testing:** After every iteration or enhancement stage, testing agents must verify all buttons, tabs, and interactive UI controls on an emulator.
- **Redundancy Elimination & Auto-Actions:** Manual or repetitive user actions must be eliminated (e.g., replacing manual "Refresh" buttons with reactive state flows, lifecycle auto-updates on `ON_RESUME`, or background synchronization).
- **Mandatory Release Startup Verification:** After every successful release build (`:app:assembleRelease`), the agent MUST install it on a local emulator (`adb install -r ...`) and verify successful startup (`adb shell monkey -p ru.doGood.Lynk -c android.intent.category.LAUNCHER 1`), reading logs (`adb logcat -d -b crash`) to prevent startup crashes.

### 5. AI Agent Ecosystem & Roles
1. **Engineering Manager Agent:** Orchestration, backlog and implementation plan management, user communication, and task delegation.
2. **Product Design Agent (`product_design_agent`):** Generation of PRD, architecture documentation, and UI/UX design specifications.
3. **Coder Agent (`coder_agent`):** Pure Java 11+ business logic development in `:core:domain`, Kotlin Jetpack Compose UI in feature/app modules, Gradle configuration, and Git management.
4. **Critic Agent (`critic_agent` / QA):** Emulator deployment, end-to-end interactive UI testing, log monitoring, and bug fix verification.
5. **Code Review & Security Auditor Agent:** Static code analysis (Checkstyle/ktlint), security vulnerability scanning, dependency auditing, and strict Java boundary enforcement in `:core:domain`.
6. **Localization Agent (`localization_agent`):** Automated dual-language string management (`values/strings.xml` and `values-ru/strings.xml`), 100% key parity enforcement, and hardcoded string elimination.

### 6. Security & CI/CD Releases
- **CI/CD Release Signing:** Release builds in CI/CD are signed strictly using GitHub Repository Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
- **Local Development:** Default Debug signing is used for local builds.
- **Key Security:** Committing `.keystore` or private signing key files to the Git repository is strictly prohibited.

### 7. Token Consumption Efficiency
- **Concise Responses:** Agent responses must be concise, precise, and free of redundant, lengthy introductory or concluding boilerplate.
- **Strict Prohibition of Technical Spam & Code Listings:** Manager and agents must respond to the user strictly with a **concise final summary in simple plain language** without technical programming details (no code listings, class/function names, or internal intermediate steps).
- **User Response Format:** State only what was accomplished for the user and what can be tested/verified.
- **No Code Duplication:** Duplicating identical code snippets multiple times is strictly prohibited; keep responses focused and to the point.
- **Batch Tool Invocation:** Tools must be invoked in parallel batches without unnecessary intermediate roundtrips.

### 8. Versioning Algorithm (Semantic Versioning)
- **MAJOR**: Full UI redesign, incompatible architectural changes (e.g., from 0.x.x to 1.0.0).
- **MINOR**: Adding new major features (e.g., new section, overlay) without breaking backward compatibility (e.g., from 0.0.1 to 0.1.0).
- **PATCH**: Bug fixes, minor UI tweaks, localization (e.g., from 0.0.1 to 0.0.2).
- **BUILD**: Automatically incremented in CI/CD (GitHub Actions) on every commit.

---

## 🇷🇺 Русский

### 1. Фундаментальные правила работы агентов
- **Согласование изменений и зоны ответственности:** Агентам запрещено проводить несогласованные кросс-модульные рефакторинги или вносить критические архитектурные изменения без явного одобрения пользователя или менеджера. Агенты работают строго в рамках своей зоны ответственности.
- **Запрет шелл-редактирования:** Изменение файлов с помощью консольных утилит (`sed`, `awk`, `echo >` и т.д.) строго запрещено. Агенты должны использовать исключительно предназначенные инструменты редактирования файлов IDE (`write_file`, `replace_file_content`, `multi_replace_file_content`), чтобы избежать рассинхронизации буфера памяти IDE.
- **Обязательная локальная релизная проверка перед Git Push**: Перед отправкой любого коммита на GitHub (`git push`), Coder Agent обязан в 100% случаев успешно выполнить локальную релизную сборку `./gradlew :app:assembleRelease`. Отправка некомпилирующегося или ломающего CI/CD кода категорически запрещена.

### 2. Архитектура и стек технологий
- **Многомодульная Clean Architecture:**
  - `:app`: Главный модуль приложения. Отвечает за связывание зависимостей, верхнеуровневый UI и навигацию на **Kotlin** + Jetpack Compose.
  - `:core:domain`: Чистая бизнес-логика, use-cases и сущности. Модуль написан **СТРОГО на Java 11+** (плагин `java-library`) и **НЕ содержит зависимостей Android SDK**.
  - `:feature:*`: Изолированные модули функционала (например, `:feature:dashboard`), содержащие Compose UI и логику представления на **Kotlin**.
- **Требования к стеку:**
  - **UI:** Исключительно Jetpack Compose (строгий запрет на XML-разметку).
  - **Навигация:** Jetpack Navigation 3 (`androidx.navigation3`).
  - **Минимальный SDK:** 31 (Android 12).
  - **Управление зависимостями:** Централизованно через Gradle Version Catalog (`gradle/libs.versions.toml`).
- **Оптимизация размера APK (R8/ProGuard):** Агенты обязаны использовать агрессивное сжатие для релизных сборок (`isMinifyEnabled = true`, `isShrinkResources = true`). Запрещено использовать слишком широкие правила исключений (например, `-keep class androidx.compose.** { *; }`), которые раздувают размер приложения. Размер релизного APK не должен превышать ~5-10 МБ.

### 3. Стандарты документации и двуязычия
- **Обязательное двуязычие:** Вся ключевая архитектурная, проектная документация и описание требований обязаны поддерживаться в двух версиях: английской (EN) и русской (RU).
- **100% паритет строковых ресурсов:** Файлы `res/values/strings.xml` (EN) и `res/values-ru/strings.xml` (RU) должны поддерживать 100% паритет ключей. Проверка паритета осуществляется командой `./gradlew :checkLocalization`.
- **Запрет хардкода строк в UI:** Использование хардкода строковых литералов непосредственно в Compose-компонентах строго запрещено. Все тексты должны выноситься в ресурсы и вызываться через `stringResource(R.string...)`.

### 4. Протокол QA, тестирования и оптимизации UX
- **Сквозное тестирование кнопок на эмуляторе:** После каждого этапа доработки QA-агент обязан выполнять автоматизированную или интерактивную проверку работоспособности всех кнопок, вкладок и элементов интерфейса.
- **Оптимизация UX и авто-обновление:** Устранение ручных и повторяющихся действий пользователя (например, замена ручных кнопок «Обновить» / «Refresh» на автоматическую реактивную подписку, авто-обновление данных при событии `ON_RESUME` или фоновую синхронизацию).
- **Обязательная проверка запуска релизного APK:** После каждой успешной сборки релизного APK (`:app:assembleRelease`) агент ОБЯЗАН установить его на локальный эмулятор (`adb install -r ...`) и проверить успешный запуск (`adb shell monkey -p ru.doGood.Lynk -c android.intent.category.LAUNCHER 1`), считав логи (`adb logcat -d -b crash`), чтобы исключить падения на старте.

### 5. Экосистема и роли ИИ-агентов
1. **Engineering Manager Agent (Менеджер проекта):** Оркестрация процесса разработки, прямое общение с пользователем, ведение плана реализации и бэклога, делегирование задач.
2. **Product Design Agent (`product_design_agent` / Проектировщик продукта):** Генерация проектного брифа, спецификаций требований (PRD), архитектурной документации и спецификаций UI/UX.
3. **Coder Agent (`coder_agent` / Программист):** Разработка бизнес-логики на чистой Java 11+ в `:core:domain`, создание Compose UI на Kotlin в модулях фич и приложения, настройка Gradle и Git.
4. **Critic Agent (`critic_agent` / QA-тестировщик):** Развертывание на эмуляторе, сквозное интерактивное тестирование UI, мониторинг системных логов и верификация исправлений.
5. **Code Review & Security Auditor Agent (Агент аудита кода и безопасности):** Автоматический статический анализ (Checkstyle/ktlint), аудит уязвимостей зависимостей и контроль соблюдения границы «чистой Java 11+» в `:core:domain`.
6. **Localization Agent (`localization_agent` / Агент локализации):** Управление строковыми ресурсами (`values/strings.xml` и `values-ru/strings.xml`), обеспечение 100% паритета ключей и устранение хардкода строк.

### 6. Безопасность и релизы CI/CD
- **Подпись релиза в CI/CD:** Релизные сборки подписываются строго через GitHub Repository Secrets (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
- **Локальная разработка:** Для локальных сборок используется стандартный Debug keystore.
- **Запрет коммита ключей:** Категорически запрещено коммитить `.keystore` и файлы закрытых ключей подписи в Git-репозиторий.

### 7. Эффективность расхода токенов
- **Лаконичность и точность ответов:** Ответы агентов должны быть точными, лаконичными и не содержать избыточных вступлений или заключений.
- **Запрет технического спама и листинга кода пользователю:** Менеджер и агенты обязаны писать пользователю строго **краткий итоговый результат на простом понятном языке** без технических подробностей программирования (без листингов кода, названий классов, функций и внутренних промежуточных шагов).
- **Формат ответа пользователю:** Только что сделано для пользователя и что можно проверять.
- **Запрет дублирования кода:** Запрещено дублировать фрагменты кода; отвечать строго по существу.
- **Пакетный вызов инструментов:** Инструменты вызываются параллельными пакетами (batch calls) без лишних промежуточных итераций.

### 8. Алгоритм изменения версий (Semantic Versioning)
- **MAJOR (Мажорная)**: Полная переработка UI, несовместимые архитектурные изменения (например, с 0.x.x на 1.0.0).
- **MINOR (Минорная)**: Добавление новых крупных функций (например, новый раздел, оверлей) без нарушения старой работы (с 0.0.1 на 0.1.0).
- **PATCH (Патч)**: Исправление багов, мелкие правки UI, локализация (с 0.0.1 на 0.0.2).
- **BUILD (Сборка)**: Увеличивается автоматически в CI/CD (GitHub Actions) при каждом коммите.
