package com.github.michaelbrunn3r.maschinenanalyse.ui

import android.Manifest
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
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.github.michaelbrunn3r.maschinenanalyse.*
import com.github.michaelbrunn3r.maschinenanalyse.databinding.FragmentMonitorBinding
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt


class MonitorFragment : Fragment(), SensorEventListener {

    private lateinit var mNavController: NavController
    private lateinit var mBinding: FragmentMonitorBinding
    private var mToolbar: Toolbar? = null

    private var mAudioSampleRate = 44100
    private var mAudioSampleSize = 4096

    private var mAudioFrequenciesSource = FrequenciesLiveData()

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null

    private var mIsAccelSampling = false

    private var mMIStartStop: MenuItem? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        setHasOptionsMenu(true)
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_monitor, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mNavController = Navigation.findNavController(view)

        val overlay = view.findViewById<TouchOverlayLayout>(R.id.touchOverlay)
        overlay.setOnShortClickListener {
            mToolbar?.toggle()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mToolbar = activity?.findViewById(R.id.toolbar)

        mSensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if(requestAudioPermissions()) {
            mAudioFrequenciesSource.observe(this, Observer { frequencies ->
                mBinding.audioSpectrogram.update(frequencies)
            })
            mAudioFrequenciesSource.setOnSamplingStateChangedListener { isSampling ->
                mMIStartStop?.apply {
                    icon = if(isSampling) resources.getDrawable(R.drawable.pause_btn, activity!!.theme)
                        else resources.getDrawable(R.drawable.play_btn, activity!!.theme)
                }
            }
        }

        if(savedInstanceState != null) {
            mAudioFrequenciesSource.setSamplingState(savedInstanceState.getBoolean("isSampling", false))
            if(savedInstanceState.getBoolean("isToolbarHidden", false)) mToolbar?.toggle()
        }
    }

    override fun onPause() {
        super.onPause()
        if(mIsAccelSampling) mSensorManager?.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        mAudioSampleSize = preferences.getString("fftAudioSamples", "4096")!!.toInt()
        mBinding.audioSpectrogram.setFrequencyRange(0f, FFT.nyquist(mAudioSampleRate.toFloat()))

        mAudioFrequenciesSource.setSamplesSource(AudioSamplesSource(mAudioSampleRate, mAudioSampleSize, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).stream())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isSampling", mAudioFrequenciesSource.isSampling)
        outState.putBoolean("isToolbarHidden", mToolbar?.visibility != View.VISIBLE)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_monitor, menu)
        super.onCreateOptionsMenu(menu, inflater)

        mMIStartStop = menu.findItem(R.id.miStartStop)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.miStartStop -> {
                mAudioFrequenciesSource.setSamplingState(!(mAudioFrequenciesSource.isSampling))
                if(mAccelerometer != null) {
                    if(mIsAccelSampling) mSensorManager?.unregisterListener(this)
                    else mSensorManager?.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)
                    mIsAccelSampling = !mIsAccelSampling
                }
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
                    val value = max(sqrt(event.values[0].pow(2) + event.values[1].pow(2) + event.values[2].pow(2)) - 9.81f, 0.0f)
                    mBinding.accelRealtimeGraph.update(value)
                }
            }
        }
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