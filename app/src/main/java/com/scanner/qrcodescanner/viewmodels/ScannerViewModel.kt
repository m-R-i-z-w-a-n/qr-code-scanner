package com.scanner.qrcodescanner.viewmodels

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.scanner.qrcodescanner.scanner.CameraManager
import com.scanner.qrcodescanner.scanner.ScannerViewState

class ScannerViewModel : ViewModel() {
    private lateinit var cameraManager: CameraManager

    /**
     * Initialize Camera Manager class.
     */
    internal fun startCamera(
        viewLifecycleOwner: LifecycleOwner,
        context: Context,
        previewView: PreviewView
    ): LiveData<ScannerViewState<String>> {
        cameraManager = CameraManager(
            owner = viewLifecycleOwner,
            context = context,
            viewPreview = previewView,
            lensFacing = CameraSelector.LENS_FACING_BACK,
            showHideFlashIcon = {}
        )

        return cameraManager.scanResult
    }
}
