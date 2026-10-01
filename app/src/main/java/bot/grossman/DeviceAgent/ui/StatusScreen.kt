package bot.grossman.DeviceAgent.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import bot.grossman.DeviceAgent.data.AuthRepository
import bot.grossman.DeviceAgent.monitor.WhatsAppMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** מסך 2 — סטטוס: מתי WhatsApp הייתה חיה לאחרונה + פרטי מכשיר וגרסאות */
@Composable
fun StatusScreen(onOpenPairing: () -> Unit, onSignedOut: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var lastAlive by remember { mutableStateOf<Long?>(null) }
    var waInstalled by remember { mutableStateOf(false) }
    var waVersion by remember { mutableStateOf<String?>(null) }
    var usageAccess by remember { mutableStateOf(false) }
    var lastSyncOk by remember { mutableStateOf<Boolean?>(null) }

    // רענון כל 10 שניות
    LaunchedEffect(Unit) {
        while (true) {
            lastAlive = WhatsAppMonitor.lastAliveAt(ctx)
            waInstalled = WhatsAppMonitor.isInstalled(ctx)
            waVersion = WhatsAppMonitor.installedVersion(ctx)
            usageAccess = WhatsAppMonitor.hasUsageAccess(ctx)
            delay(10_000)
        }
    }

    val fmt = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp)
        ) {
            Text("סטטוס WhatsApp", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(20.dp))

            // כרטיס Alive
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (lastAlive == null) "אין נתון על פעילות WhatsApp"
                        else "פעילות אחרונה: ${fmt.format(Date(lastAlive!!))}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "WhatsApp מותקן: " + if (waInstalled) "כן" else "לא",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (!usageAccess) {
                        Spacer(Modifier.height(8.dp))
                        Text("להשלמת הנתון נדרשת הרשאת גישה לנתוני שימוש:",
                            color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = {
                            ctx.startActivity(WhatsAppMonitor.usageAccessSettingsIntent())
                        }) { Text("פתח הגדרות הרשאה") }
                        Text("ולסימון 'חי' מהתראות — הפעל גישה להתראות:",
                            color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = {
                            ctx.startActivity(WhatsAppMonitor.notificationListenerSettingsIntent())
                        }) { Text("פתח גישה להתראות") }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // כרטיס פרטי מכשיר וגרסאות
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val info = WhatsAppMonitor.DeviceInfo()
                    Text("מכשיר: ${info.model}")
                    Text("מערכת: ${info.androidVersion}")
                    Text("גרסת אפליקציה: ${WhatsAppMonitor.appVersion(ctx)}")
                    Text("גרסת WhatsApp: ${waVersion ?: "—"}")
                    Text("משתמש: ${AuthRepository.currentUserId() ?: "—"}")
                }
            }

            Spacer(Modifier.height(16.dp))

            // שליחה ידנית לשרת
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        val uid = AuthRepository.currentUserId() ?: return@launch
                        try {
                            AuthRepository.uploadStatus(
                                bot.grossman.DeviceAgent.data.DeviceStatusRow(
                                    user_id = uid,
                                    device_model = WhatsAppMonitor.DeviceInfo().model,
                                    android_version = WhatsAppMonitor.DeviceInfo().androidVersion,
                                    app_version = WhatsAppMonitor.appVersion(ctx),
                                    whatsapp_version = waVersion,
                                    whatsapp_last_alive = lastAlive
                                )
                            )
                            lastSyncOk = true
                        } catch (e: Exception) { lastSyncOk = false }
                    }
                }
            ) { Text("שלח סטטוס לשרת עכשיו") }
            lastSyncOk?.let {
                Text(if (it) "נשלח בהצלחה ✓" else "שליחה נכשלה",
                    color = if (it) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            }
            Text("(סנכרון אוטומטי לשרת מתבצע כל 15 דקות ברקע)",
                style = MaterialTheme.typography.bodySmall)

            Spacer(Modifier.height(24.dp))

            // מעבר למסך אימות מחדש
            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary),
                onClick = onOpenPairing
            ) { Text("בצע אימות מחדש (צימוד)") }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { scope.launch { AuthRepository.signOut(); onSignedOut() } }
            ) { Text("התנתק") }
        }
    }
}
