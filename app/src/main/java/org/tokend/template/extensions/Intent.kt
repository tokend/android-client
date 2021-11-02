package org.tokend.template.extensions

import android.content.Intent
import org.tokend.sdk.utils.BigDecimalUtil
import java.math.BigDecimal

/**
 * Allows to specify default value for string extra
 * @return extra value or [default] if there is no such extra
 */
fun Intent.getStringExtra(key: String, default: String): String {
    return extras?.getString(key, default) ?: default
}

/**
 * @return extra value or null if there is no such extra
 */
fun Intent.getNullableStringExtra(key: String): String? {
    return extras?.getString(key, null)
}

/**
 * Allows to parse BigDecimal from string extra
 * @return parsed value or [default] if there is no such extra
 */
fun Intent.getBigDecimalExtra(key: String, default: BigDecimal): BigDecimal {
    return BigDecimalUtil.valueOf(getStringExtra(key, ""), default)
}

/**
 * Allows to parse BigDecimal from string extra
 * @return parsed value or 0 if there is no such extra
 */
fun Intent.getBigDecimalExtra(key: String): BigDecimal {
    return getBigDecimalExtra(key, BigDecimal.ZERO)
}

fun Intent.newTask() = this.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

fun Intent.singleTop() = this.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)

fun Intent.clearTop() = this.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)