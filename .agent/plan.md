# Project Plan

Приложение Lynk. Поддержка Android 12+, Material Design, модульная архитектура, полная документация кода, ведение беклога, аудит на двойные правила. Агенты выполняют полный цикл разработки. Поддержка мобильных и ультра-широких экранов в авто. Основной язык логики - Java, UI - Kotlin (Compose). Строгое согласование изменений с пользователем.

## Project Brief

# Project Brief / Проектный бриф: Lynk

**Focus:** Minimum Viable Product (MVP) / **Фокус:** Минимально жизнеспособный продукт (MVP)

## Features / Основные функции

1. **Agent Workspace & Approval Flow / Рабочее пространство агентов и цикл согласования:**
   - *EN:* Base environment supporting a full-cycle agent-driven development process with strict user-approval gates.
   - *RU:* Базовая среда, поддерживающая полный цикл разработки под управлением ИИ-агентов со строгим процессом обязательного согласования изменений с пользователем.

2. **Adaptive Dashboard / Адаптивная панель управления:**
   - *EN:* A dynamic UI adapting to standard mobile devices and ultra-wide automotive displays.
   - *RU:* Динамический пользовательский интерфейс, плавно адаптирующийся как под стандартные мобильные экраны, так и под автомобильные ультраширокие дисплеи.

3. **Backlog & Audit Engine / Управление беклогом и аудит:**
   - *EN:* Core interfaces for tracking tasks, maintaining full project documentation, and automatically auditing duplicate rules.
   - *RU:* Встроенные интерфейсы для отслеживания задач, ведения полной документации проекта и автоматического аудита на предмет двойных правил.

4. **Hybrid Logic-UI Integration / Гибридная интеграция логики и интерфейса:**
   - *EN:* Architectural foundation enforcing strict separation between Kotlin-based UI and Java-based business logic.
   - *RU:* Базовый архитектурный фундамент, обеспечивающий строгое разделение между слоем пользовательского интерфейса (на Kotlin) и бизнес-логикой (на Java).

## High-Level Tech Stack / Высокоуровневый технологический стек

* **UI & Presentation / Интерфейс и слой представления:**
  * *EN:* Kotlin, Jetpack Compose, Material Design.
  * *RU:* Kotlin, Jetpack Compose, Material Design.
* **Business Logic / Бизнес-логика:**
  * *EN:* Java.
  * *RU:* Java.
* **Navigation & Adaptive Strategy / Стратегия навигации и адаптивности:**
  * *EN:* Jetpack Navigation 3 (state-driven) & Compose Material Adaptive library.
  * *RU:* Строго **Jetpack Navigation 3** (навигация на основе состояний) и библиотека **Compose Material Adaptive** (для поддержки всех размеров экранов).
* **Asynchronous Operations / Асинхронные операции:**
  * *EN:* Kotlin Coroutines.
  * *RU:* Kotlin Coroutines.
* **System Requirements / Системные требования:**
  * *EN:* Android 12+ (API Level 31+).
  * *RU:* Минимальная версия Android 12+ (API Level 31+).
* **Architecture / Архитектура:**
  * *EN:* Modular Architecture.
  * *RU:* Модульная архитектура (Modular Architecture).

## Implementation Steps
**Total Duration:** 50m 48s

### Task_1_Setup_And_Architecture: Fetch project from GitHub (user to provide details), set up Modular Architecture separating Java logic and Kotlin UI, and initialize documentation and backlog tracking files.
- **Status:** COMPLETED
- **Updates:** Project fetched into temporary scratch directory for study. Modular architecture created with :app, :feature:dashboard (Kotlin/Compose) and :core:domain (Java). Centralized gradle version catalog used. MinSdk 31 set. ARCHITECTURE_RULES.md and BACKLOG.md created.
- **Acceptance Criteria:**
  - GitHub project fetched successfully
  - Modular architecture setup with Java and Kotlin modules
  - Documentation and backlog files created
- **Duration:** 8m 54s

### Task_2_Core_Logic_Java: Implement core business logic in Java for Agent Workspace, Approval Flow, and Backlog & Audit Engine.
- **Status:** COMPLETED
- **Updates:** Implemented core business logic in pure Java in the :core:domain module. Created packages and classes for Agent Workspace (Agent, AgentTask, TaskStatus), Approval Flow (ApprovalRequest, ApprovalStatus, ApprovalManager), Backlog Engine (BacklogItem, BacklogManager), and Audit Engine (AuditRule, AuditResult, RuleAuditor) with full Javadoc. Code compiled successfully.
- **Acceptance Criteria:**
  - Java modules contain core logic for Agent Workspace and Approval Flow
  - Audit Engine logic is implemented in Java
- **Duration:** 7m

### Task_3_Adaptive_UI_Navigation: Implement the Adaptive Dashboard (phone and ultra-wide automotive displays) using Jetpack Compose, Material Design, and Jetpack Navigation 3 in Kotlin.
- **Status:** COMPLETED
- **Updates:** Configured Compose Adaptive and Navigation 3 libraries. Created DashboardViewModel connecting to Java domain models. Developed DashboardListScreen and DashboardDetailScreen. Integrated NavDisplay and ListDetailSceneStrategy in MainActivity for state-driven adaptive navigation. Build verified successfully.
- **Acceptance Criteria:**
  - Adaptive UI handles standard and ultra-wide displays
  - Jetpack Navigation 3 is implemented
  - Kotlin UI successfully connects to Java business logic
- **Duration:** 11m 8s

### Task_4_Audit_Scripts_Integration: Implement automated audit scripts for double-rules and finalize the hybrid logic-UI integration.
- **Status:** COMPLETED
- **Updates:** Implemented concrete RuleAuditor with ArchitecturalRule and DuplicatePatternRule in Java. Created a Gradle task (runAudit) in core:domain to execute the audit manually. Integrated auditing into the Kotlin UI by adding a Run Audit button and AlertDialog in the Compose UI. Build verified.
- **Acceptance Criteria:**
  - Automated audit scripts created and functional
  - Seamless integration between Java logic and Kotlin UI components
- **Duration:** 2m 56s

### Task_5_Run_And_Verify: Run and Verify: Instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** App built and deployed successfully on phone emulator. No crashes detected. Architectural Audit button works and displays results in a dialog. Navigation between list and detail views works correctly. UI is edge-to-edge with no critical issues detected.
- **Acceptance Criteria:**
  - make sure all existing tests pass
  - build pass
  - app does not crash
- **Duration:** 5m 37s

### Task_6_Fullscreen_Domain_Migration: Analysis & Preparation: Review scratch/Fullscreen directory to categorize code. Domain Migration: Port business logic, models, and non-UI utilities to pure Java inside the :core:domain module.
- **Status:** COMPLETED
- **Updates:** Successfully analyzed scratch/Fullscreen and extracted pure business logic and models. Ported AppItem, FileItem, DeviceInfo, Installer interfaces and WaterfallInstallStrategy into pure Java inside the :core:domain module. All Android UI and Context dependencies were successfully decoupled. BACKLOG.md updated.
- **Acceptance Criteria:**
  - Fullscreen logic ported to Java in :core:domain
  - Code categorizes correctly without UI dependencies
- **Duration:** 4m 5s

### Task_7_Fullscreen_UI_Migration_Integration_Verify: UI Migration: Port Fullscreen screens to Jetpack Compose in :feature:dashboard. Connect Kotlin UI to Java domain logic. Update BACKLOG.md. Run and Verify: Instruct critic_agent to verify application stability (no crashes), confirm alignment with user requirements, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Successfully ported Fullscreen screens (FileManager, ApkInstaller, SystemInfo) into Compose UI. Connected to Java domain logic via ViewModel. Updated BACKLOG.md to bilingual EN/RU format. Critic agent verified deployment, navigation across all screens, interactive controls, and logcat output on emulator with zero crashes.
- **Acceptance Criteria:**
  - Fullscreen UI implemented in Compose
  - UI connects to Java logic via ViewModel
  - BACKLOG.md updated
  - make sure all existing tests pass
  - build pass
  - app does not crash
- **Duration:** 10m 54s

### Task_8_OTA_Domain_And_Download_Mechanisms: Configurability & Domain Logic: Add repo path to strings.xml, implement Java HTTP client in :core:domain for GitHub API, and update UpdateInfo model. Download & Install: Implement DownloadManager service and configure FileProvider in AndroidManifest.xml.
- **Status:** COMPLETED
- **Updates:** Task 8 (OTA Domain Logic) was actually already completed in previous steps by the coder_agent. Marking it back to COMPLETED to maintain correct plan state.
- **Acceptance Criteria:**
  - Repo path configurable in strings.xml
  - Pure Java HTTP client implemented in :core:domain
  - DownloadManager and FileProvider setup correctly
- **Duration:** 10s

### Task_9_OTA_UI_Integration_And_Verify: UI Integration: Update AppUpdateScreen.kt and DashboardViewModel.kt for OTA flow (Check -> Download -> Install). Run and Verify: Instruct critic_agent to verify application stability (no crashes), confirm alignment with OTA requirements, and report critical UI issues.
- **Status:** COMPLETED
- **Updates:** Task 9 (OTA UI Integration and Verification) was also already completed by the coder_agent in previous steps. Marking it back to COMPLETED.
- **Acceptance Criteria:**
  - OTA flow UI integrated with ViewModel
  - make sure all existing tests pass
  - build pass
  - app does not crash
- **Duration:** 4s

### Task_10_Agent_Team_Architecture_Analysis: Analyze existing agents (Manager, Design, Coder, Critic) and document their roles in ARCHITECTURE_RULES.md. Propose and add conceptual agents (Code Reviewer/Linting, Translation) to the rules for a fully autonomous pipeline. Explain the setup to the user in a markdown artifact.
- **Status:** IN_PROGRESS
- **Acceptance Criteria:**
  - ARCHITECTURE_RULES.md updated with existing and proposed agents
  - Conceptual Code Reviewer and Translation roles documented
- **StartTime:** 2026-10-06 09:43:14 MSK

