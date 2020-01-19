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
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.github.michaelbrunn3r.maschinenanalyse.databinding.FragmentMonitorBinding
import com.github.michaelbrunn3r.maschinenanalyse.databinding.FragmentRecordBinding
import kotlin.math.pow
import kotlin.math.sqrt


class MonitorFragment : Fragment(), Toolbar.OnMenuItemClickListener, SensorEventListener {

    private lateinit var mNavController: NavController
    private lateinit var mBinding: FragmentMonitorBinding

    private var mAudioSampleRate = 44100
    private var mAudioSampleSize = 4096

    private var mAudioAmplitudesSource = FrequencyAmplitudesLiveData()

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null

    private var mIsAccelSampling = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mBinding = DataBindingUtil.inflate(inflater, R.layout.fragment_monitor, container, false)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mNavController = Navigation.findNavController(view)

        mBinding.apply {
            toolbar as Toolbar
            toolbar.setTitle(R.string.title_monitor_fragment)
            toolbar.setNavigationIcon(R.drawable.back)
            toolbar.setNavigationOnClickListener {
                mNavController.navigateUp()
            }
            toolbar.inflateMenu(R.menu.menu_monitor)
            toolbar.setOnMenuItemClickListener(this@MonitorFragment)
        }

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
                mBinding.audioSpectrogram.update(audioAmplitudes) { index -> fftFrequenzyBin(index, mAudioSampleRate, mAudioSampleSize)}
            })
            mAudioAmplitudesSource.setOnSamplingStateChangedListener { isSampling ->
                val startStopMenuItem: MenuItem? = (mBinding.toolbar as Toolbar).menu?.findItem(R.id.miStartStop)
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
        mBinding.audioSpectrogram.setFrequencyRange(0f, (mAudioSampleRate/2).toFloat())

        mAudioAmplitudesSource.setSamplesSource(AudioSamplesSource(mAudioSampleRate, mAudioSampleSize, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).stream())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isSampling", mAudioAmplitudesSource.isSampling)
        outState.putBoolean("isToolbarHidden", (mBinding.toolbar as Toolbar).visibility != View.VISIBLE)
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
                    mBinding.accelRealtimeGraph.update(sqrt(event.values[0].pow(2) + event.values[1].pow(2) + event.values[2].pow(2)) - 9.81f)
                }
            }
        }
    }

    private fun toggleToolbar() {
        mBinding.apply {
            toolbar.visibility = when(toolbar.visibility) {
                View.VISIBLE -> View.GONE
                else -> View.VISIBLE
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