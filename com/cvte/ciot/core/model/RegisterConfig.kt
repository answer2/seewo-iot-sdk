package com.cvte.ciot.core.model

/**
 *
 * @author AnswerDev
 * @date 2025/12/7 03:46
 * @description RegisterConfig
 */


data class RegisterConfig(
    val url: String,
    val productKey: String,
    val productSecret: String,
    val deviceId : String,
    val mapIdentifiers: HashMap<String, ArrayList<String>> = HashMap()
)  {

    override fun toString(): String {
        return "RegisterConfig{" +
                "url='$url', " +
                "productKey='$productKey', " +
                "productSecret='$productSecret'" +
                "}"
    }
}

class RegisterConfigBuilder {
    var url: String = ""
    var productKey: String = ""
    var productSecret: String = ""
    var deviceId : String = ""
    var identifiers: HashMap<String, ArrayList<String>> = HashMap()

    fun setUrl(url: String): RegisterConfigBuilder {
        this.url = url
        return this
    }

    fun setProductKey(productKey: String): RegisterConfigBuilder {
        this.productKey = productKey
        return this
    }

    fun setProductSecret(productSecret: String): RegisterConfigBuilder {
        this.productSecret = productSecret
        return this
    }

    fun setIdentifiers(identifiers: HashMap<String, ArrayList<String>>): RegisterConfigBuilder {
        this.identifiers = identifiers
        return this
    }



    fun build(): RegisterConfig {
        return RegisterConfig(url, productKey, productSecret, deviceId, identifiers)
    }
}

fun registerConfig(init: RegisterConfigBuilder.() -> Unit): RegisterConfig {
    return RegisterConfigBuilder().apply(init).build()
}