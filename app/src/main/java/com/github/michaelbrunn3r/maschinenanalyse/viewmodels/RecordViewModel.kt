package com.github.michaelbrunn3r.maschinenanalyse.viewmodels

import android.content.SharedPreferences
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Handler
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.michaelbrunn3r.maschinenanalyse.AudioRecordingConfiguration
import com.github.michaelbrunn3r.maschinenanalyse.AudioSamplesSource
import com.github.michaelbrunn3r.maschinenanalyse.FFT
import com.github.michaelbrunn3r.maschinenanalyse.FrequenciesLiveData
import com.github.michaelbrunn3r.maschinenanalyse.database.MachineanalysisViewModel
import com.github.michaelbrunn3r.maschinenanalyse.database.Recording
import kotlin.math.pow
import kotlin.math.sqrt

class RecordViewModel : ViewModel(), SensorEventListener {
    val isRecording = MutableLiveData<Boolean>()
    private var recordingDurationMs = 5000L // Recording Duration in Milliseconds
    private var mRecordingHandler: Handler = Handler()

    // Audio
    val recordedFrequencies = MutableLiveData<FloatArray>()
    val audioCfg = MutableLiveData<AudioRecordingConfiguration>()
    private val audioFrequenciesSource = FrequenciesLiveData()
    private var numRecordedFrames = 0
    private var recordingBuffer: FloatArray? = null

    // Accelerometer
    var sensorManager: SensorManager? = null
    var accelerometer: Sensor? = null
    val recordedAccelMean = MutableLiveData<Float>()
    private var accelBuffer = 0f
    private var recordedAccelSamples = 0
    private var maxCooldown = false
    private var accelLastVal = 0f

    init {
        audioCfg.observeForever { cfg ->
            audioFrequenciesSource.setSamplesSource(AudioSamplesSource(cfg.sampleRate, cfg.numSamples, cfg.source, cfg.channelCfg, cfg.format).stream())
        }
        isRecording.observeForever {
            if (it) startRecording()
            else stopRecording()
        }
        audioFrequenciesSource.observeForever { frequencies ->
            numRecordedFrames++
            for (i in frequencies.indices) {
                recordingBuffer!![i] += frequencies[i]
            }
        }
    }

    private fun startRecording() {
        if (audioCfg.value == null) return

        // Start recording accelerometer
        accelBuffer = 0f
        recordedAccelSamples = 0
        sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME)

        // Start recording audio
        numRecordedFrames = 0
        recordingBuffer = FloatArray(FFT.numFrequenciesFor(audioCfg.value!!.numSamples))
        audioFrequenciesSource.setSamplingState(true)

        // Start timer
        mRecordingHandler.postDelayed({
            isRecording.value = false
        }, recordingDurationMs)
    }

    private fun stopRecording() {
        // Stop recording
        sensorManager?.unregisterListener(this)
        audioFrequenciesSource.setSamplingState(false)

        // Calculate amplitudes mean
        if (recordingBuffer != null) {
            for (i in recordingBuffer!!.indices) {
                recordingBuffer!![i] = recordingBuffer!![i] / numRecordedFrames
            }
        }

        if (recordingBuffer != null) recordedFrequencies.value = recordingBuffer

        recordedAccelMean.value = if (recordedAccelSamples > 0) accelBuffer / recordedAccelSamples else 0.0f
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val v = sqrt(event.values[0].pow(2) + event.values[1].pow(2) + event.values[2].pow(2)) - 9.81f
                    if (v > 0 && v < accelLastVal && !maxCooldown) {
                        recordedAccelSamples++
                        accelBuffer += accelLastVal
                        maxCooldown = true
                    }
                    if (v > accelLastVal) maxCooldown = false
                    accelLastVal = v
                }
            }
        }
    }

    fun onPreferences(preferences: SharedPreferences) {
        recordingDurationMs = preferences.getString("recordingDuration", "4096")!!.toLong()

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

    fun saveRecording(name: String, dbViewModel: MachineanalysisViewModel) {
        if (recordingBuffer != null) {
            dbViewModel.insert(
                    Recording(
                            0,
                            name,
                            audioCfg.value!!.sampleRate,
                            audioCfg.value!!.numSamples,
                            recordedAccelMean.value ?: 0f,
                            recordingBuffer!!.toList(),
                            recordingDurationMs,
                            System.currentTimeMillis()
                    )
            )
        }
    }
}