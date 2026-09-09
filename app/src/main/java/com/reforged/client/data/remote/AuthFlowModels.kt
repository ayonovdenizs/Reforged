package com.reforged.client.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class VKApiValidateAccount(
    val flow_name: String? = null,
    val sid: String? = null,
    val next_step: NextStep? = null
)

@Serializable
data class NextStep(
    val verification_method: String? = null,
    val has_another_verification_methods: Boolean = false
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
    val captcha_img: String? = null
)
