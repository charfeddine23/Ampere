package com.example.data.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.FileProvider
import com.example.BuildConfig
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

data class InAppUpdateUiState(
    val isChecking: Boolean = false,
    val isDialogVisible: Boolean = false,
    val updateAvailable: Boolean = false,
    val isDownloading: Boolean = false,
    val isDownloadedReadyToInstall: Boolean = false,
    val needsInstallPermission: Boolean = false,
    val downloadProgress: Float = 0f,
    val downloadedMb: Float = 0f,
    val totalMb: Float = 0f,
    val currentVersionName: String = BuildConfig.VERSION_NAME,
    val currentVersionCode: Int = BuildConfig.VERSION_CODE,
    val availableVersionName: String? = null,
    val availableVersionCode: Int? = null,
    val downloadUrl: String = InAppUpdateEngine.GITHUB_LATEST_RELEASE_URL,
    val apkFileName: String? = null,
    val apkSizeMb: Float? = null,
    val downloadedApkPath: String? = null,
    val statusBadgeText: String = "تحديث جديد",
    val headlineText: String = "تحديث جديد متاح",
    val descriptionText: String = "يتوفر تحديث جديد للتطبيق جاهز للتنزيل والتثبيت المباشر من الداخل.",
    val lastCheckSummary: String = "الإصدار الحالي v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})",
    val hasPlayUpdatePayload: Boolean = false
)

/**
 * Fully In-App Update Engine wired to GitHub Releases
 * (https://github.com/charfeddine23/Ampere) with native OkHttp streaming download,
 * FileProvider local PackageInstaller launch (no external browser), and Play Core support.
 */
class InAppUpdateEngine(private val context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var activeDownloadJob: Job? = null

    private val httpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private val appUpdateManager: AppUpdateManager by lazy {
        AppUpdateManagerFactory.create(appContext)
    }

    private var cachedAppUpdateInfo: AppUpdateInfo? = null
    private var activityResultLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    @Volatile
    private var isArabicLanguage: Boolean =
        com.example.data.local.SettingsPreferencesManager(appContext).loadThemeConfig().appLanguage.isArabic

    private fun buildDefaultUiState(isAr: Boolean): InAppUpdateUiState = InAppUpdateUiState(
        statusBadgeText = if (isAr) "تحديث جديد" else "Update",
        headlineText = if (isAr) "تحديث جديد متاح" else "Update Available",
        descriptionText = if (isAr) {
            "يتوفر تحديث جديد للتطبيق جاهز للتنزيل والتثبيت المباشر من الداخل."
        } else {
            "A new version of Volt Core is ready for direct in-app download and installation."
        },
        lastCheckSummary = if (isAr) {
            "الإصدار الحالي v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
        } else {
            "Current version v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
        }
    )

    private val _uiState = MutableStateFlow(buildDefaultUiState(isArabicLanguage))
    val uiState: StateFlow<InAppUpdateUiState> = _uiState.asStateFlow()

    fun setLanguage(language: com.example.data.AppLanguage) {
        val isAr = language.isArabic
        if (isArabicLanguage == isAr) return
        isArabicLanguage = isAr
        val cur = _uiState.value
        val targetLabel = cur.availableVersionName ?: cur.currentVersionName
        val pct = (cur.downloadProgress * 100).toInt()
        _uiState.value = when {
            cur.isDownloading -> cur.copy(
                headlineText = if (isAr) "جاري تحميل التحديث..." else "Downloading Update...",
                statusBadgeText = if (isAr) "جاري التنزيل • $pct%" else "Downloading • $pct%",
                descriptionText = if (isAr) "يتم تحميل التحديث داخل التطبيق مباشرة بدون تطبيقات خارجية..." else "Downloading the update directly inside the app...",
                lastCheckSummary = if (isAr) "جاري التنزيل داخل التطبيق ($pct%)" else "Downloading in-app ($pct%)"
            )
            cur.isDownloadedReadyToInstall -> cur.copy(
                headlineText = if (isAr) "اكتمل تنزيل التحديث" else "Update Downloaded",
                statusBadgeText = if (cur.needsInstallPermission) {
                    if (isAr) "يتطلب إذن التثبيت" else "Permission Required"
                } else {
                    if (isAr) "جاهز للتثبيت" else "Ready to Install"
                },
                descriptionText = if (cur.needsInstallPermission) {
                    if (isAr) "تم التحميل بنجاح. فعّل خيار السماح بالتثبيت من هذا المصدر لإكمال التحديث." else "Download complete. Allow installing packages from this source to finish updating."
                } else {
                    if (isAr) "تم التحميل داخل التطبيق بنجاح وجاهز للتثبيت المباشر فوق النسخة الحالية." else "Downloaded in-app and ready to install over the current build."
                },
                lastCheckSummary = if (isAr) "اكتمل التنزيل • جاهز للتثبيت" else "Downloaded • Ready to install"
            )
            cur.isChecking -> cur.copy(
                lastCheckSummary = if (isAr) "جاري البحث عن تحديثات..." else "Checking for updates..."
            )
            cur.updateAvailable -> cur.copy(
                statusBadgeText = if (isAr) "تحديث جديد • v$targetLabel" else "New Update • v$targetLabel",
                headlineText = if (isAr) "تحديث جديد متاح" else "Update Available",
                descriptionText = if (isAr) "الإصدار v$targetLabel جاهز للتنزيل والتثبيت مباشرة داخل التطبيق." else "Version v$targetLabel is ready for direct in-app download and installation.",
                lastCheckSummary = if (isAr) "يتوفر تحديث: v$targetLabel" else "Update available: v$targetLabel"
            )
            else -> cur.copy(
                statusBadgeText = if (isAr) "محدّث • v${BuildConfig.VERSION_NAME}" else "Up to date • v${BuildConfig.VERSION_NAME}",
                headlineText = if (isAr) "أنت على أحدث إصدار" else "You're Up to Date",
                descriptionText = if (isAr) "الإصدار الحالي v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})." else "Current version v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}).",
                lastCheckSummary = if (isAr) "الإصدار الحالي v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})" else "Current version v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})"
            )
        }
    }

    private val installStateListener = InstallStateUpdatedListener { state ->
        val isAr = isArabicLanguage
        when (state.installStatus()) {
            InstallStatus.DOWNLOADING -> {
                val downloaded = state.bytesDownloaded().coerceAtLeast(0L)
                val total = state.totalBytesToDownload().coerceAtLeast(1L)
                val fraction = (downloaded.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                val pct = (fraction * 100).toInt()
                _uiState.value = _uiState.value.copy(
                    isDownloading = true,
                    isDownloadedReadyToInstall = false,
                    downloadProgress = fraction,
                    downloadedMb = downloaded / (1024f * 1024f),
                    totalMb = total / (1024f * 1024f),
                    statusBadgeText = if (isAr) "جاري التنزيل • $pct%" else "Downloading • $pct%",
                    descriptionText = if (isAr) "يتم تنزيل التحديث الآن داخل التطبيق..." else "Downloading update inside the app...",
                    lastCheckSummary = if (isAr) "جاري التنزيل داخلياً ($pct%)" else "Downloading in-app ($pct%)"
                )
            }
            InstallStatus.DOWNLOADED -> {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    isDownloadedReadyToInstall = true,
                    isDialogVisible = true,
                    downloadProgress = 1f,
                    statusBadgeText = if (isAr) "جاهز للتثبيت" else "Ready to Install",
                    headlineText = if (isAr) "اكتمل تنزيل التحديث" else "Update Downloaded",
                    descriptionText = if (isAr) "التحديث جاهز للتثبيت المباشر فوق النسخة الحالية بدون حذف التطبيق." else "The update is ready to install directly over your current version.",
                    lastCheckSummary = if (isAr) "اكتمل التنزيل • جاهز للتثبيت المباشر" else "Download complete • Ready to install"
                )
            }
            InstallStatus.INSTALLED -> {
                unregisterListener()
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    isDownloadedReadyToInstall = false,
                    isDialogVisible = false,
                    updateAvailable = false
                )
            }
            InstallStatus.FAILED,
            InstallStatus.CANCELED -> {
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadProgress = 0f,
                    statusBadgeText = if (isAr) "تحديث جديد" else "Update",
                    descriptionText = if (isAr) "تعذر إكمال التنزيل. يمكنك المحاولة مرة أخرى." else "Could not complete download. Please try again."
                )
            }
            else -> Unit
        }
    }

    init {
        cleanupOldCachedApks()
    }

    fun attachLauncher(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        activityResultLauncher = launcher
    }

    fun registerListener() {
        try {
            appUpdateManager.registerListener(installStateListener)
        } catch (_: Exception) {}
    }

    fun unregisterListener() {
        try {
            appUpdateManager.unregisterListener(installStateListener)
        } catch (_: Exception) {}
    }

    /**
     * Checks GitHub Latest Release API for newer signed APK builds.
     * When [autoDownloadIfFound] is true and a newer direct .apk asset exists,
     * begins downloading inside the app automatically with no external apps.
     */
    fun checkForUpdates(
        autoShowOnAvailable: Boolean = true,
        openDialogImmediatelyOnManual: Boolean = false,
        autoDownloadIfFound: Boolean = false
    ) {
        if (_uiState.value.isChecking || _uiState.value.isDownloading) return

        val isAr = isArabicLanguage
        val existingApkPath = _uiState.value.downloadedApkPath
        if (_uiState.value.isDownloadedReadyToInstall && existingApkPath != null && File(existingApkPath).exists()) {
            val canInstall = canInstallPackages()
            _uiState.value = _uiState.value.copy(
                needsInstallPermission = !canInstall,
                lastCheckSummary = if (canInstall) {
                    if (isAr) "جاهز للتثبيت المباشر" else "Ready to install"
                } else {
                    if (isAr) "فعّل إذن التثبيت لإكمال التحديث" else "Enable install permission to continue"
                }
            )
            return
        }

        _uiState.value = _uiState.value.copy(
            isChecking = true,
            lastCheckSummary = if (isAr) "جاري البحث عن تحديثات..." else "Checking for updates..."
        )

        scope.launch {
            val ar = isArabicLanguage
            val githubResult = fetchLatestGithubRelease()
            if (githubResult != null) {
                val isNewer = compareSemanticVersions(githubResult.versionName, BuildConfig.VERSION_NAME) > 0
                if (isNewer) {
                    val sizeLabel = githubResult.apkSizeMb?.let {
                        String.format(Locale.US, " (%.1f MB)", it)
                    } ?: ""
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        updateAvailable = true,
                        isDialogVisible = autoShowOnAvailable || openDialogImmediatelyOnManual,
                        availableVersionName = githubResult.versionName,
                        downloadUrl = githubResult.downloadUrl,
                        apkFileName = githubResult.apkFileName,
                        apkSizeMb = githubResult.apkSizeMb,
                        hasPlayUpdatePayload = false,
                        statusBadgeText = if (ar) "تحديث جديد • v${githubResult.versionName}" else "New Update • v${githubResult.versionName}",
                        headlineText = if (ar) "تحديث جديد متاح" else "Update Available",
                        descriptionText = if (ar) {
                            "الإصدار v${githubResult.versionName}$sizeLabel متاح للتنزيل والتثبيت المباشر داخل التطبيق."
                        } else {
                            "Version v${githubResult.versionName}$sizeLabel is ready for direct in-app download and installation."
                        },
                        lastCheckSummary = if (ar) "يتوفر تحديث: v${githubResult.versionName}$sizeLabel" else "Update available: v${githubResult.versionName}$sizeLabel"
                    )

                    if (autoDownloadIfFound && githubResult.downloadUrl.contains(".apk", ignoreCase = true)) {
                        startInAppApkDownload(
                            url = githubResult.downloadUrl,
                            fileName = githubResult.apkFileName ?: "Volt.Core.v${githubResult.versionName}.apk",
                            expectedSizeMb = githubResult.apkSizeMb,
                            hostContext = appContext
                        )
                    }
                    return@launch
                } else {
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        updateAvailable = false,
                        availableVersionName = githubResult.versionName,
                        downloadUrl = githubResult.downloadUrl,
                        apkFileName = githubResult.apkFileName,
                        apkSizeMb = githubResult.apkSizeMb,
                        hasPlayUpdatePayload = false,
                        isDialogVisible = openDialogImmediatelyOnManual,
                        statusBadgeText = if (ar) "محدّث • v${BuildConfig.VERSION_NAME}" else "Up to date • v${BuildConfig.VERSION_NAME}",
                        headlineText = if (openDialogImmediatelyOnManual) {
                            if (ar) "أنت على أحدث إصدار" else "You're Up to Date"
                        } else {
                            if (ar) "تحديث جديد متاح" else "Update Available"
                        },
                        descriptionText = if (openDialogImmediatelyOnManual) {
                            if (ar) "أنت تستخدم أحدث إصدار متاح من Volt Core (v${BuildConfig.VERSION_NAME})." else "You are running the latest version of Volt Core (v${BuildConfig.VERSION_NAME})."
                        } else {
                            if (ar) "تحديث جديد متاح" else "Update Available"
                        },
                        lastCheckSummary = if (ar) "أنت على أحدث إصدار (v${githubResult.versionName})" else "Up to date (v${githubResult.versionName})"
                    )
                    return@launch
                }
            }

            // Secondary fallback: Google Play In-App Updates API
            checkPlayInAppUpdateFallback(autoShowOnAvailable, openDialogImmediatelyOnManual)
        }
    }

    private data class GithubReleaseInfo(
        val versionName: String,
        val downloadUrl: String,
        val apkFileName: String?,
        val apkSizeMb: Float?
    )

    private fun fetchLatestGithubRelease(): GithubReleaseInfo? {
        return try {
            val request = Request.Builder()
                .url(GITHUB_LATEST_RELEASE_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "AmperePulse-Android/${BuildConfig.VERSION_NAME}")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                val json = JSONObject(body)

                val rawTag = json.optString("tag_name", "").trim()
                val versionName = rawTag.removePrefix("v").removePrefix("V")
                    .substringBefore("-")
                    .ifBlank { return null }

                val htmlUrl = json.optString("html_url", GITHUB_LATEST_RELEASE_URL)
                var apkDownloadUrl: String? = null
                var apkName: String? = null
                var apkSizeMb: Float? = null

                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val name = asset.optString("name", "")
                        if (name.endsWith(".apk", ignoreCase = true)) {
                            apkName = name
                            apkDownloadUrl = asset.optString("browser_download_url", htmlUrl)
                            val bytes = asset.optLong("size", 0L)
                            if (bytes > 0L) {
                                apkSizeMb = bytes / (1024f * 1024f)
                            }
                            break
                        }
                    }
                }

                GithubReleaseInfo(
                    versionName = versionName,
                    downloadUrl = apkDownloadUrl ?: htmlUrl,
                    apkFileName = apkName,
                    apkSizeMb = apkSizeMb
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun checkPlayInAppUpdateFallback(
        autoShowOnAvailable: Boolean,
        openDialogImmediatelyOnManual: Boolean
    ) {
        try {
            appUpdateManager.appUpdateInfo
                .addOnSuccessListener { info ->
                    cachedAppUpdateInfo = info
                    val availability = info.updateAvailability()
                    val installStatus = info.installStatus()

                    val isAr = isArabicLanguage
                    when {
                        installStatus == InstallStatus.DOWNLOADED -> {
                            _uiState.value = _uiState.value.copy(
                                isChecking = false,
                                updateAvailable = true,
                                isDownloadedReadyToInstall = true,
                                isDialogVisible = true,
                                availableVersionCode = info.availableVersionCode(),
                                hasPlayUpdatePayload = true,
                                statusBadgeText = if (isAr) "جاهز للتثبيت" else "Ready to Install",
                                headlineText = if (isAr) "اكتمل تنزيل التحديث" else "Update Downloaded",
                                descriptionText = if (isAr) "اضغط على تحديث لإكمال التثبيت وإعادة تشغيل التطبيق." else "Tap update to finish installing and restart the app.",
                                lastCheckSummary = if (isAr) "تحديث جاهز للتثبيت (Build ${info.availableVersionCode()})" else "Ready to install (Build ${info.availableVersionCode()})"
                            )
                        }
                        availability == UpdateAvailability.UPDATE_AVAILABLE -> {
                            val verCode = info.availableVersionCode()
                            _uiState.value = _uiState.value.copy(
                                isChecking = false,
                                updateAvailable = true,
                                isDialogVisible = autoShowOnAvailable || openDialogImmediatelyOnManual,
                                availableVersionCode = verCode,
                                hasPlayUpdatePayload = true,
                                statusBadgeText = if (isAr) "تحديث جديد • Build $verCode" else "New Update • Build $verCode",
                                headlineText = if (isAr) "تحديث جديد متاح" else "Update Available",
                                descriptionText = if (isAr) "يتوفر تحديث جديد متاح للتنزيل من داخل التطبيق." else "A new update is ready to download inside the app.",
                                lastCheckSummary = if (isAr) "يتوفر تحديث جديد (Build $verCode)" else "New update available (Build $verCode)"
                            )
                        }
                        else -> {
                            _uiState.value = _uiState.value.copy(
                                isChecking = false,
                                updateAvailable = false,
                                hasPlayUpdatePayload = false,
                                downloadUrl = GITHUB_LATEST_RELEASE_URL,
                                isDialogVisible = openDialogImmediatelyOnManual,
                                statusBadgeText = if (isAr) "محدّث • v${BuildConfig.VERSION_NAME}" else "Up to date • v${BuildConfig.VERSION_NAME}",
                                headlineText = if (openDialogImmediatelyOnManual) {
                                    if (isAr) "أنت على أحدث إصدار" else "You're Up to Date"
                                } else {
                                    if (isAr) "تحديث جديد متاح" else "Update Available"
                                },
                                descriptionText = if (isAr) "الإصدار الحالي v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})." else "Current version v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}).",
                                lastCheckSummary = if (isAr) "الإصدار الحالي v${BuildConfig.VERSION_NAME} • محدّث" else "Current v${BuildConfig.VERSION_NAME} • Up to date"
                            )
                        }
                    }
                }
                .addOnFailureListener {
                    cachedAppUpdateInfo = null
                    val isAr = isArabicLanguage
                    _uiState.value = _uiState.value.copy(
                        isChecking = false,
                        hasPlayUpdatePayload = false,
                        downloadUrl = GITHUB_LATEST_RELEASE_URL,
                        isDialogVisible = openDialogImmediatelyOnManual,
                        statusBadgeText = "v${BuildConfig.VERSION_NAME} • GitHub Releases",
                        headlineText = if (isAr) "التحديث التلقائي" else "Auto Update",
                        descriptionText = if (isAr) "تعذر الاتصال بخادم التحديثات الآن. تأكد من الاتصال بالإنترنت وأعد المحاولة." else "Unable to reach update server right now. Check your connection and try again.",
                        lastCheckSummary = if (isAr) "تعذر الاتصال • الإصدار الحالي v${BuildConfig.VERSION_NAME}" else "Connection failed • Current v${BuildConfig.VERSION_NAME}"
                    )
                }
        } catch (_: Exception) {
            _uiState.value = _uiState.value.copy(
                isChecking = false,
                downloadUrl = GITHUB_LATEST_RELEASE_URL,
                lastCheckSummary = if (isArabicLanguage) "الإصدار الحالي v${BuildConfig.VERSION_NAME}" else "Current version v${BuildConfig.VERSION_NAME}"
            )
        }
    }

    fun openUpdateDialogPreview() {
        val current = _uiState.value
        val isAr = isArabicLanguage
        val targetLabel = current.availableVersionName ?: current.currentVersionName
        _uiState.value = current.copy(
            isDialogVisible = true,
            statusBadgeText = when {
                current.isDownloadedReadyToInstall -> if (isAr) "جاهز للتثبيت" else "Ready to Install"
                current.isDownloading -> if (isAr) "جاري التنزيل • ${(current.downloadProgress * 100).toInt()}%" else "Downloading • ${(current.downloadProgress * 100).toInt()}%"
                current.updateAvailable -> if (isAr) "تحديث جديد • v$targetLabel" else "New Update • v$targetLabel"
                else -> if (isAr) "تحديث جديد • v$targetLabel" else "Update • v$targetLabel"
            },
            headlineText = when {
                current.isDownloadedReadyToInstall -> if (isAr) "اكتمل تنزيل التحديث" else "Update Downloaded"
                current.updateAvailable -> if (isAr) "تحديث جديد متاح" else "Update Available"
                else -> if (isAr) "تحديث جديد متاح" else "Check for Updates"
            },
            descriptionText = when {
                current.isDownloadedReadyToInstall -> if (isAr) "تم التنزيل داخل التطبيق بنجاح وجاهز للتثبيت المباشر." else "Downloaded inside the app and ready to install directly."
                current.updateAvailable -> if (isAr) "الإصدار v$targetLabel جاهز للتنزيل والتثبيت مباشرة داخل التطبيق." else "Version v$targetLabel is ready to download and install inside the app."
                else -> if (isAr) "اضغط تحديث لفحص وتحميل أحدث نسخة مباشرة داخل التطبيق دون مغادرته." else "Tap Update to check and download the latest release directly inside the app."
            }
        )
    }

    fun dismissDialog() {
        _uiState.value = _uiState.value.copy(isDialogVisible = false)
    }

    fun startOrCompleteUpdate(hostContext: Context) {
        val current = _uiState.value
        if (current.isDownloading) return

        // 1. Ready to install locally downloaded APK
        val apkPath = current.downloadedApkPath
        if (current.isDownloadedReadyToInstall && apkPath != null) {
            val apkFile = File(apkPath)
            if (apkFile.exists()) {
                launchLocalApkInstaller(hostContext, apkFile)
                return
            }
        }

        // 2. Ready to complete Play Core flexible update
        if (current.isDownloadedReadyToInstall && current.hasPlayUpdatePayload) {
            try {
                appUpdateManager.completeUpdate()
            } catch (_: Exception) {}
            return
        }

        // 3. Play Store Update flow
        val info = cachedAppUpdateInfo
        if (current.hasPlayUpdatePayload && info != null && info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
            val updateType = when {
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                else -> AppUpdateType.FLEXIBLE
            }
            if (launchPlayUpdateFlow(info, updateType)) return
        }

        // 4. Direct In-App APK download from GitHub Releases (no external apps/browser!)
        if (current.updateAvailable && current.downloadUrl.contains(".apk", ignoreCase = true)) {
            val targetName = current.apkFileName ?: "Ampere.Pulse.v${current.availableVersionName ?: "latest"}.apk"
            startInAppApkDownload(
                url = current.downloadUrl,
                fileName = targetName,
                expectedSizeMb = current.apkSizeMb,
                hostContext = hostContext
            )
            return
        }

        // 5. Otherwise run a live check & auto-download inside the app if found
        checkForUpdates(
            autoShowOnAvailable = true,
            openDialogImmediatelyOnManual = true,
            autoDownloadIfFound = true
        )
    }

    private fun startInAppApkDownload(
        url: String,
        fileName: String,
        expectedSizeMb: Float?,
        hostContext: Context
    ) {
        activeDownloadJob?.cancel()
        activeDownloadJob = scope.launch {
            val updatesDir = File(appContext.cacheDir, "updates").apply { mkdirs() }
            val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
            val targetFile = File(updatesDir, safeFileName)
            val tempFile = File(updatesDir, "$safeFileName.part")

            val isArStart = isArabicLanguage
            _uiState.value = _uiState.value.copy(
                isDownloading = true,
                isDownloadedReadyToInstall = false,
                needsInstallPermission = false,
                downloadProgress = 0f,
                downloadedMb = 0f,
                totalMb = expectedSizeMb ?: 0f,
                headlineText = if (isArStart) "جاري تحميل التحديث..." else "Downloading Update...",
                statusBadgeText = if (isArStart) "تنزيل داخلي • 0%" else "In-App Download • 0%",
                descriptionText = if (isArStart) "يتم تحميل التحديث داخل التطبيق مباشرة بدون تطبيقات خارجية..." else "Downloading update directly inside the app...",
                lastCheckSummary = if (isArStart) "جاري التنزيل داخل التطبيق (0%)" else "Downloading in-app (0%)"
            )

            try {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "AmperePulse-Android/${BuildConfig.VERSION_NAME}")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("HTTP ${response.code}")
                    }
                    val body = response.body ?: throw IllegalStateException("Empty body")
                    val totalBytes = body.contentLength().let { len ->
                        if (len > 0L) len else ((expectedSizeMb ?: 0f) * 1024f * 1024f).toLong()
                    }
                    val totalMbVal = if (totalBytes > 0L) totalBytes / (1024f * 1024f) else (expectedSizeMb ?: 0f)

                    body.byteStream().use { input ->
                        FileOutputStream(tempFile).use { output ->
                            val buffer = ByteArray(16 * 1024)
                            var bytesReadTotal = 0L
                            var lastUiEmitTime = 0L

                            while (isActive) {
                                val read = input.read(buffer)
                                if (read == -1) break
                                output.write(buffer, 0, read)
                                bytesReadTotal += read

                                val now = System.currentTimeMillis()
                                if (now - lastUiEmitTime >= 80L) {
                                    lastUiEmitTime = now
                                    val fraction = if (totalBytes > 0L) {
                                        (bytesReadTotal.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                                    } else 0.5f
                                    val downloadedMbVal = bytesReadTotal / (1024f * 1024f)
                                    val pct = (fraction * 100).toInt()
                                    val arLoop = isArabicLanguage
                                    _uiState.value = _uiState.value.copy(
                                        downloadProgress = fraction,
                                        downloadedMb = downloadedMbVal,
                                        totalMb = totalMbVal,
                                        statusBadgeText = if (arLoop) "تنزيل داخلي • $pct%" else "In-App Download • $pct%",
                                        lastCheckSummary = if (arLoop) {
                                            "جاري التحميل داخلياً: $pct% (${String.format(Locale.US, "%.1f", downloadedMbVal)} MB)"
                                        } else {
                                            "Downloading: $pct% (${String.format(Locale.US, "%.1f", downloadedMbVal)} MB)"
                                        }
                                    )
                                }
                            }
                            output.flush()
                        }
                    }
                }

                if (targetFile.exists()) targetFile.delete()
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                val canInstall = canInstallPackages()
                val arDone = isArabicLanguage
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    isDownloadedReadyToInstall = true,
                    needsInstallPermission = !canInstall,
                    downloadProgress = 1f,
                    downloadedApkPath = targetFile.absolutePath,
                    headlineText = if (arDone) "اكتمل تنزيل التحديث" else "Update Downloaded",
                    statusBadgeText = if (canInstall) {
                        if (arDone) "جاهز للتثبيت المباشر" else "Ready to Install"
                    } else {
                        if (arDone) "يتطلب إذن التثبيت" else "Permission Required"
                    },
                    descriptionText = if (canInstall) {
                        if (arDone) "تم تحميل التحديث داخل التطبيق بنجاح وجاهز للتثبيت مباشرة فوق النسخة الحالية." else "Update downloaded in-app and ready to install directly over the current version."
                    } else {
                        if (arDone) "تم التحميل بنجاح. فعّل خيار السماح بالتثبيت من هذا المصدر لإكمال التحديث." else "Download complete. Allow installing packages from this source to finish."
                    },
                    lastCheckSummary = if (canInstall) {
                        if (arDone) "اكتمل التنزيل • جاهز للتثبيت المباشر" else "Downloaded • Ready to install"
                    } else {
                        if (arDone) "تم التنزيل • فعّل إذن التثبيت" else "Downloaded • Enable install permission"
                    }
                )

                withContext(Dispatchers.Main) {
                    launchLocalApkInstaller(hostContext, targetFile)
                }
            } catch (ce: kotlinx.coroutines.CancellationException) {
                runCatching { tempFile.delete() }
                throw ce
            } catch (_: Exception) {
                runCatching { tempFile.delete() }
                val arErr = isArabicLanguage
                _uiState.value = _uiState.value.copy(
                    isDownloading = false,
                    downloadProgress = 0f,
                    headlineText = if (arErr) "تعذر تحميل التحديث" else "Download Failed",
                    statusBadgeText = if (arErr) "فشل التنزيل" else "Failed",
                    descriptionText = if (arErr) "تعذر تنزيل الحزمة داخل التطبيق الآن. تأكد من استقرار الشبكة وحاول مرة أخرى." else "Could not download the package inside the app. Check your network connection and retry.",
                    lastCheckSummary = if (arErr) "تعذر تنزيل التحديث • أعد المحاولة" else "Download failed • Retry"
                )
            }
        }
    }

    fun cancelInAppDownload() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        val isAr = isArabicLanguage
        _uiState.value = _uiState.value.copy(
            isDownloading = false,
            downloadProgress = 0f,
            headlineText = if (isAr) "تحديث جديد متاح" else "Update Available",
            statusBadgeText = if (isAr) "تحديث جديد" else "Update",
            descriptionText = if (isAr) "تم إيقاف التنزيل مؤقتاً. يمكنك استئنافه في أي وقت." else "Download paused. You can resume at any time.",
            lastCheckSummary = if (isAr) "تم إلغاء التنزيل" else "Download canceled"
        )
    }

    private fun canInstallPackages(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                appContext.packageManager.canRequestPackageInstalls()
            } catch (_: Exception) {
                true
            }
        } else {
            true
        }
    }

    private fun launchLocalApkInstaller(hostContext: Context, apkFile: File) {
        if (!apkFile.exists()) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !canInstallPackages()) {
            _uiState.value = _uiState.value.copy(needsInstallPermission = true)
            try {
                val permIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${appContext.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                hostContext.startActivity(permIntent)
            } catch (_: Exception) {}
            return
        }

        try {
            val contentUri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                apkFile
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            hostContext.startActivity(installIntent)
        } catch (_: Exception) {}
    }

    private fun cleanupOldCachedApks() {
        scope.launch {
            try {
                val updatesDir = File(appContext.cacheDir, "updates")
                updatesDir.listFiles()?.forEach { file ->
                    if (file.isFile && (file.name.endsWith(".part") || System.currentTimeMillis() - file.lastModified() > 24 * 3600_000L)) {
                        file.delete()
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun launchPlayUpdateFlow(info: AppUpdateInfo, updateType: Int): Boolean {
        val launcher = activityResultLauncher ?: return false
        return try {
            if (updateType == AppUpdateType.FLEXIBLE) {
                registerListener()
            }
            appUpdateManager.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(updateType).build()
            )
        } catch (_: Exception) {
            false
        }
    }

    fun onUpdateFlowResult(resultCode: Int) {
        if (resultCode != android.app.Activity.RESULT_OK) {
            _uiState.value = _uiState.value.copy(
                isDownloading = false,
                lastCheckSummary = if (isArabicLanguage) {
                    "تم تأجيل التحديث — يمكنك التحديث في أي وقت من الإعدادات"
                } else {
                    "Update postponed — you can update anytime from Settings"
                }
            )
        }
    }

    companion object {
        const val GITHUB_OWNER = "charfeddine23"
        const val GITHUB_REPO = "Ampere"
        const val GITHUB_LATEST_RELEASE_API =
            "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        const val GITHUB_LATEST_RELEASE_URL =
            "https://github.com/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

        fun compareSemanticVersions(remote: String, current: String): Int {
            val remoteParts = remote.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
            val currentParts = current.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
            val maxLen = maxOf(remoteParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val r = remoteParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (r != c) return r.compareTo(c)
            }
            return 0
        }
    }
}
