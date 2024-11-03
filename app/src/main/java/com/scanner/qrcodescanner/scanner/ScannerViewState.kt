package com.scanner.qrcodescanner.scanner

sealed class ScannerViewState<T>(
    val data: T? = null,
    val message: String? = null,
) {
    class Success<T>(data: T?) : ScannerViewState<T>(data)
    class Error<T>(message: String, data: T? = null) : ScannerViewState<T>(data, message)
}
