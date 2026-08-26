package com.reforged.client.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reforged.client.data.local.TokenStorage
import com.reforged.client.data.repository.AuthRepository
import com.vk.api.sdk.VK
import com.vk.dto.common.id.UserId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val token: String) : AuthState()
    data class NeedCaptcha(val sid: String, val imgUrl: String) : AuthState()
    data class Need2FA(val sid: String, val phoneMask: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val tokenStorage: TokenStorage
) : ViewModel() {

    private val _isAuthorized = MutableStateFlow(VK.isLoggedIn())
    val isAuthorized: StateFlow<Boolean> = _isAuthorized

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    private var currentUsername = ""
    private var currentPassword = ""

    fun login(username: String, password: String, captchaKey: String? = null, code: String? = null) {
        currentUsername = username
        currentPassword = password
        
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            
            val sid = when (val state = authState.value) {
                is AuthState.NeedCaptcha -> state.sid
                is AuthState.Need2FA -> state.sid
                else -> null
            }
            
            repository.login(
                username = username,
                password = password,
                captchaSid = sid,
                captchaKey = captchaKey,
                code = code
            ).onSuccess { response ->
                when {
                    response.access_token != null -> {
                        val uId = response.user_id ?: 0L
                        tokenStorage.accessToken = response.access_token
                        tokenStorage.userId = uId
                        
                        VK.saveAccessToken(
                            userId = UserId(uId),
                            accessToken = response.access_token,
                            secret = response.secret,
                            expiresInSec = response.expires_in ?: 0,
                            createdMs = System.currentTimeMillis()
                        )
                        _authState.value = AuthState.Success(response.access_token)
                        _isAuthorized.value = true
                    }
                    response.error == "need_captcha" -> {
                        _authState.value = AuthState.NeedCaptcha(
                            sid = response.captcha_sid!!,
                            imgUrl = response.captcha_img!!
                        )
                    }
                    response.error == "need_validation" && 
                    (response.validation_type == "2fa" || response.validation_type == "phone" || response.validation_type == "2fa_app") -> {
                        _authState.value = AuthState.Need2FA(
                            sid = response.validation_sid!!,
                            phoneMask = if (response.validation_type == "2fa_app") "Authenticator App" else response.phone_mask ?: ""
                        )
                    }
                    else -> {
                        _authState.value = AuthState.Error(response.error_description ?: "Login failed")
                    }
                }
            }.onFailure { error ->
                _authState.value = AuthState.Error(error.message ?: "Unknown error")
            }
        }
    }

    fun logout() {
        VK.logout()
        _isAuthorized.value = false
        _authState.value = AuthState.Idle
    }
}
