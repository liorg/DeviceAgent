package bot.grossman.DeviceAgent.monitor

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import java.util.concurrent.TimeUnit

/**
 * זיהוי "WhatsApp חי":
 * 1) חותמת ההתראה האחרונה שהתקבלה מ-WhatsApp (NotificationListener) — העדיפה.
 * 2) גיבוי: UsageStats — מתי האפליקציה הייתה בשימוש לאחרונה (דורש הרשאת Usage Access).
 */
object WhatsAppMonitor {

    const val WA_PACKAGE = "com.whatsapp"
    const val WA_BUSINESS = "com.whatsapp.w4b"

    private const val PREFS = "wa_monitor_prefs"
    private const val KEY_LAST_NOTIFICATION = "last_wa_notification"

    /** נקרא מתוך ה-NotificationListenerService בכל התראת WhatsApp */
    fun markAlive(context: Context, at: Long = System.currentTimeMillis()) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_NOTIFICATION, at).apply()
    }

    fun lastNotificationAt(context: Context): Long? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_LAST_NOTIFICATION, -1L).takeIf { it > 0 }

    fun lastUsedAt(context: Context, pkg: String = WA_PACKAGE): Long? {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val stats = usm.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            end - TimeUnit.DAYS.toMillis(30), end
        ) ?: return null
        return stats.filter { it.packageName == pkg }
            .maxOfOrNull { it.lastTimeUsed }
            ?.takeIf { it > 0 }
    }

    /** מתי WhatsApp הייתה "חיה" לאחרונה — הטוב מבין המקורות */
    fun lastAliveAt(context: Context): Long? =
        listOfNotNull(lastNotificationAt(context), lastUsedAt(context)).maxOrNull()

    fun isInstalled(context: Context): Boolean =
        try {
            context.packageManager.getPackageInfo(WA_PACKAGE, 0); true
        } catch (e: PackageManager.NameNotFoundException) {
            try { context.packageManager.getPackageInfo(WA_BUSINESS, 0); true }
            catch (e2: PackageManager.NameNotFoundException) { false }
        }

    fun installedVersion(context: Context): String? =
        listOf(WA_PACKAGE, WA_BUSINESS).firstNotNullOfOrNull { pkg ->
            try { context.packageManager.getPackageInfo(pkg, 0).versionName }
            catch (e: Exception) { null }
        }

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(), context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun usageAccessSettingsIntent() = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun notificationListenerSettingsIntent() =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)

    data class DeviceInfo(
        val model: String = "${Build.MANUFACTURER} ${Build.MODEL}",
        val androidVersion: String = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
    )

    fun appVersion(context: Context): String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
}
