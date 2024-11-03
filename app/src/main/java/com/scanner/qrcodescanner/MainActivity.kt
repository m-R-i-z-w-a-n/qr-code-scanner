package com.scanner.qrcodescanner

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import com.scanner.qrcodescanner.databinding.ActivityMainBinding
import com.scanner.qrcodescanner.scanner.ScannerViewState
import com.scanner.qrcodescanner.viewmodels.ScannerViewModel

class MainActivity : AppCompatActivity() {
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private lateinit var scannerViewModel: ScannerViewModel
    private lateinit var viewLifecycleOwner: LifecycleOwner

    private val vibrator: Vibrator   by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        }
        else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startScan()
            } else {
                Toast.makeText(this, "Camera permission is required!", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        viewLifecycleOwner = this

        scannerViewModel = ViewModelProvider(this)[ScannerViewModel::class.java]

        binding.btnScan.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                startScan()
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun startScan() {
        scannerViewModel.startCamera(viewLifecycleOwner, this, binding.previewView).observe(viewLifecycleOwner) { state ->
            if (vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200L, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    vibrator.vibrate(200L)
                }
            }

            when (state) {
                is ScannerViewState.Success -> {
                    binding.qrCodeContent.text = state.data
                }

                is ScannerViewState.Error -> {
                    Toast.makeText(this, "Error reading QR Code: ${state.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
