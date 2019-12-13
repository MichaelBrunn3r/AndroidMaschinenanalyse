package com.github.michaelbrunn3r.maschinenanalyse

import android.Manifest
import android.content.pm.PackageManager
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

class MonitorFragment : Fragment(), Toolbar.OnMenuItemClickListener {

    private lateinit var mNavController: NavController
    private lateinit var mToolbar: Toolbar

    private var mAudioSpectrogram: SpectrogramView? = null
    private var mAccelSpectrogram: SpectrogramView? = null

    private var mAudioSampleRate = 44100
    private var mAudioSampleSize = 4096

    private var mAudioAmplitudeSource: AudioAmplitudesLiveData? = null

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

        val overlay = view.findViewById<TouchOverlay>(R.id.touchOverlay)
        overlay.setOnShortClickListener {
            toggleToolbar()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        if(requestAudioPermissions()) {
            mAudioAmplitudeSource = AudioAmplitudesLiveData(AudioSampleSource(mAudioSampleRate, mAudioSampleSize, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT))
            mAudioAmplitudeSource?.observe(this, Observer { audioAmplitudes ->
                mAudioSpectrogram?.update(audioAmplitudes) { index -> fftFrequenzyBin(index, mAudioSampleRate, mAudioSampleSize)}
            })
            mAudioAmplitudeSource?.setOnSamplingStateChangedListener {isSampling ->
                val startStopMenuItem: MenuItem? = mToolbar.menu?.findItem(R.id.miStartStop)
                if(isSampling) startStopMenuItem?.icon = resources.getDrawable(R.drawable.pause_btn, activity!!.theme)
                else startStopMenuItem?.icon = resources.getDrawable(R.drawable.play_btn, activity!!.theme)
            }
        }

        if(savedInstanceState != null) {
            mAudioAmplitudeSource?.setSamplingState(savedInstanceState.getBoolean("isSampling", false))
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
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isSampling", mAudioAmplitudeSource?.isSampling?: false)
        outState.putBoolean("isToolbarHidden", mToolbar.visibility != View.VISIBLE)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.miSettings -> {
                mNavController.navigate(R.id.action_monitorFragment_to_settingsFragment)
                return true
            }
            R.id.miStartStop -> {
                mAudioAmplitudeSource?.setSamplingState(!(mAudioAmplitudeSource!!.isSampling))
            }
        }
        return false
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