package org.tokend.template.util.locale;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;

import java.util.Locale;

/**
 * Created by Oleg Koretsky on 10/18/17.
 */

public class LocalizedContextWrapper {
    public static Context wrap(Context context, Locale locale) {
        Resources res = context.getResources();
        Configuration configuration = res.getConfiguration();

        if (Build.VERSION.SDK_INT >= 24) {
            configuration.setLocale(locale);

            LocaleList localeList = new LocaleList(locale);
            LocaleList.setDefault(localeList);
            configuration.setLocales(localeList);

            context = context.createConfigurationContext(configuration);

        } else if (Build.VERSION.SDK_INT >= 17) {
            configuration.setLocale(locale);
            context = context.createConfigurationContext(configuration);

        } else {
            configuration.locale = locale;
            res.updateConfiguration(configuration, res.getDisplayMetrics());
        }

        return context;
    }
}
