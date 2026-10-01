package bot.grossman.DeviceAgent

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import bot.grossman.DeviceAgent.data.AuthRepository
import bot.grossman.DeviceAgent.data.UserCacheManager
import bot.grossman.DeviceAgent.monitor.StatusSyncWorker
import bot.grossman.DeviceAgent.ui.AuthScreen
import bot.grossman.DeviceAgent.ui.PairScreen
import bot.grossman.DeviceAgent.ui.StatusScreen

object Routes {
    const val AUTH = "auth"
    const val STATUS = "status"
    const val PAIR = "pair"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface { AppNav() }
            }
        }
    }
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    
    // ניווט התחלתי תלוי גם בהתחברות ל-Supabase וגם ב-Cache של פרטי המכשיר והטלפון
    val start = remember {
        if (AuthRepository.currentUserId() != null && UserCacheManager.isConnected(ctx)) {
            StatusSyncWorker.schedule(ctx) 
            Routes.STATUS
        } else Routes.AUTH
    }

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.AUTH) {
            AuthScreen(onLoggedIn = {
                StatusSyncWorker.schedule(ctx)
                nav.navigate(Routes.STATUS) { popUpTo(Routes.AUTH) { inclusive = true } }
            })
        }
        composable(Routes.STATUS) {
            StatusScreen(
                onOpenPairing = { nav.navigate(Routes.PAIR) },
                onSignedOut = {
                    nav.navigate(Routes.AUTH) { popUpTo(0) { inclusive = true } }
                }
            )
        }
        composable(Routes.PAIR) {
            PairScreen(onBack = { nav.popBackStack() })
        }
    }
}
