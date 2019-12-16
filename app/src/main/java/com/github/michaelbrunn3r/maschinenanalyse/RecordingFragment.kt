package com.github.michaelbrunn3r.maschinenanalyse

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.paramsen.noise.Noise
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlin.math.pow
import kotlin.math.sqrt

class RecordingFragment : Fragment(), Toolbar.OnMenuItemClickListener, SensorEventListener {

    private lateinit var mNavController: NavController
    private lateinit var mToolbar: Toolbar

    private var mSampleRate = 44100
    private var mSampleSize = 4096
    private var mIsRecording:Boolean = false
    private val mDisposable: CompositeDisposable = CompositeDisposable()

    private var mRecordingHandler:Handler = Handler()
    private var mNumRecordedFrames = 0
    private var mRecordingBuffer:FloatArray? = null

    private lateinit var mAudioSpectrogram: SpectrogramView

    private lateinit var mAccelMeanView: TextView

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null
    private var mRecordedAccelSamples = 0
    private var mAccelBuffer = 0f
    private var mAccelLastVal = 0f
    private var mMaxCooldown = false

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
        mAudioSpectrogram.setFrequencyRange(0f, (mSampleRate/2).toFloat())

        mAccelMeanView = view.findViewById(R.id.meanAccel)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mSensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    }

    override fun onResume() {
        super.onResume()

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        mSampleSize = preferences.getString("fftAudioSamples", "4096")!!.toInt()
        mSampleRate = preferences.getString("fftSampleRate", "44100")!!.toInt()
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
        if(mIsRecording || mDisposable.size() != 0) return
        setRecordBtnState(true)

        mAccelBuffer = 0f
        mRecordedAccelSamples = 0
        mSensorManager?.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)

        val audioSrc = AudioSamplesSource(mSampleRate, mSampleSize, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).stream()
        val noise = Noise.real(mSampleSize)

        mRecordingHandler.postDelayed({
            println("Stopping recording")
            stopRecording()
        }, 10000)

        mNumRecordedFrames = 0
        mRecordingBuffer = FloatArray(mSampleSize+2)

        mDisposable.add(audioSrc.observeOn(Schedulers.newThread())
            .map { samples ->
                var arr = FloatArray(samples.size)
                for(i in 0 until samples.size) {
                    arr[i] = samples[i].toFloat()
                }
                return@map noise.fft(arr, FloatArray(mSampleSize+2))
            }.map {fft ->
                return@map calcFFTMagnitudes(fft)
            }.subscribe{ magnitudes ->
                mNumRecordedFrames++
                for(i in magnitudes.indices) {
                    mRecordingBuffer!![i] += magnitudes[i]
                }
            })
    }

    private fun stopRecording() {
        setRecordBtnState(false)
        mDisposable.clear()

        if(mRecordingBuffer != null) {
            for(i in mRecordingBuffer!!.indices) {
                mRecordingBuffer!![i] = mRecordingBuffer!![i] / mNumRecordedFrames
            }
        }

        mAudioSpectrogram.update(mRecordingBuffer!!) { index -> fftFrequenzyBin(index, mSampleRate, mSampleSize)}

        mAccelMeanView.text = (mAccelBuffer/mRecordedAccelSamples).toString()
        mSensorManager?.unregisterListener(this)
    }

    private fun setRecordBtnState(isSampling: Boolean) {
        val startStopMenuItem: MenuItem? = mToolbar.menu?.findItem(R.id.miRecord)
        if(isSampling) startStopMenuItem?.icon = resources.getDrawable(R.drawable.stop_recording, activity!!.theme)
        else startStopMenuItem?.icon = resources.getDrawable(R.drawable.record, activity!!.theme)
    }
}