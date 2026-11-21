# Security Summary

**Reviewed Files**: 19
**Issues Resolved**: 7

### Issues Resolved
| Area | File | Line | What Was Happening | Impact | Status | How It Was Fixed |
|---|---|---|---|---|---|---|
| Cleartext Network Traffic | `app/src/main/AndroidManifest.xml` | 30 | Older Android versions (`API 24–27`) allowed unencrypted HTTP traffic by default when `usesCleartextTraffic` was unset. | Medium | Fixed | Explicitly set `android:usesCleartextTraffic="false"` in `AndroidManifest.xml`. |
| Unencrypted Backup & Transfer Extraction | `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/backup_rules.xml`, `app/src/main/res/xml/data_extraction_rules.xml` | 29–32 | `android:allowBackup="true"` with empty exclusion rules permitted ADB/cloud backup extraction of local Room telemetry and preferences. | Medium | Fixed | Set `android:allowBackup="false"` and explicitly excluded `sharedpref` and `database` domains in `backup_rules.xml` and `data_extraction_rules.xml`. |
| Untrusted Update Origin & Redirects | `app/src/main/java/com/example/data/update/InAppUpdateEngine.kt` | 84–100, 916–923 | Update requests and HTTP redirects had no host or HTTPS allowlist guard. | High | Fixed | Enforced `isTrustedUpdateUrl` (`https://`, no userinfo, strict GitHub/CDN host allowlist) via an OkHttp network interceptor and release JSON validation. |
| Downloaded APK Integrity & Path Confinement | `app/src/main/java/com/example/data/update/InAppUpdateEngine.kt` | 593–601, 632–652, 811–854, 925–931 | APK downloads lacked size caps, filename sanitization, canonical directory checks, version-downgrade protection, and signing-certificate verification prior to launching `PackageInstaller`. | High | Fixed | Added `sanitizeApkFileName`, canonical path confinement inside `cacheDir/updates`, `150 MB` transfer limit, `VERSION_CODE` anti-downgrade check, and SHA-256 signing certificate comparison before install. |
| Broadcast Action spoofing / Unchecked Intent | `app/src/main/java/com/example/service/AlarmScheduler.kt` | 88–89 | `AlarmReceiver.onReceive` processed intents without verifying the expected `ACTION_ALARM_HEARTBEAT` action. | Low | Fixed | Added an early action check rejecting intents whose action does not match `AlarmScheduler.ACTION_ALARM_HEARTBEAT`. |
| Debug Log Leakage & Unused HTTP Libraries | `app/src/main/java/com/example/data/telemetry/CurrentCalibrationEngine.kt`, `app/build.gradle.kts` | 56, 104, 189 | Calibration logs wrote unconditionally to Logcat in release builds, and unused Retrofit/Moshi/OkHttp logging dependencies enlarged the APK attack surface. | Low | Fixed | Gated calibration debug logs behind `BuildConfig.DEBUG` and commented out unused template dependencies including `logging-interceptor`. |
| Constant-Time Redeem Validation & Log Hygiene | `app/src/main/java/com/example/ui/AmperePulseViewModel.kt` | 289–314 | Redeem code verification used direct equality and echoed the literal secret string into session logs. | Low | Fixed | Switched comparison to constant-time `MessageDigest.isEqual` (UTF-8) and removed the raw code from the generated session log entry. |

### Safeguards Verified
| Area | File | How It's Protected |
|---|---|---|
| Exported Components & FileProvider | `app/src/main/AndroidManifest.xml` | `TelemetryForegroundService`, `AlarmReceiver`, and `FileProvider` are marked `android:exported="false"`, sharing only private cache paths under scoped temporary read grants. |
| Update Host Allowlist | `app/src/main/java/com/example/data/update/InAppUpdateEngine.kt` | Rejects `http://`, local `file://`, credentialed URLs (`@`), and any host outside GitHub Releases API/CDN domains. |
| Least-Privilege Hardware Sensors | `app/src/main/AndroidManifest.xml` | Requests only hardware battery telemetry, network state, notifications, wake lock, and user-approved usage stats without storage, camera, or location permissions. |

## PoC Verification

### Unverified In-App APK Replacement & Redirect Bypass

#### Summary
| Field | Value |
|---|---|
| Issue Type | Untrusted Update Origin & Unverified Package Install |
| Impact | High |
| Affected File | `app/src/main/java/com/example/data/update/InAppUpdateEngine.kt:84-100` |
| Test Scenario | Supplying `http://github.com/...`, `https://github.com@evil.example.com/VoltCore.apk`, or a crafted filename `../../etc/evil payload?.apk` |

#### How It Was Fixed
Added strict HTTPS host allowlisting (`isTrustedUpdateUrl`) checked both before request dispatch and on every redirect response, sanitized filenames (`sanitizeApkFileName`) with canonical path confinement inside `cacheDir/updates`, enforced a `150 MB` download limit, and verified matching `packageName`, non-downgrading `versionCode`, and SHA-256 signing certificate digests before invoking the system package installer.

#### Step-by-Step Trace
| Step | What Happens in the Code | Outcome |
|---|---|---|
| 1 | Untrusted URL `https://github.com@evil.example.com/VoltCore.apk` or redirect header arrives | Enters `isTrustedUpdateUrl` (`InAppUpdateEngine.kt:916`) |
| 2 | `parsed.userInfo` check (`line 920`) and host check (`line 922`) reject the URL; OkHttp network interceptor (`line 87-97`) throws `SecurityException` | Blocked before network socket read or file write |
| 3 | Crafted filename `../../etc/evil payload?.apk` enters `sanitizeApkFileName` (`line 925`) and canonical prefix check (`line 597`) | Path separators and traversal tokens stripped; confined to `cacheDir/updates` |
| 4 | Any downloaded APK reaches `verifyDownloadedApkIntegrity` (`line 811`) before installer launch (`line 772`) | Mismatched signature or lower version code triggers deletion and aborts install |

#### Conclusion
**Fix Verified** - Untrusted hosts, cleartext downgrades, directory traversal filenames, and tampered or differently signed APKs are blocked before reaching `FileProvider` or `PackageInstaller`.
