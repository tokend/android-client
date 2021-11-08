package org.tokend.template.features.kyc.model

import org.tokend.sdk.api.base.model.RemoteFile

interface KycFormWithAvatar {
    val avatar: RemoteFile?
}