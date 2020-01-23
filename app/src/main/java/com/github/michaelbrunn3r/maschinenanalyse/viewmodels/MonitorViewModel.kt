package com.github.michaelbrunn3r.maschinenanalyse.viewmodels

import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.MediaRecorder
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.michaelbrunn3r.maschinenanalyse.*

class MonitorViewModel : ViewModel() {
    val isMonitoring = MutableLiveData(false)
    val isToolbarHidden = MutableLiveData(false)

    // Audio
    val audioCfg = MutableLiveData<AudioRecordingConfiguration>()
    val audioFrequenciesSource = FrequenciesLiveData()

    // Accelerometer
    val accelSource = NormalizedAccelerationLiveData()

    init {
        audioCfg.observeForever { cfg ->
            val audioSamplesSource = AudioSamplesSource(cfg.sampleRate, cfg.numSamples, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).stream()
            audioFrequenciesSource.setSamplesSource(ShortArr2FloatArrSource(audioSamplesSource).stream())
        }
        isMonitoring.observeForever {
            audioFrequenciesSource.setSamplingState(it)
            accelSource.setSamplingState(it)
        }
    }

    fun onPreferences(preferences: SharedPreferences) {
        // Update audio recording configurations
        val audioSampleRate = preferences.getString("fftSampleRate", "44100")!!.toInt()
        val numAudioSamples = preferences.getString("fftAudioSamples", "4096")!!.toInt()

        val audioConfigChanged = audioCfg.value?.run { sampleRate != audioSampleRate || numSamples != numAudioSamples }
                ?: true
        if (audioConfigChanged) {
            audioCfg.value = AudioRecordingConfiguration(audioSampleRate,
                    numAudioSamples,
                    MediaRecorder.AudioSource.MIC,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT)
        }
    }
}