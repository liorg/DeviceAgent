package bot.grossman.DeviceAgent.monitor

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import bot.grossman.DeviceAgent.data.AuthRepository
import bot.grossman.DeviceAgent.data.DeviceStatusRow
import java.util.concurrent.TimeUnit

/**
 * שולח לשרת (Supabase) את סטטוס ה-alive כל X דקות.
 * מינימום תקופה של WorkManager הוא 15 דקות.
 */
class StatusSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val uid = AuthRepository.currentUserId() ?: return Result.success()
        val ctx = applicationContext
        return try {
            AuthRepository.uploadStatus(
                DeviceStatusRow(
                    user_id = uid,
                    device_model = WhatsAppMonitor.DeviceInfo().model,
                    android_version = WhatsAppMonitor.DeviceInfo().androidVersion,
                    app_version = WhatsAppMonitor.appVersion(ctx),
                    whatsapp_version = WhatsAppMonitor.installedVersion(ctx),
                    whatsapp_last_alive = WhatsAppMonitor.lastAliveAt(ctx)
                )
            )
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "wa_status_sync"

        fun schedule(context: Context, intervalMinutes: Long = 15) {
            val request = PeriodicWorkRequestBuilder<StatusSyncWorker>(
                intervalMinutes, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request
            )
        }
    }
}
