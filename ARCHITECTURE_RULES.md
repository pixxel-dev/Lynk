# Architecture Rules / Архитектурные правила

This document defines the architectural guidelines and agent operating procedures for the `Lynk` project.
Данный документ определяет архитектурные принципы и правила работы агентов для проекта `Lynk`.

## 🇬🇧 English

### 1. Architecture Overview
The project follows a multi-module Clean Architecture approach:
- `:app`: The main Android application module. Wires up dependencies, navigation (Navigation 3), and top-level UI components. Written in **Kotlin** and Jetpack Compose.
- `:core:domain`: Contains the core business logic, use cases, and entities. This module is **STRICTLY Java** (pure `java-library` plugin) and must not contain any Android framework dependencies.
- `:feature:*`: Encapsulated feature modules (e.g., `:feature:dashboard`). Contains Compose UI and presentation logic. Written in **Kotlin**.

### 2. Tech Stack Requirements
- **UI:** Jetpack Compose (strictly no XML).
- **Navigation:** Jetpack Navigation 3 (`androidx.navigation3`).
- **Minimum SDK:** 31 (Android 12).
- **Dependency Management:** Centralized via `gradle/libs.versions.toml`.

### 3. Agent Operating Rules (CRITICAL)
1. **Language constraints:** Business logic inside `:core:domain` MUST be written in **Java**. UI inside `:app` and `:feature:*` MUST be written in **Kotlin**.
2. **No cross-module pollution:** Do not add Android dependencies to `:core:domain`.
3. **Cross-module changes:** Agents **must not** make cross-module refactoring changes (e.g., changing a domain model that breaks feature modules) without explicitly asking the user or manager for approval first. 
4. **Tool constraints:** Modifying files via shell tools (e.g., `sed`, `awk`) is strictly prohibited. Use the designated file editing tools.
5. **Localization enforcement:** Hardcoding strings in Compose UI files is strictly prohibited. All user-facing text must use `stringResource(R.string...)` with 100% key parity between `values/strings.xml` (EN) and `values-ru/strings.xml` (RU).

### 4. QA & UX Optimization Protocol
- **Full Interactive Controls Testing:** After every iteration or enhancement stage, the testing agent must verify the functionality of all buttons, tabs, and interactive UI controls across the application.
- **Redundancy Elimination & Auto-Actions:** The agent must automatically identify and propose ways to eliminate manual or repetitive user actions (e.g., replacing manual "Refresh" buttons with automatic reactive subscriptions, auto-updates triggered on the `ON_RESUME` lifecycle event, or background synchronization).

### 5. AI Agent Roles & Ecosystem
The project is developed and maintained by a collaborative multi-agent AI system:

**Active Agents:**
1. **Engineering Manager Agent:** Orchestration, user communication, backlog and implementation plan management, and task delegation.
2. **Product Design Agent (`product_design_agent`):** Generation of project briefs, product requirements (PRD), architecture documentation, and UI/UX design specifications.
3. **Coder Agent (`coder_agent`):** Development of pure Java business logic in `:core:domain`, Kotlin Jetpack Compose UI in feature/app modules, Gradle configuration, and Git version control management.
4. **Critic Agent (`critic_agent` / QA):** Automated emulator deployment, end-to-end interactive testing of all UI controls, system log monitoring, and bug fix verification.
5. **Localization Agent (`localization_agent`):** Automated dual-language string resource management (`res/values/strings.xml` for EN and `res/values-ru/strings.xml` for RU), 100% key parity enforcement, and elimination of hardcoded strings in Jetpack Compose UI.

**Proposed Future Agents (Full Automation Cycle):**
6. **Code Review & Security Auditor Agent:** Automated static code analysis (Checkstyle/ktlint), security vulnerability scanning, dependency auditing, and strict enforcement of the pure Java architectural boundary in `:core:domain`.

### 6. App Signing
- **Single Keystore Requirement:** All builds (both Release and Debug) MUST be signed with the unified `lynk_release.keystore` to ensure seamless updates and avoid "App not installed" errors for the end user when transitioning between development and production versions.

### 7. Localization Protocol & Rules
- **Automatic String Synchronization:** Upon adding any new UI text or modifying existing strings, agents are required to automatically generate and update keys in both `res/values/strings.xml` (EN) and `res/values-ru/strings.xml` (RU).
- **100% Key Parity:** All string resources must maintain 100% key parity between Russian and English languages. Missing translations in either language are strictly prohibited. Parity can be audited at any time via `./gradlew :checkLocalization`.
- **No Hardcoded Compose Strings:** Using hardcoded text literals directly in Compose UI components (e.g., `Text("Hardcoded Text")`) is forbidden. All user-facing text must be referenced via `stringResource(R.string...)`.

---

## 🇷🇺 Русский

### 1. Обзор архитектуры
Проект использует многомодульную Clean Architecture:
- `:app`: Главный модуль приложения. Отвечает за сборку зависимостей, навигацию (Navigation 3) и высокоуровневый UI. Написан на **Kotlin** с использованием Jetpack Compose.
- `:core:domain`: Содержит бизнес-логику, use-cases и сущности (entities). Этот модуль написан **СТРОГО на Java** (чистый плагин `java-library`) и не должен содержать зависимостей Android SDK.
- `:feature:*`: Изолированные модули функционала (например, `:feature:dashboard`). Содержат Compose UI и логику представления. Написаны на **Kotlin**.

### 2. Требования к стеку технологий
- **UI:** Jetpack Compose (никаких XML-разметок).
- **Навигация:** Jetpack Navigation 3 (`androidx.navigation3`).
- **Минимальный SDK:** 31 (Android 12).
- **Управление зависимостями:** Централизованно через `gradle/libs.versions.toml`.

### 3. Правила работы агентов (КРИТИЧЕСКИ ВАЖНО)
1. **Ограничения по языкам:** Бизнес-логика в `:core:domain` ДОЛЖНА быть написана на **Java**. Пользовательский интерфейс в `:app` и `:feature:*` ДОЛЖЕН быть написан на **Kotlin**.
2. **Чистота модулей:** Запрещено добавлять зависимости Android в модуль `:core:domain`.
3. **Кросс-модульные изменения:** Агентам **запрещается** вносить кросс-модульные изменения (например, менять доменную модель, что сломает модули фич) без предварительного согласования с пользователем или менеджером.
4. **Ограничения инструментов:** Модификация файлов через shell-утилиты (`sed`, `awk` и т.д.) строго запрещена. Используйте только встроенные инструменты редактирования файлов.
5. **Требования к локализации:** Строго запрещен хардкод строк в Compose-файлах. Все отображаемые тексты должны вызываться через `stringResource(R.string...)` с обеспечением 100% паритета ключей между `values/strings.xml` (EN) и `values-ru/strings.xml` (RU).

### 4. Протокол проверки качества и оптимизации UX
- **Обязательное сквозное тестирование кнопок (Full Interactive Controls Testing):** После каждого этапа доработки агент тестирования должен проверять работоспособность всех кнопок, вкладок и интерактивных элементов интерфейса приложения.
- **Исключение лишних действий пользователя (Redundancy Elimination & Auto-Actions):** Агент должен автоматически выявлять и предлагать способы устранения ручных/повторяющихся действий пользователя (например, заменять ручные кнопки «Обновить»/«Refresh» на автоматическую реактивную подписку, авто-обновление по событию `ON_RESUME` жизненного цикла или фоновую синхронизацию).

### 5. Экосистема и роли ИИ-агентов
Поддержка и разработка проекта осуществляется совместной экосистемой специализированных ИИ-агентов:

**Активные агенты:**
1. **Engineering Manager Agent (Менеджер проекта):** Оркестрация процесса разработки, прямое общение с пользователем, ведение плана реализации и бэклога, делегирование задач.
2. **Product Design Agent (`product_design_agent` / Проектировщик продукта):** Генерация проектного брифа, спецификаций требований (PRD), архитектурной документации и рекомендаций по UI/UX.
3. **Coder Agent (`coder_agent` / Программист):** Разработка бизнес-логики на чистой Java в `:core:domain`, создание Compose UI на Kotlin в модулях фич и приложения, настройка Gradle и работа с Git.
4. **Critic Agent (`critic_agent` / QA-тестировщик):** Автоматическое развертывание приложения на эмуляторе, сквозное интерактивное тестирование всех элементов UI, анализ логов и верификация исправлений ошибок.
5. **Localization Agent (Агент локализации / `localization_agent`):** Автоматическое управление строковыми ресурсами на двух языках (`res/values/strings.xml` для EN и `res/values-ru/strings.xml` для RU), обеспечение 100% паритета ключей и предотвращение хардкода строк в Compose-файлах.

**Предлагаемые концептуальные агенты (для полного цикла автоматизации):**
6. **Code Review & Security Auditor Agent (Агент статического анализа и безопасности):** Автоматическая проверка кода на соответствие стандартам Checkstyle/ktlint, аудит безопасности и уязвимостей зависимостей, а также строгое соблюдение границы «чистого Java» в модуле `:core:domain`.

### 6. Подпись приложения
- **Единый ключ подписи (Keystore):** Все сборки (как Release, так и Debug) ДОЛЖНЫ подписываться единым ключом `lynk_release.keystore`, чтобы гарантировать бесшовные обновления и избежать ошибки «Приложение не установлено» у конечного пользователя при переходе между тестовыми и рабочими версиями.

### 7. Регламент и правила локализации
- **Автоматическая синхронизация строк:** При любом добавлении нового текста или изменении UI агенты обязаны автоматически генерировать и обновлять ключи в `res/values/strings.xml` (EN) и `res/values-ru/strings.xml` (RU).
- **100% паритет ключей:** Все строковые ресурсы должны иметь 100% паритет ключей между русский и английским языками. Отсутствие ключа в любом из языков строго запрещено. Аудит паритета выполняется командой `./gradlew :checkLocalization`.
- **Запрет хардкода строк в Compose:** Запрещено использовать хардкод строк в Compose-файлах (`Text("Хардкод")`). Все отображаемые пользователю строки должны вызываться исключительно через `stringResource(R.string...)`.
