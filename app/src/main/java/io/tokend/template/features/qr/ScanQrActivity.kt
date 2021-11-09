package io.tokend.template.features.qr

import android.os.Bundle
import com.google.zxing.*
import com.google.zxing.qrcode.QRCodeReader
import com.journeyapps.barcodescanner.CaptureActivity
import com.journeyapps.barcodescanner.CompoundBarcodeView
import com.journeyapps.barcodescanner.Decoder
import io.tokend.template.R

/**
 * Need to lock qr scanner in portrait orientation and process inverted QRs.
 */
class ScanQrActivity : CaptureActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addInvertedQrDecoding()
    }

    private fun addInvertedQrDecoding() {
        val barcodeScannerView = findViewById(R.id.zxing_barcode_scanner)
                as? CompoundBarcodeView
            ?: return

        barcodeScannerView.barcodeView.setDecoderFactory {
            var invertedBitmapToDecode: BinaryBitmap? = null

            val reader = object : Reader {
                private val readers = listOf(
                    QRCodeReader(),
                    object : QRCodeReader() {
                        override fun decode(ignored: BinaryBitmap): Result {
                            return invertedBitmapToDecode
                                ?.let { super.decode(it) }
                                ?: throw NotFoundException.getNotFoundInstance()
                        }
                    }
                )

                override fun reset() = readers.forEach(Reader::reset)

                override fun decode(
                    image: BinaryBitmap,
                    hints: MutableMap<DecodeHintType, *>
                ) = decode(image)

                override fun decode(image: BinaryBitmap): Result {
                    for (reader in readers) {
                        try {
                            return reader.decode(image)
                        } catch (re: ReaderException) {
                            // continue
                        }
                    }

                    throw NotFoundException.getNotFoundInstance()
                }
            }

            object : Decoder(reader) {
                override fun toBitmap(source: LuminanceSource): BinaryBitmap {
                    invertedBitmapToDecode = super.toBitmap(InvertedLuminanceSource(source))
                    return super.toBitmap(source)
                }
            }
        }
    }
}