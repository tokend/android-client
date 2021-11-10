package io.tokend.template.logic.providers

import io.tokend.template.features.urlconfig.model.UrlConfig

interface UrlConfigProvider {
    fun hasConfig(): Boolean
    fun getConfig(): UrlConfig
    fun setConfig(config: UrlConfig)
}