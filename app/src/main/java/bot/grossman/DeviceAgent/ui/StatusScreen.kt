package bot.grossman.DeviceAgent.ui

import androidx.compose.animation.AnimatedVisibility
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
import bot.grossman.DeviceAgent.data.AuthRepository
import bot.grossman.DeviceAgent.data.UserCacheManager
import bot.grossman.DeviceAgent.monitor.WhatsAppMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatusScreen(onOpenPairing: () -> Unit, onSignedOut: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var lastAlive by remember { mutableStateOf<Long?>(null) }
    var waInstalled by remember { mutableStateOf(false) }
    var waVersion by remember { mutableStateOf<String?>(null) }
    var usageAccess by remember { mutableStateOf(false) }
    var lastSyncOk by remember { mutableStateOf<Boolean?>(null) }
    
    // Cache UI State
    var showUserDetails by remember { mutableStateOf(false) }

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
            
            // אינדיקטור חיבור ופרטי סוכן מתוך ה-Cache
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                onClick = { showUserDetails = !showUserDetails }
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🟢 Connected", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.weight(1f))
                        Text(if (showUserDetails) "הסתר פרטים" else "הצג פרטים", style = MaterialTheme.typography.bodySmall)
                    }
                    
                    AnimatedVisibility(visible = showUserDetails) {
                        Column(Modifier.padding(top = 12.dp)) {
                            Text("Username: ${UserCacheManager.getUsername(ctx)}", style = MaterialTheme.typography.bodyMedium)
                            Text("Pref Lang: ${UserCacheManager.getPrefLang(ctx)}", style = MaterialTheme.typography.bodyMedium)
                            Text("Phone Connect: ${UserCacheManager.getPhone(ctx)}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

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

            Spacer(Modifier.height(24.dp))

            Button(
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                onClick = onOpenPairing
            ) { Text("בצע אימות מחדש (צימוד)") }

            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { 
                    scope.launch { 
                        AuthRepository.signOut()
                        UserCacheManager.clear(ctx)
                        onSignedOut() 
                    } 
                }
            ) { Text("התנתק") }
        }
    }
}
