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

### 4. QA & UX Optimization Protocol
- **Full Interactive Controls Testing:** After every iteration or enhancement stage, the testing agent must verify the functionality of all buttons, tabs, and interactive UI controls across the application.
- **Redundancy Elimination & Auto-Actions:** The agent must automatically identify and propose ways to eliminate manual or repetitive user actions (e.g., replacing manual "Refresh" buttons with automatic reactive subscriptions, auto-updates triggered on the `ON_RESUME` lifecycle event, or background synchronization).

### 5. App Signing
- **Single Keystore Requirement:** All builds (both Release and Debug) MUST be signed with the unified `lynk_release.keystore` to ensure seamless updates and avoid "App not installed" errors for the end user when transitioning between development and production versions.

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

### 4. Протокол проверки качества и оптимизации UX
- **Обязательное сквозное тестирование кнопок (Full Interactive Controls Testing):** После каждого этапа доработки агент тестирования должен проверять работоспособность всех кнопок, вкладок и интерактивных элементов интерфейса приложения.
- **Исключение лишних действий пользователя (Redundancy Elimination & Auto-Actions):** Агент должен автоматически выявлять и предлагать способы устранения ручных/повторяющихся действий пользователя (например, заменять ручные кнопки «Обновить»/«Refresh» на автоматическую реактивную подписку, авто-обновление по событию `ON_RESUME` жизненного цикла или фоновую синхронизацию).

### 5. Подпись приложения
- **Единый ключ подписи (Keystore):** Все сборки (как Release, так и Debug) ДОЛЖНЫ подписываться единым ключом `lynk_release.keystore`, чтобы гарантировать бесшовные обновления и избежать ошибки «Приложение не установлено» у конечного пользователя при переходе между тестовыми и рабочими версиями.
