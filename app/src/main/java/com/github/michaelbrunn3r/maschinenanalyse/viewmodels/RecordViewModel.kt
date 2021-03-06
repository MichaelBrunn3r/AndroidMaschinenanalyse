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
import com.github.michaelbrunn3r.maschinenanalyse.ui.Settings
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.round

class RecordViewModel : ViewModel() {

    val accelSpectrogramUnfolded = MutableLiveData(false)
    val audioSpectrogramUnfolded = MutableLiveData(false)

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
    private var mAccelFrequenciesBuffer: ArrayList<Float>? = null
    private var mNumRecordedAccelFrequenciesPerBucket: ArrayList<Int>? = null
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

        val bucketsPerHz = 3 // Don't change!!!
        mAccelFrequenciesSource.observeForever { frequencies ->
            // Remember the maximum update frequency during the recording
            val currentFrequency = mAccelerationSource!!.updateFrequency
            mMaxAccelFrequency = max(mMaxAccelFrequency, currentFrequency)

            val numberOfFrequencyBuckets = ceil(FFT.nyquist(mMaxAccelFrequency)*bucketsPerHz)

            // Make sure buffer can hold all frequencies
            if (mAccelFrequenciesBuffer!!.size < numberOfFrequencyBuckets) {
                // Fill missing capacity with zeros
                val difference = (numberOfFrequencyBuckets - mAccelFrequenciesBuffer!!.size).toInt()
                for (i in 0..difference) {
                    mAccelFrequenciesBuffer!!.add(0f)
                    mNumRecordedAccelFrequenciesPerBucket!!.add(0)
                }
            }

            val step = numberOfFrequencyBuckets / frequencies.size
            var frequencyOfIdx = 0f  // Frequency of the i-th element
            for (i in frequencies.indices) {
                val bucketIdx = round(frequencyOfIdx).toInt()
                mAccelFrequenciesBuffer!![bucketIdx] += frequencies[i]
                mNumRecordedAccelFrequenciesPerBucket!![bucketIdx]++

                frequencyOfIdx += step
            }
        }
    }

    private fun startRecording() {
        if (audioCfg.value == null) return

        // Start recording accelerometer
        mMaxAccelFrequency = 0f
        mAccelFrequenciesBuffer = ArrayList(100)
        mAccelFrequenciesBuffer!!.fill(0f)
        mNumRecordedAccelFrequenciesPerBucket = ArrayList(100)
        mNumRecordedAccelFrequenciesPerBucket!!.fill(0)
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
                val numRecordedFrequencies = mNumRecordedAccelFrequenciesPerBucket!![i]
                if(numRecordedFrequencies != 0) floatBuffer[i] = buf[i] / numRecordedFrequencies
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
        recordingDurationMs = preferences.getString("recordingDuration", Settings.DEFAULT_RECORDING_DURATION.toString())!!.toLong()

        // Update audio recording configurations
        val audioSampleRate = preferences.getString("audioSampleRate", Settings.DEFAULT_AUDIO_SAMPLE_RATE.toString())!!.toInt()
        val numAudioSamples = preferences.getString("numAudioSamples", Settings.DEFAULT_NUM_AUDIO_SAMPLES.toString())!!.toInt()

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
                            mAudioFrequenciesBuffer!!.toList(),
                            mMaxAccelFrequency,
                            accelCfg.value!!.numSamples,
                            mAccelFrequenciesBuffer!!,
                            recordingDurationMs,
                            System.currentTimeMillis()
                    )
            )
        }
    }
}