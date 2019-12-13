package com.github.michaelbrunn3r.maschinenanalyse

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.preference.PreferenceManager
import com.paramsen.noise.Noise
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers


class SpectrogramActivity : AppCompatActivity() {
    private var mSampleRate = 44100
    private var mSampleSize = 4096

    private var mAudioSpectrogram: SpectrogramView? = null
    private var mToolbar: Toolbar? = null

    private var mIsSampling:Boolean = false

    private val mDisposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spectrogram)

        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)

        val overlay:TouchOverlay = findViewById(R.id.touchFilter)
        overlay.setOnShortClickListener {
            toggleToolbar()
        }

        mAudioSpectrogram = findViewById(R.id.chartAudio)
        mAudioSpectrogram?.setFrequencyRange(0f, (mSampleRate/2).toFloat())

        if(savedInstanceState != null) {
            mIsSampling = savedInstanceState.getBoolean("isSampling", false)
            if(savedInstanceState.getBoolean("isToolbarHidden", false)) {
                toggleToolbar()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        val preferences = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        mSampleSize = preferences.getString("fftAudioSamples", "4096")!!.toInt()
        mAudioSpectrogram?.setFrequencyRange(0f, (mSampleRate/2).toFloat())

        if(mIsSampling && requestAudioPermissions() && mDisposable.size() == 0) {
            startSampling()
        }
    }

    override fun onStop() {
        pauseSampling()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isSampling", mIsSampling)
        outState.putBoolean("isToolbarHidden", (mToolbar?.visibility ?: View.VISIBLE) != View.VISIBLE)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.miStartStop -> if(requestAudioPermissions()) {
                if(mIsSampling) stopSampling()
                else startSampling()
            }
            R.id.miSettings -> {
                // TODO add navigation to Settings Fragment
            }
        }
        return true
    }

    private fun toggleToolbar() {
        if(mToolbar?.visibility == View.VISIBLE) {
            mToolbar?.visibility = View.GONE
        } else {
            mToolbar?.visibility = View.VISIBLE
        }
    }

    private fun setStartStopBtnState(isSampling: Boolean) {
        val startStopMenuItem: MenuItem? = mToolbar?.menu?.findItem(R.id.miStartStop)
        if(isSampling) startStopMenuItem?.icon = getDrawable(R.drawable.pause_btn)
        else startStopMenuItem?.icon = getDrawable(R.drawable.play_btn)
    }

    private fun startSampling() {
        setStartStopBtnState(true)
        if(mDisposable.size() != 0) return

        val audioSrc = AudioSamplesPublisher(mSampleRate, mSampleSize, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).stream()
        val noise = Noise.real(mSampleSize)

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
                    mAudioSpectrogram?.update(magnitudes) { index -> fftFrequenzyBin(index, mSampleRate, mSampleSize)}
                })

        mIsSampling = true
    }

    private fun pauseSampling() {
        mDisposable.clear()
    }

    private fun stopSampling() {
        setStartStopBtnState(false)
        mDisposable.clear()
        mIsSampling = false
    }

    private fun requestAudioPermissions():Boolean {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(RECORD_AUDIO) != PERMISSION_GRANTED) {
            requestPermissions(arrayOf(RECORD_AUDIO), 1234)
            println("No Audio Permission granted")
            return false
        }
        return true
    }

    companion object {
        private val TAG = "MainActivity"
    }
}