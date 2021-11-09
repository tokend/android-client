package io.tokend.template.features.urlconfig.storage

import android.content.SharedPreferences
import io.tokend.template.data.storage.persistence.ObjectPersistenceOnPrefs
import io.tokend.template.features.urlconfig.model.UrlConfig

class UrlConfigPersistence(
    preferences: SharedPreferences
) : ObjectPersistenceOnPrefs<UrlConfig>(UrlConfig::class.java, preferences, "url_config")