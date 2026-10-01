package bot.grossman.DeviceAgent.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import bot.grossman.DeviceAgent.data.AuthRepository
import kotlinx.coroutines.launch

/** מסך 1 — אימות מול Supabase (אימייל + סיסמה) */
@Composable
fun AuthScreen(onLoggedIn: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("WhatsApp Guard", style = MaterialTheme.typography.headlineMedium)
            Text("התחברות לחשבון", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(24.dp))

            OutlinedTextField(
                value = email, onValueChange = { email = it },
                label = { Text("אימייל") }, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = password, onValueChange = { password = it },
                label = { Text("סיסמה") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            Button(
                enabled = !loading && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    scope.launch {
                        loading = true; error = null
                        try { AuthRepository.signIn(email.trim(), password); onLoggedIn() }
                        catch (e: Exception) { error = e.message }
                        loading = false
                    }
                }
            ) { Text(if (loading) "מתחבר…" else "התחבר") }

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = {
                scope.launch {
                    loading = true; error = null
                    try { AuthRepository.signUp(email.trim(), password); onLoggedIn() }
                    catch (e: Exception) { error = e.message }
                    loading = false
                }
            }) { Text("אין חשבון? הירשם") }

            error?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
