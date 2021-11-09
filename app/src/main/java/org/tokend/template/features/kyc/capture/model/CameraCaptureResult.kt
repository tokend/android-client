package org.tokend.template.features.kyc.capture.model

import java.io.File
import java.io.Serializable

sealed class CameraCaptureResult : Serializable {
    class Success(
        val imageFile: File
    ) : CameraCaptureResult()

    object Skipped : CameraCaptureResult()
}