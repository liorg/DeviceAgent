package bot.grossman.DeviceAgent.data

import android.content.Context
import android.content.SharedPreferences

object UserCacheManager {
    private const val PREFS_NAME = "agent_prefs"

    fun saveUserDetails(context: Context, phone: String, username: String, lang: String) {
        prefs(context).edit()
            .putString("phone", phone)
            .putString("username", username)
            .putString("pref_lang", lang)
            .putBoolean("is_connected", true)
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun isConnected(context: Context): Boolean = prefs(context).getBoolean("is_connected", false)
    fun getPhone(context: Context): String = prefs(context).getString("phone", "") ?: ""
    fun getUsername(context: Context): String = prefs(context).getString("username", "") ?: ""
    fun getPrefLang(context: Context): String = prefs(context).getString("pref_lang", "") ?: ""

    private fun prefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
}
