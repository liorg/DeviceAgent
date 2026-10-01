package bot.grossman.DeviceAgent.data

import kotlinx.coroutines.delay

data class AgentConnectionResponse(
    val username: String,
    val prefLang: String
)

object ApiClient {
    // API מדומה - כאן תכניסי את הקריאה ל-FastAPI/השרת שלך בהמשך
    suspend fun connectAgent(phone: String): AgentConnectionResponse {
        // מדמה השהיית רשת
        delay(1500)
        
        // מדמה תשובה מהשרת שיוצר משתמש על בסיס הטלפון
        return AgentConnectionResponse(
            username = "Agent_$phone",
            prefLang = "he"
        )
    }
}
