package com.reforged.client.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.remote.LoginResponse
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
    data class SelectValidationMethod(val sid: String, val methods: List<String>) : AuthState()
    data class CodeValidation(val sid: String, val method: String) : AuthState()
    data class NeedPassword(val sid: String) : AuthState()
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

    fun startLogin(username: String) {
        currentUsername = username
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.validateAccount(username).onSuccess { validation ->
                currentSid = validation.sid ?: ""
                when {
                    validation.flow_name == "need_password_and_validation" || validation.flow_name == "need_password" -> {
                        _authState.value = AuthState.NeedPassword(currentSid)
                    }
                    validation.next_step?.verification_method != null -> {
                        // For now, simplify: if there's a next step, we might need to select method
                        // Fenrir does getEcosystemVerificationMethods(sid) here
                        // We'll jump to password or simple SMS if possible
                        _authState.value = AuthState.NeedPassword(currentSid)
                    }
                    else -> {
                        _authState.value = AuthState.NeedPassword(currentSid)
                    }
                }
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Validation failed")
            }
        }
    }

    fun loginWithPassword(password: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            repository.directLogin(
                username = currentUsername,
                password = password,
                sid = currentSid,
                grantType = "password"
            ).onSuccess { response ->
                handleLoginResponse(response)
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Login failed")
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
                    repository.authByExchangeToken(token).onSuccess { musicToken ->
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
                _authState.value = AuthState.Need2FA(response.validation_sid ?: "", response.phone_mask ?: "")
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
