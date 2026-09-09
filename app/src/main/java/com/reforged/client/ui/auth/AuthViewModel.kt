package com.reforged.client.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.remote.*
import com.reforged.client.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val token: String) : AuthState()
    data class SelectValidationMethod(val sid: String, val methods: List<VerificationMethod>) : AuthState()
    data class CodeValidation(val sid: String, val method: String, val info: String? = null) : AuthState()
    data class NeedPassword(val sid: String, val canSkip: Boolean = false) : AuthState()
    data class NeedCaptcha(val sid: String, val imgUrl: String) : AuthState()
    data class Need2FA(val sid: String, val phoneMask: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _isAuthorized = MutableStateFlow(tokenStorage.accessToken != null)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private var currentUsername = ""
    private var currentSid = ""
    private var currentMethod = ""
    private var canSkipPassword = false

    fun startLogin(username: String) {
        currentUsername = username
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.validateAccount(username).onSuccess { validation ->
                currentSid = validation.sid ?: ""
                val nextStep = validation.next_step
                
                when {
                    validation.flow_name == "need_registration" -> {
                        _authState.value = AuthState.Error("Account not registered")
                    }
                    nextStep == null || nextStep.verification_method == "password" -> {
                        currentMethod = "password"
                        _authState.value = AuthState.NeedPassword(currentSid)
                    }
                    else -> {
                        // Get methods
                        repository.getVerificationMethods(currentSid).onSuccess { methods ->
                            if (methods.isEmpty()) {
                                // Fallback to nextStep method
                                currentMethod = nextStep.verification_method ?: ""
                                sendOtp(currentSid, currentMethod)
                            } else if (methods.size == 1 && !nextStep.has_another_verification_methods) {
                                currentMethod = methods[0].name
                                sendOtp(currentSid, currentMethod)
                            } else {
                                _authState.value = AuthState.SelectValidationMethod(currentSid, methods)
                            }
                        }.onFailure {
                            // Fallback
                            currentMethod = nextStep.verification_method ?: ""
                            sendOtp(currentSid, currentMethod)
                        }
                    }
                }
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Validation failed")
            }
        }
    }

    fun selectMethod(sid: String, method: String) {
        currentSid = sid
        currentMethod = method
        sendOtp(sid, method)
    }

    private fun sendOtp(sid: String, method: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.sendEcosystemOtp(sid, method, currentUsername).onSuccess { response ->
                currentSid = response.sid ?: currentSid
                _authState.value = AuthState.CodeValidation(currentSid, method, response.info)
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Failed to send code")
            }
        }
    }

    fun verifyCode(code: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.checkEcosystemOtp(currentSid, currentMethod, code).onSuccess { response ->
                currentSid = response.sid ?: currentSid
                canSkipPassword = response.can_skip_password
                
                if (canSkipPassword) {
                    doAuth(grantType = "without_password")
                } else {
                    _authState.value = AuthState.NeedPassword(currentSid, canSkip = false)
                }
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Code verification failed")
            }
        }
    }

    fun loginWithPassword(password: String) {
        doAuth(password = password, grantType = "password")
    }

    fun submitCaptcha(captchaKey: String) {
        // In modern VK ID, captcha usually returns a redirect to a web view or a success_token.
        // If it's a simple captcha_sid/key, we'd add it to params.
        // For now, let's allow passing it to doAuth if we extend it.
        // But Fenrir says it's a "VKIdCaptcha" (web-based or special success_token).
    }

    fun submit2FA(code: String) {
        val currentState = authState.value
        if (currentState is AuthState.Need2FA) {
            doAuth(sid = currentState.sid, code = code, grantType = "password") // or appropriate grantType
        }
    }

    private fun doAuth(
        password: String? = null,
        sid: String? = currentSid,
        code: String? = null,
        grantType: String,
        captchaSuccessToken: String? = null
    ) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.directLogin(
                username = currentUsername,
                password = password,
                sid = sid,
                code = code,
                grantType = grantType,
                captchaSuccessToken = captchaSuccessToken
            ).onSuccess { response ->
                handleLoginResponse(response)
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Auth failed")
            }
        }
    }

    private fun handleLoginResponse(response: LoginResponse) {
        when {
            response.access_token != null -> {
                val token = response.access_token
                val userId = response.user_id ?: 0L
                tokenStorage.accessToken = token
                tokenStorage.userId = userId
                
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setUserId(userId.toString())
                
                // Exchange token warming
                viewModelScope.launch {
                    repository.refreshAndWarmToken(token).onSuccess { musicToken ->
                        tokenStorage.musicAccessToken = musicToken
                    }
                }

                _isAuthorized.value = true
                _authState.value = AuthState.Success(token)
            }
            response.error == "need_captcha" -> {
                _authState.value = AuthState.NeedCaptcha(response.captcha_sid ?: "", response.captcha_img ?: "")
            }
            response.error == "need_validation" -> {
                // Check if it's 2FA or Web Validation
                if (response.validation_type == "2fa" || response.validation_type == "phone" || response.validation_type == "2fa_app" || response.validation_type == "2fa_sms") {
                    _authState.value = AuthState.Need2FA(response.validation_sid ?: "", response.phone_mask ?: "")
                } else if (!response.captcha_img.isNullOrEmpty()) {
                    // Sometimes need_validation comes with captcha
                    _authState.value = AuthState.NeedCaptcha(response.validation_sid ?: "", response.captcha_img)
                } else {
                    _authState.value = AuthState.Error(response.error_description ?: "Validation required")
                }
            }
            else -> {
                _authState.value = AuthState.Error(response.error_description ?: "Login failed")
            }
        }
    }

    fun login(username: String, password: String, captchaKey: String? = null, code: String? = null) {
        currentUsername = username
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            // Using captchaKey indirectly via currentSid/sidToUse logic if needed
            val sidToUse = if (captchaKey != null) currentSid else currentSid
            
            repository.directLogin(
                username = username,
                password = password,
                sid = sidToUse,
                code = code,
                grantType = if (password.isNotEmpty()) "password" else "without_password"
            ).onSuccess { response ->
                handleLoginResponse(response)
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Login failed")
            }
        }
    }

    fun logout() {
        tokenStorage.accessToken = null
        tokenStorage.musicAccessToken = null
        tokenStorage.userId = 0L
        _isAuthorized.value = false
        _authState.value = AuthState.Idle
    }

    fun onTokenCaptured(token: String, uId: Long) {
        viewModelScope.launch {
            tokenStorage.accessToken = token
            tokenStorage.userId = uId
            
            com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance().setUserId(uId.toString())
            _authState.value = AuthState.Success(token)
            _isAuthorized.value = true
        }
    }
}
