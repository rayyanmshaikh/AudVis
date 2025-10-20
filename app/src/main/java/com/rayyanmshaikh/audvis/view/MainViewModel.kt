package com.rayyanmshaikh.audvis.view
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel to hold and observe UI state for MainActivity.
 */
class MainViewModel : ViewModel() {
    private val _isVisualizerRunning = MutableLiveData(false)
    val isVisualizerRunning: LiveData<Boolean> = _isVisualizerRunning

    fun setRunning(running: Boolean) {
        _isVisualizerRunning.value = running
    }
}
