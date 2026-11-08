# Implementation Plan - UI Reconstruction & Alarm Permission Fix

This plan covers the UI/UX reconstruction of the "Jadwalin" app according to the modern design system specifications and fixes the `SecurityException` related to exact alarm scheduling.

## User Review Required

> [!IMPORTANT]
> The UI reconstruction will involve significant changes to the global theme, colors, and typography. Please verify if you have the "Plus Jakarta Sans" font files ready to be added to the project. I will set up the XML definitions, but the actual `.ttf` or `.otf` files need to be placed in `res/font/`.

> [!NOTE]
> For the Alarm Permission fix, I will ensure that the app handles both `SCHEDULE_EXACT_ALARM` and `USE_EXACT_ALARM` correctly for different Android versions (API 31-35).

## Proposed Changes

### 1. Alarm Permission & Scheduling Fix
Fixes the `SecurityException` when scheduling exact alarms.

#### [MODIFY] [AlarmScheduler.kt](file:///C:/Users/user/StudioProjects/Jadwalin-App/app/src/main/java/com/oxygen/jadwalin/util/AlarmScheduler.kt)
- Improve the `SecurityException` handling.
- Ensure `setAlarmClock` is used only when permission is verified.
- Add better logging for debugging.

#### [MODIFY] [MainActivity.kt](file:///C:/Users/user/StudioProjects/Jadwalin-App/app/src/main/java/com/oxygen/jadwalin/MainActivity.kt)
- Refine the permission request logic to handle API 31+ specifically.

---

### 2. Design System & Theme (UI Reconstruction)
Implementing the new design tokens and typography.

#### [MODIFY] [colors.xml](file:///C:/Users/user/StudioProjects/Jadwalin-App/app/src/main/res/values/colors.xml)
- Add Primary Brand Color (#2563EB).
- Add Background Surface Color (#FAF8FF).
- Add Category Chip colors (Kuliah, Kerja, Pribadi).

#### [MODIFY] [themes.xml](file:///C:/Users/user/StudioProjects/Jadwalin-App/app/src/main/res/values/themes.xml)
- Update `Theme.Jadwalin` to use the new color tokens.
- Set default card elevation and corner radius.
- Configure typography.

#### [NEW] [fonts.xml](file:///C:/Users/user/StudioProjects/Jadwalin-App/app/src/main/res/font/plus_jakarta_sans.xml)
- Define the font family for Plus Jakarta Sans.

---

### 3. Modular UI Components
Creating reusable styles and drawables for the modern UI.

#### [NEW] [bg_card_modern.xml](file:///C:/Users/user/StudioProjects/Jadwalin-App/app/src/main/res/drawable/bg_card_modern.xml)
- Modern card background with 16dp-20dp rounded corners and soft shadow.

#### [NEW] [bg_bottom_dock.xml](file:///C:/Users/user/StudioProjects/Jadwalin-App/app/src/main/res/drawable/bg_bottom_dock.xml)
- Floating dock bar background with high corner radius and elevation.

#### [NEW] [styles_chips.xml](file:///C:/Users/user/StudioProjects/Jadwalin-App/app/src/main/res/values/styles_chips.xml)
- Modular styles for category chips (Kuliah, Kerja, Pribadi).

---

### 4. Layout Updates
Applying the new design to the main container and login screen.

#### [MODIFY] [activity_main.xml](file:///C:/Users/user/StudioProjects/Jadwalin-App/app/src/main/res/layout/activity_main.xml)
- Implement the Floating Dock Bar with a central FAB.

#### [MODIFY] [fragment_login.xml](file:///C:/Users/user/StudioProjects/Jadwalin-App/app/src/main/res/layout/fragment_login.xml)
- Update the login screen UI to match the modern design tokens.

## Verification Plan

### Automated Tests
- Run unit tests for `AlarmScheduler` to ensure it doesn't crash even if permissions are missing (fallback logic).

### Manual Verification
- Deploy to an Android 13+ device to verify the "Floating Dock Bar" appearance.
- Test scheduling an alarm to ensure the `SecurityException` is no longer crashing the app and that the fallback works if permission is revoked.
- Verify typography and color scheme in the Login screen.
