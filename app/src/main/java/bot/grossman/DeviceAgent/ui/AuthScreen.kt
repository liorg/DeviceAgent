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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import bot.grossman.DeviceAgent.data.ApiClient
import bot.grossman.DeviceAgent.data.AuthRepository
import bot.grossman.DeviceAgent.data.UserCacheManager
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(onLoggedIn: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    
    // מצב שמחזיק האם להציג את בקשת מספר הטלפון לאחר התחברות מוצלחת
    var showPhonePrompt by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        if (showPhonePrompt) {
            // מסך / דיאלוג בקשת טלפון (לאחר התחברות Supabase)
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("הגדרת סוכן", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                Text("התחברת בהצלחה! כעת הזן את מספר הנייד של המכשיר לטובת זיהוי הבוט:", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = phoneNumber, onValueChange = { phoneNumber = it },
                    label = { Text("מספר טלפון (לדוגמה: 0501234567)") }, 
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(24.dp))
                
                Button(
                    enabled = !loading && phoneNumber.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        scope.launch {
                            loading = true; error = null
                            try {
                                // 1. קריאה ל-API שלך כדי לחבר את הסוכן
                                val apiRes = ApiClient.connectAgent(phoneNumber)
                                // 2. שמירת התשובה ב-Cache קבוע
                                UserCacheManager.saveUserDetails(
                                    ctx, 
                                    phone = phoneNumber, 
                                    username = apiRes.username, 
                                    lang = apiRes.prefLang
                                )
                                // 3. מעבר למסך סטטוס
                                onLoggedIn()
                            } catch (e: Exception) {
                                error = "שגיאה בתקשורת מול ה-API: ${e.message}"
                            }
                            loading = false
                        }
                    }
                ) { Text(if (loading) "מחבר לשרת..." else "המשך וחיבור הבוט") }
                
                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        } else {
            // מסך התחברות רגיל (Supabase)
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("WhatsApp Guard", style = MaterialTheme.typography.headlineMedium)
                Text("בחר שיטת התחברות", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(32.dp))

                // אפשרות 1: התחברות עם חשבון גוגל (Gmail)
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    onClick = {
                        scope.launch {
                            loading = true; error = null
                            try { 
                                AuthRepository.signInWithGoogle()
                                showPhonePrompt = true
                            } catch (e: Exception) { error = e.message }
                            loading = false
                        }
                    }
                ) { Text("התחברות עם Gmail (Google)") }

                Spacer(Modifier.height(24.dp))
                Text("או עם אימייל וסיסמה:", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))

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
                            try { 
                                AuthRepository.signIn(email.trim(), password)
                                showPhonePrompt = true 
                            }
                            catch (e: Exception) { error = e.message }
                            loading = false
                        }
                    }
                ) { Text(if (loading) "מתחבר…" else "התחבר עם אימייל") }

                Spacer(Modifier.height(8.dp))
                TextButton(onClick = {
                    scope.launch {
                        loading = true; error = null
                        try { 
                            AuthRepository.signUp(email.trim(), password)
                            showPhonePrompt = true 
                        }
                        catch (e: Exception) { error = e.message }
                        loading = false
                    }
                }) { Text("אין חשבון? הירשם עם אימייל זה") }

                error?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
