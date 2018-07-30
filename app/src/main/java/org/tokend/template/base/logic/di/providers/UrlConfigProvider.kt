package org.tokend.template.base.logic.di.providers

import org.tokend.template.base.logic.model.UrlConfig

interface UrlConfigProvider {
    fun hasConfig(): Boolean
    fun getConfig(): UrlConfig
    fun setConfig(config: UrlConfig)
}