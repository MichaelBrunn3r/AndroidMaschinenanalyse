package com.github.michaelbrunn3r.maschinenanalyse

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
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.paramsen.noise.Noise
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlin.math.pow
import kotlin.math.sqrt

class RecordFragment : Fragment(), Toolbar.OnMenuItemClickListener, SensorEventListener {

    private lateinit var mNavController: NavController
    private lateinit var mToolbar: Toolbar

    private var mRecordingHandler:Handler = Handler()
    private var mNumRecordedFrames = 0
    private var mRecordingBuffer:FloatArray? = null

    private lateinit var mAudioSpectrogram: SpectrogramView

    private var mAccelMean:Float = 0f
    private lateinit var mAccelMeanView: TextView

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mRecordedAccelSamples = 0
    private var mAccelBuffer = 0f
    private var mAccelLastVal = 0f
    private var mMaxCooldown = false

    private var mAudioSampleRate = 44100
    private var mNumAudioSamples = 4096
    private var mAudioAmplitudesSource = FrequencyAmplitudesLiveData()
    private var mIsRecording:Boolean = false

    private lateinit var mMachineanalysisViewModel: MachineanalysisViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_record, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mNavController = Navigation.findNavController(view)

        mToolbar = view.findViewById(R.id.toolbar)
        mToolbar.setTitle(R.string.title_record_fragment)
        mToolbar.setNavigationIcon(R.drawable.back)
        mToolbar.setNavigationOnClickListener {
            mNavController.navigateUp()
        }
        mToolbar.inflateMenu(R.menu.menu_record)
        mToolbar.setOnMenuItemClickListener(this)

        mAudioSpectrogram = view.findViewById(R.id.chartRecordedFrequencies)
        mAccelMeanView = view.findViewById(R.id.meanAccel)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mSensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        mMachineanalysisViewModel = activity?.run {
            ViewModelProviders.of(this)[MachineanalysisViewModel::class.java]
        } ?: throw Exception("Invalid Activity")

        if(requestAudioPermissions()) {
            mAudioAmplitudesSource.observe(this, Observer { audioAmplitudes ->
                mNumRecordedFrames++
                for(i in audioAmplitudes.indices) {
                    mRecordingBuffer!![i] += audioAmplitudes[i]
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        mAudioSampleRate = preferences.getString("fftSampleRate", "44100")!!.toInt()
        mNumAudioSamples = preferences.getString("fftAudioSamples", "4096")!!.toInt()

        mAudioSpectrogram.setFrequencyRange(0f, (mAudioSampleRate/2).toFloat())
        mAudioAmplitudesSource.setSamplesSource(AudioSamplesSource(mAudioSampleRate, mNumAudioSamples, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).stream())
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.miSettings -> {
                mNavController.navigate(R.id.action_recordingFragment_to_settingsFragment)
                return true
            }
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
        if(event != null) {
            when(event.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    val v = sqrt(event.values[0].pow(2) + event.values[1].pow(2) + event.values[2].pow(2)) - 9.81f
                    if(v > 0 && v < mAccelLastVal && !mMaxCooldown) {
                        mRecordedAccelSamples++
                        mAccelBuffer += mAccelLastVal
                        mMaxCooldown = true
                    }
                    if(v > mAccelLastVal) mMaxCooldown = false
                    mAccelLastVal = v
                }
            }
        }
    }

    private fun startRecording() {
        if(mIsRecording) return
        setRecordBtnState(true)

        // Start Accelerometer Recording
        mAccelBuffer = 0f
        mRecordedAccelSamples = 0
        mSensorManager?.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)

        // Start Audio Recording
        mNumRecordedFrames = 0
        mRecordingBuffer = FloatArray(mNumAudioSamples+2)
        mAudioAmplitudesSource.setSamplingState(true)

        // Start Timer
        val recordingDuration = 10000L // in ms
        mRecordingHandler.postDelayed({
            stopRecording()
        }, recordingDuration)
    }

    private fun stopRecording() {
        setRecordBtnState(false)

        // Stop recording
        mSensorManager?.unregisterListener(this)
        mAudioAmplitudesSource.setSamplingState(false)

        // Calculate amplitudes mean
        if(mRecordingBuffer != null) {
            for(i in mRecordingBuffer!!.indices) {
                mRecordingBuffer!![i] = mRecordingBuffer!![i] / mNumRecordedFrames
            }
        }

        // Calculate acceleration mean
        mAccelMean = if(mRecordedAccelSamples > 0) mAccelBuffer/mRecordedAccelSamples else 0.0f

        // Show amplitudes mean
        mAudioSpectrogram.update(mRecordingBuffer!!) { index -> fftFrequenzyBin(index, mAudioSampleRate, mNumAudioSamples)}

        // Show acceleration mean
        mAccelMeanView.text = mAccelMean.toString()
    }

    private fun setRecordBtnState(isSampling: Boolean) {
        val startStopMenuItem: MenuItem? = mToolbar.menu?.findItem(R.id.miRecord)
        if(isSampling) startStopMenuItem?.icon = resources.getDrawable(R.drawable.stop_recording, activity!!.theme)
        else startStopMenuItem?.icon = resources.getDrawable(R.drawable.record, activity!!.theme)
    }

    private fun saveRecording() {
        if(mRecordingBuffer == null || fragmentManager == null) return

        SaveRecordingAsDialogFragment { dialog ->
            if(mRecordingBuffer != null) {
                val s: String = mRecordingBuffer!!.joinToString(separator = ";") { "$it" }
                mMachineanalysisViewModel.insert(Recording(0, dialog.recordingName, mAudioSampleRate, mNumAudioSamples, mAccelMean, s))
            }
        }.show(fragmentManager!!, "saveRecordingAs")
    }

    private fun requestAudioPermissions():Boolean {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && activity!!.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1234)
            println("No Audio Permission granted")
            return false
        }
        return true
    }
}

class SaveRecordingAsDialogFragment(val onPositive:(SaveRecordingAsDialogFragment) -> Unit) : DialogFragment() {
    private lateinit var mInput:EditText
    var recordingName = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            mInput = EditText(it)
            mInput.inputType = InputType.TYPE_CLASS_TEXT

            // Build the dialog and set up the button click handlers
            AlertDialog.Builder(it)
                    .setTitle(R.string.dialog_save_recording_as)
                    .setPositiveButton(R.string.save) {_,_ ->
                        recordingName = mInput.text.toString()
                        onPositive(this)
                    }
                    .setNegativeButton(R.string.cancel) {_,_ -> }
                    .setView(mInput)
                    .create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }
}