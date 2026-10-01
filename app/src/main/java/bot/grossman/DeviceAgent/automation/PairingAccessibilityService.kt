package bot.grossman.DeviceAgent.automation

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * PoC אוטומציית צימוד WhatsApp דרך AccessibilityService.
 *
 * זרימה (בקירוב, תלויה בגרסת WhatsApp ובשפה):
 * 1. פתיחת WhatsApp -> מסך "מכשירים מקושרים" (Linked devices)
 * 2. לחיצה על "קשר מכשיר" (Link a device)
 * 3. בחירה ב"קשר עם מספר טלפון במקום" (Link with phone number instead)
 * 4. הזנת ה-pair code שהועתק ל-clipboard
 *
 * המערכת מחפשת טקסטים בעברית ובאנגלית. זה PoC — ה-UI של WhatsApp משתנה
 * בין גרסאות וייתכן שיהיה צורך לעדכן את מחרוזות החיפוש.
 */
class PairingAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(Dispatchers.Main)

    companion object {
        var instance: PairingAccessibilityService? = null
            private set

        var pendingPairCode: String? = null

        fun isEnabled(context: Context): Boolean {
            val enabled = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return enabled.contains(context.packageName, ignoreCase = true)
        }

        fun accessibilitySettingsIntent() = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    }

    override fun onServiceConnected() {
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }

    override fun onInterrupt() {}

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val code = pendingPairCode ?: return
        val root = rootInActiveWindow ?: return

        scope.launch {
            // שלב 1: "מכשירים מקושרים" / "Linked devices"
            if (clickNodeByTexts(root, listOf("מכשירים מקושרים", "Linked devices"))) return@launch
            // שלב 2: "קשר מכשיר" / "Link a device"
            if (clickNodeByTexts(root, listOf("קשר מכשיר", "קשירת מכשיר", "Link a device"))) return@launch
            // שלב 3: "קשר עם מספר טלפון במקום" / "Link with phone number instead"
            if (clickNodeByTexts(root, listOf("קשר עם מספר טלפון במקום", "Link with phone number instead"))) {
                delay(1500)
                pasteCodeIntoField(code)
                return@launch
            }
        }
    }

    private fun clickNodeByTexts(root: AccessibilityNodeInfo, texts: List<String>): Boolean {
        for (text in texts) {
            val nodes = root.findAccessibilityNodeInfosByText(text) ?: continue
            for (node in nodes) {
                var target: AccessibilityNodeInfo? = node
                while (target != null && !target.isClickable) target = target.parent
                if (target?.performAction(AccessibilityNodeInfo.ACTION_CLICK) == true) return true
            }
        }
        return false
    }

    private fun pasteCodeIntoField(code: String) {
        val root = rootInActiveWindow ?: return
        val edit = findFocusableEditText(root) ?: return
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("paircode", code))
        edit.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
        edit.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }

    private fun findFocusableEditText(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (root.className?.contains("EditText") == true) return root
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val found = findFocusableEditText(child)
            if (found != null) return found
        }
        return null
    }
}
