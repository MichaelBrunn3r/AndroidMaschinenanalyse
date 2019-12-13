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
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.preference.PreferenceManager
import com.paramsen.noise.Noise
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class MonitorFragment : Fragment(), Toolbar.OnMenuItemClickListener, SensorEventListener {

    private lateinit var mNavController: NavController
    private lateinit var mToolbar: Toolbar

    private var mAudioSpectrogram: SpectrogramView? = null
    private var mAccelSpectrogram: SpectrogramView? = null

    private var mIsSampling:Boolean = false
    private val mDisposable: CompositeDisposable = CompositeDisposable()

    private var mAudioSampleRate = 44100
    private var mAudioSampleSize = 4096

    private lateinit var mSensorManger:SensorManager
    private lateinit var mAccelerometer:Sensor

    private var mAccelSampleRate = 10 // Hz
    private var mAccelSampleSize = 512

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_monitor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mNavController = Navigation.findNavController(view)

        mToolbar = view.findViewById(R.id.toolbar)
        mToolbar.setNavigationIcon(R.drawable.back)
        mToolbar.setNavigationOnClickListener {
            mNavController.navigateUp()
        }
        mToolbar.inflateMenu(R.menu.menu_monitor)
        mToolbar.setOnMenuItemClickListener(this)

        mAudioSpectrogram = view.findViewById(R.id.chartAudio)
        mAccelSpectrogram = view.findViewById(R.id.chartAccel)

        // TODO put into onResume() ???
        mSensorManger = context!!.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManger.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        val overlay = view.findViewById<TouchOverlay>(R.id.touchOverlay)
        overlay.setOnShortClickListener {
            toggleToolbar()
        }

        if(savedInstanceState != null) {
            mIsSampling = savedInstanceState.getBoolean("isSampling", false)
            if(savedInstanceState.getBoolean("isToolbarHidden", false)) {
                toggleToolbar()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        mAudioSampleSize = preferences.getString("fftAudioSamples", "4096")!!.toInt()
        mAudioSpectrogram?.setFrequencyRange(0f, (mAudioSampleRate/2).toFloat())

        mSensorManger.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST)

        if(mIsSampling) startAudioSampling()
    }

    override fun onPause() {
        mSensorManger.unregisterListener(this)
        super.onPause()
    }

    override fun onStop() {
        pauseAudioSampling()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isSampling", mIsSampling)
        outState.putBoolean("isToolbarHidden", mToolbar.visibility != View.VISIBLE)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.miSettings -> {
                mNavController.navigate(R.id.action_monitorFragment_to_settingsFragment)
                return true
            }
            R.id.miStartStop -> {
                if(mIsSampling) stopAudioSampling()
                else startAudioSampling()
                return true
            }
        }
        return false
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }

    override fun onSensorChanged(event: SensorEvent?) {
        // TODO FFT on Accelerometer
    }

    private fun startAudioSampling() {
        if(!requestAudioPermissions() || mDisposable.size() != 0) return
        setStartStopBtnState(true)

        val audioSrc = AudioSamplesPublisher(mAudioSampleRate, mAudioSampleSize, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).stream()
        val noise = Noise.real(mAudioSampleSize)

        mDisposable.add(audioSrc.observeOn(Schedulers.newThread())
                .map { samples ->
                    var arr = FloatArray(samples.size)
                    for(i in 0 until samples.size) {
                        arr[i] = samples[i].toFloat()
                    }
                    return@map noise.fft(arr, FloatArray(mAudioSampleSize+2))
                }.map {fft ->
                    return@map calcFFTMagnitudes(fft)
                }.subscribe{ magnitudes ->
                    mAudioSpectrogram?.update(magnitudes) { index -> fftFrequenzyBin(index, mAudioSampleRate, mAudioSampleSize)}
                })

        mIsSampling = true
    }

    private fun pauseAudioSampling() {
        mDisposable.clear()
    }

    private fun stopAudioSampling() {
        setStartStopBtnState(false)
        mDisposable.clear()
        mIsSampling = false
    }

    private fun setStartStopBtnState(isSampling: Boolean) {
        val startStopMenuItem: MenuItem? = mToolbar.menu?.findItem(R.id.miStartStop)
        if(isSampling) startStopMenuItem?.icon = resources.getDrawable(R.drawable.pause_btn, activity!!.theme)
        else startStopMenuItem?.icon = resources.getDrawable(R.drawable.play_btn, activity!!.theme)
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