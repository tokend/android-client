package org.tokend.template.features.urlconfig.storage

import android.content.SharedPreferences
import org.tokend.template.data.repository.base.ObjectPersistenceOnPrefs
import org.tokend.template.features.urlconfig.model.UrlConfig

class UrlConfigPersistence(
        preferences: SharedPreferences
) : ObjectPersistenceOnPrefs<UrlConfig>(UrlConfig::class.java, preferences, "url_config")