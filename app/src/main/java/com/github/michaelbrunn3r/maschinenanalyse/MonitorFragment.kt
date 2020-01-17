package com.github.michaelbrunn3r.maschinenanalyse

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
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import kotlin.math.pow
import kotlin.math.sqrt


class MonitorFragment : Fragment(), Toolbar.OnMenuItemClickListener, SensorEventListener {

    private lateinit var mNavController: NavController
    private lateinit var mToolbar: Toolbar

    private lateinit var mAudioSpectrogram: SpectrogramView
    private lateinit var mAccelChart: RealtimeLinearView

    private var mAudioSampleRate = 44100
    private var mAudioSampleSize = 4096

    private var mAudioAmplitudesSource = FrequencyAmplitudesLiveData()

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null

    private var mIsAccelSampling = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_monitor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mNavController = Navigation.findNavController(view)

        mToolbar = view.findViewById(R.id.toolbar)
        mToolbar.setTitle(R.string.title_monitor_fragment)
        mToolbar.setNavigationIcon(R.drawable.back)
        mToolbar.setNavigationOnClickListener {
            mNavController.navigateUp()
        }
        mToolbar.inflateMenu(R.menu.menu_monitor)
        mToolbar.setOnMenuItemClickListener(this)

        mAudioSpectrogram = view.findViewById(R.id.chartAudio)
        mAccelChart = view.findViewById(R.id.chartAccel)

        val overlay = view.findViewById<TouchOverlay>(R.id.touchOverlay)
        overlay.setOnShortClickListener {
            toggleToolbar()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        mSensorManager = activity?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if(requestAudioPermissions()) {
            mAudioAmplitudesSource.observe(this, Observer { audioAmplitudes ->
                mAudioSpectrogram.update(audioAmplitudes) { index -> fftFrequenzyBin(index, mAudioSampleRate, mAudioSampleSize)}
            })
            mAudioAmplitudesSource.setOnSamplingStateChangedListener { isSampling ->
                val startStopMenuItem: MenuItem? = mToolbar.menu?.findItem(R.id.miStartStop)
                if(isSampling) startStopMenuItem?.icon = resources.getDrawable(R.drawable.pause_btn, activity!!.theme)
                else startStopMenuItem?.icon = resources.getDrawable(R.drawable.play_btn, activity!!.theme)
            }
        }

        if(savedInstanceState != null) {
            mAudioAmplitudesSource.setSamplingState(savedInstanceState.getBoolean("isSampling", false))
            if(savedInstanceState.getBoolean("isToolbarHidden", false)) toggleToolbar()
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
        mAudioSpectrogram.setFrequencyRange(0f, (mAudioSampleRate/2).toFloat())

        mAudioAmplitudesSource.setSamplesSource(AudioSamplesSource(mAudioSampleRate, mAudioSampleSize, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).stream())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isSampling", mAudioAmplitudesSource.isSampling)
        outState.putBoolean("isToolbarHidden", mToolbar.visibility != View.VISIBLE)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.miSettings -> {
                mNavController.navigate(R.id.action_monitorFragment_to_settingsFragment)
                return true
            }
            R.id.miStartStop -> {
                mAudioAmplitudesSource.setSamplingState(!(mAudioAmplitudesSource.isSampling))
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
                    mAccelChart.update(sqrt(event.values[0].pow(2) + event.values[1].pow(2) + event.values[2].pow(2)) - 9.81f)
                }
            }
        }
    }

    private fun toggleToolbar() {
        if(mToolbar.visibility == View.VISIBLE) {
            mToolbar.visibility = View.GONE
        } else {
            mToolbar.visibility = View.VISIBLE
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