package io.tokend.template.di.providers

import io.tokend.template.features.urlconfig.model.UrlConfig

interface UrlConfigProvider {
    fun hasConfig(): Boolean
    fun getConfig(): UrlConfig
    fun setConfig(config: UrlConfig)
}