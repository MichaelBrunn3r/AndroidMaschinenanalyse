package com.github.michaelbrunn3r.maschinenanalyse.viewmodels

import android.content.SharedPreferences
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Handler
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.github.michaelbrunn3r.maschinenanalyse.database.MachineanalysisViewModel
import com.github.michaelbrunn3r.maschinenanalyse.database.Recording
import com.github.michaelbrunn3r.maschinenanalyse.sensors.*
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round

class RecordViewModel : ViewModel() {

    val isRecording = MutableLiveData<Boolean>()
    private var recordingDurationMs = 5000L // Recording Duration in Milliseconds
    private var mRecordingHandler: Handler = Handler()

    // Audio
    val recordedAudioFrequencies = MutableLiveData<FloatArray>()
    val audioCfg = MutableLiveData<AudioRecordingConfiguration>()
    private val mAudioFrequenciesSource = FrequenciesLiveData()
    private var mNumRecordedAudioFrames = 0
    private var mAudioFrequenciesBuffer: FloatArray? = null

    // Accelerometer
    val recordedAccelFrequencies = MutableLiveData<FloatArray>()
    val accelCfg = MutableLiveData<AccelerationRecordingConfiguration>()
    val accelFrequency = MutableLiveData<Float>(200f)
    private val mAccelFrequenciesSource = FrequenciesLiveData()
    private var mAccelerationSource: NormalizedAccelerationMagnitudeSamplesSource? = null
    private var mNumRecordedAccelFrames = 0
    private var mAccelFrequenciesBuffer: ArrayList<Float>? = null
    private var mMaxAccelFrequency = 0f

    init {
        audioCfg.observeForever { cfg ->
            val audioSamplesSource = AudioSamplesSource(cfg.sampleRate, cfg.numSamples, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).stream()
            mAudioFrequenciesSource.setSamplesSource(ShortArr2FloatArrSource(audioSamplesSource).stream())
        }
        accelCfg.observeForever { cfg ->
            mAccelerationSource = NormalizedAccelerationMagnitudeSamplesSource(cfg.sensorManager, cfg.numSamples, cfg.constantForce)
            mAccelFrequenciesSource.setSamplesSource(mAccelerationSource!!.stream())
        }
        isRecording.observeForever {
            if (it) startRecording()
            else stopRecording()
        }
        mAudioFrequenciesSource.observeForever { frequencies ->
            mNumRecordedAudioFrames++
            for (i in frequencies.indices) {
                mAudioFrequenciesBuffer!![i] += frequencies[i]
            }
        }
        mAccelFrequenciesSource.observeForever { samples ->
            mNumRecordedAccelFrames++
            val currentFrequency = mAccelerationSource!!.updateFrequency
            mMaxAccelFrequency = max(mMaxAccelFrequency, currentFrequency)

            // Make sure buffer can hold all frequencies
            if (mAccelFrequenciesBuffer!!.size < FFT.nyquist(mMaxAccelFrequency)) {
                val difference = ceil(FFT.nyquist(mMaxAccelFrequency) - mAccelFrequenciesBuffer!!.size).toInt()
                for (i in 0..difference) {
                    mAccelFrequenciesBuffer!!.add(0f)
                }
            }

            val step = FFT.nyquist(currentFrequency / samples.size)
            for (i in samples.indices) {
                val frequency = round(i * step).toInt()
                mAccelFrequenciesBuffer!![frequency] += samples[i]
            }
        }
    }

    private fun startRecording() {
        if (audioCfg.value == null) return

        // Start recording accelerometer
        mNumRecordedAccelFrames = 0
        mMaxAccelFrequency = 0f
        mAccelFrequenciesBuffer = ArrayList(100)
        mAccelFrequenciesBuffer!!.fill(0f)
        mAccelFrequenciesSource.setSamplingState(true)

        // Start recording audio
        mNumRecordedAudioFrames = 0
        mAudioFrequenciesBuffer = FloatArray(FFT.numFrequenciesFor(audioCfg.value!!.numSamples))
        mAudioFrequenciesSource.setSamplingState(true)

        // Start timer
        mRecordingHandler.postDelayed({
            isRecording.value = false
        }, recordingDurationMs)
    }

    private fun stopRecording() {
        // Stop recording
        mAccelFrequenciesSource.setSamplingState(false)
        mAudioFrequenciesSource.setSamplingState(false)

        // Accelerometer
        accelFrequency.value = mMaxAccelFrequency // Has to be updated FIRST
        mAccelFrequenciesBuffer?.let { buf ->
            val floatBuffer = FloatArray(buf.size)
            for (i in buf.indices) {
                val x = buf[i]
                floatBuffer[i] = buf[i] / mNumRecordedAccelFrames
            }

            recordedAccelFrequencies.value = floatBuffer
        }

        // Calculate audio mean
        mAudioFrequenciesBuffer?.let { buf ->
            for (i in buf.indices) {
                buf[i] = buf[i] / mNumRecordedAudioFrames
            }

            recordedAudioFrequencies.value = buf
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
        if (mAudioFrequenciesBuffer != null) {
            dbViewModel.insert(
                    Recording(
                            0,
                            name,
                            audioCfg.value!!.sampleRate,
                            audioCfg.value!!.numSamples,
                            0f, //TODO save acceleration
                            mAudioFrequenciesBuffer!!.toList(),
                            recordingDurationMs,
                            System.currentTimeMillis()
                    )
            )
        }
    }
}