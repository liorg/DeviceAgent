package bot.grossman.DeviceAgent.monitor

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/** מאזין להתראות; כל התראת WhatsApp מסמנת שהחשבון חי */
class WaNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == WhatsAppMonitor.WA_PACKAGE ||
            sbn.packageName == WhatsAppMonitor.WA_BUSINESS
        ) {
            WhatsAppMonitor.markAlive(this, sbn.postTime)
        }
    }
}
