# PrI meaaaoject Plan

Add a "Quick Launch" feature to the AutoGrabber Dashboard to directly open Spark Driver, DoorDash, and Uber apps. Handle cases where the app is not installed with appropriate feedback.

## Project Brief

# Project Brief: AutoGrabber (MVP)

AutoGrabber is a specialized utility designed for gig economy drivers (Spark, DoorDash, and Uber) to centralize platform management. The app combines precision filtering tools with quick-access launch capabilities to optimize driver efficiency and earnings.

### Features
*   **Intelligent Gig Dashboard:** A unified control center for Spark, DoorDash, and Uber, featuring primary On/Off toggles for each service and a "Quick Launch" shortcut to open the respective driver apps directly.
*   **Precision Filter Management:** Dedicated configuration pages for each platform using synchronized Slider and TextField inputs, allowing drivers to set exact "Minimum Pay" and "Maximum Distance" thresholds.
*   **Quick Launch & Intent Handling:** Integrated shortcut system that intelligently detects installed driver applications and provides user feedback if a specific platform app (Spark, DoorDash, or Uber) is not found.
*   **Persistent Local Storage:** Automatic synchronization of all filter preferences and platform states using Jetpack DataStore, ensuring driver configurations are preserved across app restarts.
*   **Adaptive Material 3 Design:** A modern, state-driven interface built with Material 3 that supports seamless Light/Dark mode transitions and responsive layouts for various screen sizes.

### High-Level Technical Stack
*   **Language:** Kotlin
*   **UI Framework:** Jetpack Compose (Material 3)
*   **Navigation:** Jetpack Navigation 3 (State-driven architecture)
*   **Adaptive Strategy:** Compose Material Adaptive library for multi-pane and responsive layouts
*   **Concurrency:** Kotlin Coroutines & Flow
*   **Persistence:** Jetpack DataStore (Preferences DataStore)

## Implementation Steps
**Total Duration:** 26m 45s

### Task_1_CoreSetup_Data_Theme: Set up the core data layer and Material 3 theme. This includes defining data models for GigOffers and FilterPreferences, implementing DataStore for persistence, and configuring a True Black Material 3 theme with vibrant accents.
- **Status:** COMPLETED
- **Updates:** Data models for GigOffer and FilterPreferences created. SettingsRepository using DataStore implemented for persisting filters and theme preferences. Material 3 theme configured with True Black background and vibrant accents (Electric Blue, Neon Green). Project builds successfully with compileSdk/targetSdk 37.
- **Acceptance Criteria:**
  - DataStore correctly persists driver filters (min pay, max distance)
  - Material 3 theme implemented with True Black support
  - Project builds successfully

### Task_2_Navigation_Dashboard_Settings: Implement Jetpack Navigation 3 and the primary UI screens. Create the Unified Dashboard for Spark, DoorDash, and Uber, and the Settings screen for filter configuration.
- **Status:** COMPLETED
- **Updates:** Implemented Jetpack Navigation 3 with Dashboard and Settings screens. Dashboard displays sections for Spark, DoorDash, and Uber. Settings screen allows modifying filters and True Black mode. Full Edge-to-Edge display implemented. Project builds successfully.
- **Acceptance Criteria:**
  - Navigation between Dashboard and Settings works
  - Dashboard displays separate sections for Spark, DoorDash, and Uber
  - Settings screen allows modifying filter preferences
  - Full Edge-to-Edge display implemented
- **Duration:** 4m 18s

### Task_3_OfferLogic_Simulation: Develop the filtering logic and simulated gig offer streams. Implement the engine that matches incoming offers against user preferences and highlights them in the UI.
- **Status:** COMPLETED
- **Updates:** Implemented filtering engine and real-time offer simulation for Spark, DoorDash, and Uber. Dashboard now displays incoming offers with visual "MATCH" highlighting for those meeting user criteria. State updates reactively based on DataStore settings. Project builds successfully.
- **Acceptance Criteria:**
  - Offers are filtered based on pay-per-mile and distance criteria
  - Matching offers are visually highlighted on the dashboard
  - Simulated offer streams for Spark, DoorDash, and Uber are functional
- **Duration:** 47s

### Task_4_AppIcon_FinalPolish_Verification: Finalize the app with an adaptive icon, UI polish, and a comprehensive verification run. Ensure the app meets Material Design 3 guidelines and stability requirements.
- **Status:** COMPLETED
- **Updates:** The coder_agent finalized the app with an adaptive app icon, UI polish (vibrant colors, high-impact typography), and full edge-to-edge support. The critic_agent performed a comprehensive verification run, confirming that the app is stable, fulfills all functional requirements (Unified Dashboard, Smart Filters, Real-time Simulation, True Black theme), and follows Material 3 guidelines perfectly. No issues were found.
- **Acceptance Criteria:**
  - Adaptive app icon matching the gig driver theme is present
  - Final UI polish with vibrant colors and energetic design
  - App does not crash and passes all basic unit tests
  - Critic agent verifies stability and requirement alignment
- **Duration:** 4m 33s

### Task_5_PlatformUI_and_Filters: Refactor the Dashboard into a button-based UI with individual toggles for Spark, DoorDash, and Uber. Implement dedicated filter preference screens for each platform with state persistence.
- **Status:** COMPLETED
- **Updates:** Refactored Dashboard into a button-based UI with individual On/Off toggles for Spark, DoorDash, and Uber. Implemented dedicated filter preference screens for each platform with state persistence in DataStore. Updated navigation and offer simulation to respect platform-specific settings and active status.
- **Acceptance Criteria:**
  - Dashboard features buttons and On/Off toggles for each platform
  - Individual filter pages for Spark, DoorDash, and Uber are fully functional
  - Platform status and filter settings persist via DataStore
- **Duration:** 1m 59s

### Task_6_AdaptiveLayout_Theme_FinalRun: Integrate the Compose Material Adaptive library for multi-pane layouts on large screens and add a Light/Dark mode toggle. Perform final stability verification and requirement check.
- **Status:** COMPLETED
- **Updates:** Integrated the Compose Material Adaptive library to provide a side-by-side multi-pane layout on tablets and foldables. Added a Light/Dark mode toggle to the Settings screen that updates the app theme reactively. Verified that the "True Black" background is maintained in Dark mode. The app is stable and meets all updated UI requirements.
- **Acceptance Criteria:**
  - App displays side-by-side panes on tablets/foldables
  - Settings screen features a working Light/Dark theme toggle
  - App builds, does not crash, and satisfies all MVP requirements
  - All existing tests pass
- **Duration:** 1m 12s

### Task_7_Redesign_Theme_and_UI: Redesign the application to a clean, modern Material 3 interface. Remove the OLED/True Black theme and the live offer simulation logic and UI. Implement a standard Light/Dark theme that respects system settings.
- **Status:** COMPLETED
- **Updates:** Redesigned the application to a clean, modern Material 3 interface. Removed the OLED/True Black toggle and the live offer simulation logic and UI. Implemented a standard Light/Dark theme that respects system settings. The app now focuses on managing gig platform filter preferences with a professional aesthetic. Project builds successfully.
- **Acceptance Criteria:**
  - OLED/True Black toggle and logic are removed
  - Live offer simulation is removed from UI and codebase
  - Dashboard and platform screens follow a clean, professional Material 3 design
  - Theme correctly switches between standard Light and Dark modes
- **Duration:** 1m 37s

### Task_8_Final_Verification_Redesign: Finalize the new UI, verify that all platform filters (Spark, DoorDash, Uber) work correctly with the new design, and ensure the adaptive layout is stable. Conduct a final run to confirm stability and requirement alignment.
- **Status:** COMPLETED
- **Updates:** The critic_agent successfully verified the complete redesign of AutoGrabber. The app is now a stable, professional configuration utility for gig drivers. The live offer simulation and OLED toggles have been removed. The Dashboard features clean cards for Spark, DoorDash, and Uber with functional toggles. Individual filter pages are operational and persist data. Standard Light/Dark theme and adaptive layouts are correctly implemented and stable. No crashes or UI issues were found.
- **Acceptance Criteria:**
  - Filter configurations for all platforms are correctly persisted and editable
  - Adaptive multi-pane layout works seamlessly with the new design
  - App builds without errors and does not crash
  - Critic agent confirms alignment with 'Clean/Modern M3' requirements
- **Duration:** 2m 31s

### Task_9_UI_Refinements_and_Layout: Apply specific UI refinements: update platform status labels to 'Currently Active/Inactive', implement a unified TopAppBar for the Dashboard, remove redundant headings, and reposition version info in Settings.
- **Status:** COMPLETED
- **Updates:** Refined Dashboard UI: Moved 'AutoGrabber' title and settings button to a unified TopAppBar, updated status labels to 'Currently Active/Inactive', and removed the 'Platform Assistance' heading. Moved platform cards higher. Repositioned version info to the bottom of the Settings screen. Fixed the 'white flash' issue by adding a Surface wrapper with the background color in the main layout.
- **Acceptance Criteria:**
  - Dashboard uses a unified TopAppBar with app name and settings icon
  - Status labels display 'Currently Active' and 'Currently Inactive'
  - 'Platform Assistance' heading is removed and platform cards are positioned higher
  - Version info is moved to the bottom of the Settings screen
- **Duration:** 3m 47s

### Task_10_Visual_Polish_and_Verification: Address visual smoothness by fixing the 'white flash' issue during transitions. Perform a final verification run to ensure all UI refinements are stable and meet requirements.
- **Status:** COMPLETED
- **Updates:** The coder_agent applied all requested UI refinements, including updating status labels to 'Currently Active/Inactive', restructuring the Dashboard header with a TopAppBar, removing redundant text, and repositioning the version info in Settings. The 'white flash' issue was resolved by adding a Surface wrapper. The critic_agent verified all changes, confirming the app is stable, visually smooth, and aligns perfectly with the refinements brief.
- **Acceptance Criteria:**
  - 'White flash' issue during theme or screen transitions is resolved
  - All UI refinements from Task 9 are verified functional
  - App builds pass
  - all existing tests pass
  - app does not crash
  - Critic agent verifies stability and alignment with the UI Refinements brief
- **Duration:** 2m 46s

### Task_11_RenameUber_DataStorePersistence: Rename 'Uber Eats' to 'Uber' across the application and implement robust DataStore persistence for all platform toggles and filter settings.
- **Status:** COMPLETED
- **Updates:** Renamed all instances of 'Uber Eats' to 'Uber' in UI and data models. Verified and ensured that all platform toggles (Spark, DoorDash, Uber) and their respective filter values (min pay, max distance) are robustly persisted in DataStore and survive app restarts. Project builds successfully.
- **Acceptance Criteria:**
  - 'Uber Eats' is renamed to 'Uber' in UI labels, models, and repositories
  - DataStore correctly persists all platform ON/OFF states and filter values across app restarts
- **Duration:** 1m 19s

### Task_12_DualInputFilters_FinalVerification: Implement dual-input filter controls (synchronized sliders and numeric text fields) for Minimum Pay and Maximum Distance. Perform a final verification run.
- **Status:** COMPLETED
- **Updates:** Fixed the state-saving crash by applying the 'kotlin-parcelize' plugin and updating all 'Destination' classes and the 'Platform' enum to implement Parcelable. This allows the navigation state to be safely persisted during configuration changes and backgrounding. Project builds successfully.
- **Acceptance Criteria:**
  - Platform preference pages feature synchronized Sliders and TextFields for precise entry
  - Filter values update reactively between Slider and TextField inputs
  - Build pass, all tests pass, and app does not crash
  - Critic agent verifies stability and alignment with user requirements
- **Duration:** 49s

### Task_13_QuickLaunch_Functionality: Implement 'Quick Launch' functionality for Spark, DoorDash, and Uber on the Dashboard. This includes intent handling to open external driver apps directly and providing appropriate feedback if an app is not installed.
- **Status:** COMPLETED
- **Updates:** Implemented 'Quick Launch' functionality for Spark, DoorDash, and Uber on the Dashboard. Added 'Quick Launch' buttons to platform cards. Implemented intent logic using PackageManager to open external apps. Added Toast feedback for missing apps. Verified build stability.
- **Acceptance Criteria:**
  - Dashboard features 'Quick Launch' buttons for Spark, DoorDash, and Uber
  - Clicking a launch button opens the respective driver app if installed
  - Clear user feedback (e.g., Toast or Snackbar) is provided if the target app is not found
  - Intent logic correctly identifies the package names for all target platforms
- **Duration:** 1m 7s

### Task_14_Final_Verification_QuickLaunch: Final verification run for the Quick Launch feature and overall application. Confirm stability, persistence, and adherence to Material 3 guidelines.
- **Status:** IN_PROGRESS
- **Updates:** Refined the 'Open App' feature: The launch button for DoorDash is now labeled 'Open Dasher'. All launch buttons ('Open App' / 'Open Dasher') are now automatically disabled (grayed out) if the corresponding driver app is not detected on the device. Package names verified: Spark (com.walmart.sparkdriver), Dasher (com.doordash.driverapp), and Uber (com.ubercab.driver). Project builds successfully.
- **Acceptance Criteria:**
  - Quick Launch functionality works as intended across all platforms
  - All filter preferences and platform states persist correctly
  - App builds pass, all existing tests pass, and the app does not crash
  - Critic agent confirms alignment with the 'Quick Launch' brief and overall MVP quality
- **StartTime:** 2026-05-17 21:45:36 EDT

