package com.cvte.ciot.core.model

/**
 *
 * @author AnswerDev
 * @date 2025/12/7 03:49
 * @description SSLOption
 */


data class SSLOption(
    var trustStore: String? = null,
    var keyStore: String? = null,
    var privateKey: String? = null,
    var privateKeyPassword: String? = null,
    var enabledCipherSuites: String? = null,
    var enableServerCertAuth: Boolean = false)