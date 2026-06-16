# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FuckJunkCall is an Android application that automatically blocks unknown incoming calls (not in contacts) and sends automatic SMS replies. The project uses modern Android development practices with Kotlin and Jetpack Compose.

**Package**: `com.hoseadev.fuckjunkcall`

**Core Features**:
- Automatic call screening using `CallScreeningService`
- Block incoming calls from numbers not in contacts
- Automatic SMS reply to blocked numbers
- Whitelist management
- Call blocking history

## Build Configuration

- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 35
- **JVM Target**: 11
- **Kotlin Version**: 2.0.21
- **AGP Version**: 8.10.0

## Common Commands

### Build and Run
```bash
# Clean build
./gradlew clean

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Install and run on connected device/emulator
./gradlew installDebug
```

### Testing
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests com.hoseadev.fuckjunkcall.ExampleUnitTest

# Run specific instrumented test
./gradlew connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.hoseadev.fuckjunkcall.ExampleInstrumentedTest
```

### Code Quality
```bash
# Lint checks
./gradlew lint

# View lint report
open app/build/reports/lint-results-debug.html
```

## Architecture

### Technology Stack
- **UI Framework**: Jetpack Compose with Material3
- **Database**: Room 2.6.1 for local data persistence
- **Architecture**: Single-Activity with service-based call screening
- **Coroutines**: For asynchronous operations (contacts lookup, database access)
- **Build System**: Gradle with Kotlin DSL + KSP for annotation processing

### Project Structure
```
app/src/main/java/com/hoseadev/fuckjunkcall/
├── MainActivity.kt                      # App entry point
├── service/
│   ├── CallScreeningService.kt          # Core: Screens incoming calls
│   └── SmsService.kt                    # Foreground service for SMS sending
├── data/
│   ├── model/
│   │   ├── BlockedCall.kt               # Entity: Blocked call records
│   │   └── WhitelistNumber.kt           # Entity: Whitelist numbers
│   └── database/
│       ├── AppDatabase.kt               # Room database singleton
│       ├── BlockedCallDao.kt            # DAO for blocked calls
│       └── WhitelistDao.kt              # DAO for whitelist
├── ui/
│   ├── screens/
│   │   └── HomeScreen.kt                # Main UI with settings
│   └── theme/                           # Material3 theme configuration
└── util/
    ├── ContactsHelper.kt                # Contacts lookup utilities
    └── PreferenceHelper.kt              # SharedPreferences wrapper
```

### Key Dependencies
- Compose BOM: 2024.09.00
- Room: 2.6.1 (runtime, ktx, compiler via KSP)
- Navigation Compose: 2.8.0
- Lifecycle ViewModel Compose: 2.9.3

### Required Permissions
The app requires the following permissions (all declared in AndroidManifest.xml):
- `READ_PHONE_STATE` - Monitor phone state
- `READ_CALL_LOG` - Access call logs
- `READ_CONTACTS` - Check if caller is in contacts
- `SEND_SMS` - Send auto-reply messages
- `FOREGROUND_SERVICE` - Run foreground SMS service
- `FOREGROUND_SERVICE_SHORT_SERVICE` - Foreground service type (Android 14+)
- `POST_NOTIFICATIONS` - Show notifications (Android 13+)

### Services Configuration
Two services are registered in AndroidManifest.xml:
1. **CallScreeningService**: Intercepts incoming calls
   - Permission: `android.permission.BIND_SCREENING_SERVICE`
   - Exported: true
   - Intent filter: `android.telecom.CallScreeningService`

2. **SmsService**: Sends SMS in foreground
   - Foreground service type: `shortService`
   - Exported: false

## How It Works

### Call Screening Flow
1. **Incoming Call** → System routes to `CallScreeningService.onScreenCall()`
2. **Extract Phone Number** → From `Call.Details.handle`
3. **Check Whitelist** → Query Room database via `WhitelistDao`
4. **Check Contacts** → Query `ContactsContract.PhoneLookup`
5. **Decision**:
   - If in whitelist OR contacts → Allow call (normal ringing)
   - If stranger AND blocking enabled → Block call + send SMS
6. **Block Actions**:
   - `setDisallowCall(true)` - Prevents call from showing
   - `setRejectCall(true)` - Rejects the call
   - `setSkipCallLog(true)` - No call log entry (Android 10+)
   - `setSkipNotification(true)` - No missed call notification (Android 10+)
7. **SMS Reply** → Start `SmsService` as foreground service
8. **Database Logging** → Save blocked call record to Room DB

### Important Implementation Details

**CallScreeningService**:
- Uses coroutines (`CoroutineScope`) for async operations
- Must store `Call.Details` as lateinit var for async response
- Catches exceptions to default to "allow call" on errors
- Logs all decisions for debugging

**ContactsHelper**:
- All operations run on `Dispatchers.IO`
- Normalizes phone numbers (removes spaces, hyphens)
- Returns `true` for stranger = should block

**SmsService**:
- Runs as foreground service to bypass Android 10+ background restrictions
- Shows notification while sending SMS
- Auto-stops after sending
- Checks `PreferenceHelper.isAutoSmsEnabled()` before sending

**PreferenceHelper**:
- Singleton object for SharedPreferences access
- Default SMS template: "您好，我现在不方便接听电话，稍后回复您。"
- All settings stored in `fuck_junk_call_prefs`

## Development Notes

### Testing Call Screening
- Use real device (emulator may not support CallScreeningService properly)
- Grant "Call Screening" role via Settings or RoleManager
- Test with both known and unknown numbers
- Check logcat for "CallScreeningService" tag

### Database Access
- All database operations are suspend functions
- Use `AppDatabase.getDatabase(context)` to get singleton instance
- DAO methods return `Flow<T>` for reactive updates in UI

### Adding New Features
- **New settings**: Add to `PreferenceHelper` with getter/setter
- **New database table**: Create entity + DAO, bump database version
- **New permissions**: Add to AndroidManifest + update permission check in HomeScreen

### Common Issues
- **Calls not being blocked**: Check if Call Screening role is granted
- **SMS not sending**: Verify SEND_SMS permission + auto-SMS enabled
- **Crashes on database access**: Ensure not accessing DB on main thread