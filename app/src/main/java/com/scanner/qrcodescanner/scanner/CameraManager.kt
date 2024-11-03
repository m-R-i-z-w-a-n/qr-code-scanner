package com.scanner.qrcodescanner.scanner

import android.content.ContentValues
import android.content.Context
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraManager(
    private val owner: LifecycleOwner,
    private val context: Context,
    private val viewPreview: PreviewView,
    private var lensFacing: Int,
    private val showHideFlashIcon: (show: Int) -> Unit
) : DefaultLifecycleObserver {

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraProvider: ProcessCameraProvider
    private var stopped: Boolean = false
    private var camera: Camera? = null
    private var flashMode: Int = ImageCapture.FLASH_MODE_OFF

    private val _scanResult = MutableLiveData<ScannerViewState<String>>()
    val scanResult: LiveData<ScannerViewState<String>> get() = _scanResult

    private val cameraExecutor: ExecutorService by lazy {
        Executors.newSingleThreadExecutor()
    }

    init {
        owner.lifecycle.addObserver(this)
        startCamera()
    }

    /**
     * Initialise Camera and call this method again when switch camera is clicked or you want to reinitialise camera.
     */
    private fun startCamera(isSwitchButtonClicked: Boolean = false) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            try {
                controlWhichCameraToDisplay(isSwitchButtonClicked)
                bindCameraUseCases()
            } catch (ex: Exception) {
                _scanResult.postValue(ScannerViewState.Error(ex.message.toString()))
                ex.printStackTrace()
                if (::cameraProvider.isInitialized) {
                    cameraProvider.unbindAll()
                    stopped = true
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Return front or back camera based on which was open last.
     */
    private fun controlWhichCameraToDisplay(isSwitchButtonClicked: Boolean): Int {
        if (isSwitchButtonClicked) {
            lensFacing = if (lensFacing == CameraSelector.LENS_FACING_FRONT) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
        } else lensFacing

        showHideFlashIcon(lensFacing)
        return lensFacing
    }

    /**
     * Bind Camera provider to lifecycle owner.
     */
    private fun bindCameraUseCases() {
        val cameraSelector = getCameraSelector()
        val previewView = getPreviewUseCase()
        imageCapture = getImageCapture()
        cameraProvider.unbindAll()
        try {
            imageCapture?.let {
                bindToLifecycle(cameraProvider, owner, cameraSelector, previewView, it)
            }
            previewView.surfaceProvider = viewPreview.surfaceProvider
        } catch (ex: Exception) {
            _scanResult.postValue(ScannerViewState.Error(ex.message.toString()))
            Log.e(ContentValues.TAG, "Use case binding failed $ex")
        }
    }

    override fun onPause(owner: LifecycleOwner) {
        if (this::cameraProvider.isInitialized) {
            cameraProvider.unbindAll()
            stopped = true
            super.onPause(owner)
        }
    }

    override fun onResume(owner: LifecycleOwner) {
        if (stopped) {
            bindCameraUseCases()
            stopped = false
        }
        super.onResume(owner)
    }


    private fun bindToLifecycle(
        cameraProvider: ProcessCameraProvider,
        owner: LifecycleOwner,
        cameraSelector: CameraSelector,
        previewView: Preview,
        imageCapture: ImageCapture
    ) {
        camera = cameraProvider.bindToLifecycle(
            owner,
            cameraSelector,
            previewView,
            getImageAnalysis(),
            imageCapture
        )
    }

    /**
     * Initialise Camera Selector.
     */
    private fun getCameraSelector(): CameraSelector = CameraSelector.Builder()
        .requireLensFacing(lensFacing)
        .build()

    /**
     * Initialise Preview Builder.
     */
    private fun getPreviewUseCase(): Preview = Preview.Builder().build()

    /**
     * Initialise Image capture builder.
     */
    private fun getImageCapture(): ImageCapture = ImageCapture.Builder().setFlashMode(flashMode).build()

    fun enableFlashForCamera(flashStatus: Boolean) {
        flashMode = if (flashStatus)
            ImageCapture.FLASH_MODE_ON
        else
            ImageCapture.FLASH_MODE_OFF
        // Re-bind use cases to include changes
        imageCapture?.flashMode = flashMode
    }

    private fun getImageAnalysis(): ImageAnalysis {
        return ImageAnalysis.Builder()
            .build()
            .also {
                it.setAnalyzer(
                    cameraExecutor,
                    ScannerAnalyzer(_scanResult::postValue)
                )
            }
    }


    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        cameraExecutor.shutdownNow()
    }
}
