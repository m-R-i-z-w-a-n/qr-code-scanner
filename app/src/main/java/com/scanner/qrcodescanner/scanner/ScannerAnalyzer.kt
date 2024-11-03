package com.scanner.qrcodescanner.scanner

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 *  This class scan the codes based on defined format(QR) and deliver the result value.
 */
class ScannerAnalyzer(
    private val onResult: (state: ScannerViewState<String>) -> Unit
) : ImageAnalysis.Analyzer {

    private companion object {
        private const val NEXT_IMAGE_PROCESS_DELAY = 300L
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
            .build()

        val scanner = BarcodeScanning.getClient(options)
        imageProxy.image?.let { mediaImage ->
            InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees).let { inputImage ->
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        for (barcode in barcodes) {
                            onResult(ScannerViewState.Success(barcode.rawValue ?: ""))
                        }
                    }
                    .addOnFailureListener {
                        onResult(ScannerViewState.Error(it.message.toString()))
                    }
                    .addOnCompleteListener {
                        CoroutineScope(Dispatchers.IO).launch {
                            delay(NEXT_IMAGE_PROCESS_DELAY)
                            imageProxy.close()
                        }
                    }
            }
        } ?: onResult(ScannerViewState.Error("Image is empty!"))
    }
}
