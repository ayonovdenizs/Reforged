package com.reforged.client.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VKApiValidateAccount(
    val flow_name: String? = null,
    val sid: String? = null,
    val next_step: NextStep? = null,
    val error: AuthError? = null
)

@Serializable
data class AuthError(
    @SerialName("error_code") val error_code: Int,
    @SerialName("error_msg") val error_msg: String? = null,
    val redirect_uri: String? = null,
    val captcha_sid: String? = null,
    val captcha_img: String? = null
)

@Serializable
data class NextStep(
    val verification_method: String? = null,
    val has_another_verification_methods: Boolean = false,
    val phone_mask: String? = null
)

@Serializable
data class EcosystemVerificationMethods(
    val methods: List<VerificationMethod>? = null
)

@Serializable
data class VerificationMethod(
    val name: String,
    val priority: Int = 0,
    val timeout: Int = 0,
    val info: String? = null,
    val can_fallback: Boolean = false
)

@Serializable
data class EcosystemSendOtp(
    val sid: String? = null,
    val code_length: Int = 0,
    val info: String? = null,
    val status: Int = 0
)

@Serializable
data class EcosystemCheckOtp(
    val sid: String? = null,
    val auth_hash: String? = null,
    val can_skip_password: Boolean = false,
    val profile_exist: Boolean = false,
    val profile: EcosystemProfile? = null
)

@Serializable
data class EcosystemProfile(
    val first_name: String? = null,
    val last_name: String? = null,
    val phone: String? = null,
    val photo_200: String? = null,
    val has_2fa: Boolean = false
)

@Serializable
data class VKApiValidatePhone(
    val type: String? = null,
    val sid: String? = null,
    val validation_type: String? = null,
    val code_length: Int = 0,
    val validation_url: String? = null
)

@Serializable
data class LoginResponse(
    val access_token: String? = null,
    val user_id: Long? = null,
    val error: String? = null,
    val error_description: String? = null,
    val validation_type: String? = null,
    val phone_mask: String? = null,
    val validation_sid: String? = null,
    val captcha_sid: String? = null,
    val captcha_img: String? = null,
    val redirect_uri: String? = null
)
