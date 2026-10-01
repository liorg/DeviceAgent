package bot.grossman.DeviceAgent.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import bot.grossman.DeviceAgent.automation.PairingAccessibilityService
import bot.grossman.DeviceAgent.monitor.WhatsAppMonitor

/** מסך 3 — אימות מחדש: הזנת pair code + העתקה + PoC אוטומציה */
@Composable
fun PairScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    var pairCode by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(Modifier.fillMaxSize().padding(24.dp)) {
            Text("אימות מחדש — צימוד מכשיר", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(16.dp))

            Text(
                "הדבק/הכנס את קוד הצימוד (pair code) שקיבלת מהממשק:",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = pairCode,
                onValueChange = { pairCode = it },
                label = { Text("Pair Code") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))

            // העתקה ל-clipboard
            Button(
                enabled = pairCode.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("paircode", pairCode.trim()))
                    message = "הקוד הועתק ל-clipboard ✓"
                }
            ) { Text("העתק קוד ל-Clipboard") }

            Spacer(Modifier.height(16.dp))

            // PoC אוטומציה
            val a11yOn = remember { mutableStateOf(PairingAccessibilityService.isEnabled(ctx)) }
            LaunchedEffect(Unit) {
                while (true) {
                    a11yOn.value = PairingAccessibilityService.isEnabled(ctx)
                    kotlinx.coroutines.delay(1500)
                }
            }

            Button(
                enabled = pairCode.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary),
                onClick = {
                    if (!a11yOn.value) {
                        message = "יש להפעיל קודם את שירות הנגישות"
                        ctx.startActivity(PairingAccessibilityService.accessibilitySettingsIntent())
                    } else {
                        // מסמן את הקוד כממתין ומעביר ל-WhatsApp; השירות ינסה להשלים את הזרימה
                        PairingAccessibilityService.pendingPairCode = pairCode.trim()
                        val launch = ctx.packageManager
                            .getLaunchIntentForPackage(WhatsAppMonitor.WA_PACKAGE)
                            ?: ctx.packageManager.getLaunchIntentForPackage(WhatsAppMonitor.WA_BUSINESS)
                        if (launch != null) {
                            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ctx.startActivity(launch)
                            message = "האוטומציה פעילה — עובר ל-WhatsApp…"
                        } else {
                            message = "WhatsApp לא מותקן במכשיר"
                        }
                    }
                }
            ) { Text("נסה אוטומציה (PoC)") }

            if (!a11yOn.value) {
                Spacer(Modifier.height(8.dp))
                Text("שירות הנגישות כבוי — לחיצה על הכפתור תפתח את ההגדרות",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall)
            }

            message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onBack) { Text("חזרה לסטטוס") }
        }
    }
}
