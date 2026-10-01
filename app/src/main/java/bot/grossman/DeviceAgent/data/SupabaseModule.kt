package bot.grossman.DeviceAgent.data

import bot.grossman.DeviceAgent.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.Google
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.Serializable

object SupabaseModule {
    val client: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth)
        install(Postgrest)
    }
}

@Serializable
data class DeviceStatusRow(
    val user_id: String,
    val device_model: String,
    val android_version: String,
    val app_version: String,
    val whatsapp_version: String?,
    val whatsapp_last_alive: Long?,
    val updated_at: Long = System.currentTimeMillis()
)

object AuthRepository {

    suspend fun signIn(email: String, password: String) {
        SupabaseModule.client.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signUp(email: String, password: String) {
        SupabaseModule.client.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }
    
    // התחברות גוגל - Supabase מנהל את פתיחת הדפדפן להזדהות OAUTH
    suspend fun signInWithGoogle() {
        // הערה: נדרש להגדיר OAuth ב-Supabase
        SupabaseModule.client.auth.signInWith(Google)
    }

    suspend fun signOut() = SupabaseModule.client.auth.signOut()

    fun currentUserId(): String? = SupabaseModule.client.auth.currentUserOrNull()?.id

    suspend fun uploadStatus(row: DeviceStatusRow) {
        SupabaseModule.client.postgrest["device_status"].upsert(row)
    }
}
