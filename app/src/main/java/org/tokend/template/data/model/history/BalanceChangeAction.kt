package org.tokend.template.data.model.history

enum class BalanceChangeAction {
    LOCKED,
    CHARGED_FROM_LOCKED,
    UNLOCKED,
    CHARGED,
    WITHDRAWN,
    MATCHED,
    ISSUED,
    FUNDED
}