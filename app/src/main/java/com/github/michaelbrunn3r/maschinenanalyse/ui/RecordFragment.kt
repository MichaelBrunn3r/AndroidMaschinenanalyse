package com.github.michaelbrunn3r.maschinenanalyse.ui

import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.text.InputType
import android.view.*
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.github.michaelbrunn3r.maschinenanalyse.*
import com.github.michaelbrunn3r.maschinenanalyse.database.MachineanalysisViewModel
import com.github.michaelbrunn3r.maschinenanalyse.database.Recording
import com.github.michaelbrunn3r.maschinenanalyse.databinding.FragmentRecordBinding
import kotlin.math.pow
import kotlin.math.sqrt

class RecordFragment : Fragment(), SensorEventListener {

    private lateinit var mBinding: FragmentRecordBinding
    private lateinit var mNavController: NavController

    private var mRecordingHandler: Handler = Handler()
    private var mNumRecordedFrames = 0
    private var mRecordingBuffer: FloatArray? = null

    private var mAccelMean: Float = 0f

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mRecordedAccelSamples = 0
    private var mAccelBuffer = 0f
    private var mAccelLastVal = 0f
    private var mMaxCooldown = false

    private var mAudioSampleRate = 44100
    private var mNumAudioSamples = 4096
    private var mAudioFrequenciesSource = FrequenciesLiveData()
    private var mIsRecording: Boolean = false

    private lateinit var mMachineanalysisViewModel: MachineanalysisViewModel

    private var mRecordingDurationMs = 5000L // Recording Duration in Milliseconds

    private lateinit var mMIRecord: MenuItem

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_record, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mNavController = Navigation.findNavController(view)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mSensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        mMachineanalysisViewModel = activity?.run {
            ViewModelProviders.of(this)[MachineanalysisViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        if (requestAudioPermissions()) {
            mAudioFrequenciesSource.observe(this, Observer { frequencies ->
                mNumRecordedFrames++
                for (i in frequencies.indices) {
                    mRecordingBuffer!![i] += frequencies[i]
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        mAudioSampleRate = preferences.getString("fftSampleRate", "44100")!!.toInt()
        mNumAudioSamples = preferences.getString("fftAudioSamples", "4096")!!.toInt()
        mRecordingDurationMs = preferences.getString("recordingDuration", "4096")!!.toLong()

        mBinding.spectrogram.setFrequencyRange(0f, FFT.nyquist(mAudioSampleRate.toFloat()))
        mAudioFrequenciesSource.setSamplesSource(AudioSamplesSource(mAudioSampleRate, mNumAudioSamples, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).stream())
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_record, menu)
        super.onCreateOptionsMenu(menu, inflater)

        mMIRecord = menu.findItem(R.id.miRecord)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.miRecord -> {
                startRecording()
                return true
            }
            R.id.miSaveRecording -> {
                saveRecording()
                return true
            }
        }
        return false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event != null) {
            when (event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val v = sqrt(event.values[0].pow(2) + event.values[1].pow(2) + event.values[2].pow(2)) - 9.81f
                    if (v > 0 && v < mAccelLastVal && !mMaxCooldown) {
                        mRecordedAccelSamples++
                        mAccelBuffer += mAccelLastVal
                        mMaxCooldown = true
                    }
                    if (v > mAccelLastVal) mMaxCooldown = false
                    mAccelLastVal = v
                }
            }
        }
    }

    private fun startRecording() {
        if (mIsRecording) return
        setRecordBtnState(true)

        // Start Accelerometer Recording
        mAccelBuffer = 0f
        mRecordedAccelSamples = 0
        mSensorManager?.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)

        // Start Audio Recording
        mNumRecordedFrames = 0
        mRecordingBuffer = FloatArray(FFT.numFrequenciesFor(mNumAudioSamples))
        mAudioFrequenciesSource.setSamplingState(true)

        // Start Timer
        mRecordingHandler.postDelayed({
            stopRecording()
        }, mRecordingDurationMs)
    }

    private fun stopRecording() {
        setRecordBtnState(false)

        // Stop recording
        mSensorManager?.unregisterListener(this)
        mAudioFrequenciesSource.setSamplingState(false)

        // Calculate amplitudes mean
        if (mRecordingBuffer != null) {
            for (i in mRecordingBuffer!!.indices) {
                mRecordingBuffer!![i] = mRecordingBuffer!![i] / mNumRecordedFrames
            }
        }

        // Calculate acceleration mean
        mAccelMean = if (mRecordedAccelSamples > 0) mAccelBuffer / mRecordedAccelSamples else 0.0f

        // Show amplitudes mean
        mBinding.spectrogram.update(mRecordingBuffer!!)

        // Show acceleration mean
        mBinding.meanAccel.text = mAccelMean.toString()
    }

    private fun setRecordBtnState(isSampling: Boolean) {
        if (isSampling) mMIRecord.icon = resources.getDrawable(R.drawable.stop_recording, activity!!.theme)
        else mMIRecord.icon = resources.getDrawable(R.drawable.record, activity!!.theme)
    }

    private fun saveRecording() {
        if (mRecordingBuffer == null || fragmentManager == null) return

        SaveRecordingAsDialogFragment { dialog ->
            if (mRecordingBuffer != null) {
                mMachineanalysisViewModel.insert(
                        Recording(
                                0,
                                dialog.recordingName,
                                mAudioSampleRate,
                                mNumAudioSamples,
                                mAccelMean,
                                mRecordingBuffer!!.toList(),
                                mRecordingDurationMs,
                                System.currentTimeMillis()
                        )
                )
            }
        }.show(fragmentManager!!, "saveRecordingAs")
    }

    private fun requestAudioPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity!!.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1234)
            println("No Audio Permission granted")
            return false
        }
        return true
    }
}

class SaveRecordingAsDialogFragment(val onPositive: (SaveRecordingAsDialogFragment) -> Unit) : DialogFragment() {
    private lateinit var mInput: EditText
    var recordingName = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            mInput = EditText(it)
            mInput.inputType = InputType.TYPE_CLASS_TEXT

            // Build the dialog and set up the button click handlers
            AlertDialog.Builder(it)
                    .setTitle(R.string.dialog_save_recording_as)
                    .setPositiveButton(R.string.save) { _, _ ->
                        recordingName = mInput.text.toString()
                        onPositive(this)
                    }
                    .setNegativeButton(R.string.cancel) { _, _ -> }
                    .setView(mInput)
                    .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}