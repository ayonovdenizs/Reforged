package com.reforged.client.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import android.app.Activity
import android.content.Intent

@Composable
fun LoginScreen(viewModel: AuthViewModel) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("LoginScreen", "Launcher result code: ${result.resultCode}")
        if (result.resultCode == Activity.RESULT_OK) {
            val token = result.data?.getStringExtra("access_token")
            val userId = result.data?.getStringExtra("user_id")?.toLongOrNull() ?: 0L
            val successToken = result.data?.getStringExtra("success_token")
            
            android.util.Log.d("LoginScreen", "Launcher Data: token=${token != null}, successToken=${successToken != null}")
            
            if (token != null) {
                viewModel.onTokenCaptured(token, userId)
            } else if (successToken != null) {
                viewModel.onCaptchaSuccess(successToken)
            }
        }
    }
    
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var captchaKey by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Telegram-style Logo/Icon
            Surface(
                modifier = Modifier.size(100.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(28.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("VK", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "VK Reforged",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Enter your VK credentials to continue.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            when (val state = authState) {
                is AuthState.Idle, is AuthState.Error -> {
                    if (state is AuthState.Error) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Text(
                                text = state.message,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Phone or Email") },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { viewModel.startLogin(username) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("NEXT", style = MaterialTheme.typography.labelLarge)
                    }
                }
                
                is AuthState.Loading -> {
                    CircularProgressIndicator()
                }

                is AuthState.SelectValidationMethod -> {
                    Text(text = "Choose Verification Method", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    state.methods.forEach { method ->
                        val methodText = when (method.name) {
                            "push" -> "Push Notification"
                            "sms" -> "SMS"
                            "email" -> "Email"
                            "callreset" -> "Call with Code"
                            "codegen" -> "Code Generator"
                            "password" -> "Password"
                            "reserve_code" -> "Reserve Code"
                            else -> method.name.uppercase()
                        }
                        val infoText = if (!method.info.isNullOrEmpty()) " (${method.info})" else ""
                        
                        OutlinedButton(
                            onClick = { viewModel.selectMethod(state.sid, method.name) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = methodText + infoText)
                        }
                    }
                }

                is AuthState.CodeValidation -> {
                    Text(text = "Enter Code", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = state.info ?: "Check your ${state.method}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { if (it.length <= 6) code = it },
                        label = { Text("Verification Code") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { viewModel.verifyCode(code) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("CONTINUE")
                    }
                    TextButton(onClick = { viewModel.logout() }) { Text("CANCEL") }
                }

                is AuthState.NeedPassword -> {
                    Text(text = "Password Required", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Rounded.Lock, contentDescription = null) },
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.loginWithPassword(password) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("LOGIN", style = MaterialTheme.typography.labelLarge)
                    }
                }

                is AuthState.NeedCaptcha -> {
                    Text(text = "Captcha Required", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (state.redirectUri != null) {
                        Button(
                            onClick = {
                                val intent = Intent(context, AuthWebViewActivity::class.java).apply {
                                    putExtra("url", state.redirectUri)
                                }
                                launcher.launch(intent)
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("SOLVE IN BROWSER")
                        }
                    } else {
                        Surface(
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)),
                            color = Color.White
                        ) {
                            AsyncImage(
                                model = state.imgUrl,
                                contentDescription = "Captcha",
                                modifier = Modifier.size(130.dp, 50.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = captchaKey,
                            onValueChange = { captchaKey = it },
                            label = { Text("Captcha Text") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.submitCaptcha(captchaKey) },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("SUBMIT")
                        }
                    }
                    TextButton(onClick = { viewModel.logout() }) { Text("CANCEL") }
                }

                is AuthState.Need2FA -> {
                    Text(text = "Two-Step Verification", style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We have sent an authentication code to ${state.phoneMask}.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { if (it.length <= 6) code = it },
                        label = { Text("Code") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = { viewModel.submit2FA(code) },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("VERIFY")
                    }
                }
                
                is AuthState.Success -> {
                    CircularProgressIndicator()
                    Text(text = "Success! Redirecting...", modifier = Modifier.padding(top = 16.dp))
                }
            }
        }
    }
}
