package io.tokend.template.features.history.model

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