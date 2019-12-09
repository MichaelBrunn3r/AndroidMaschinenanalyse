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
import com.paramsen.noise.Noise
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlin.math.pow
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {
    private var mSampleRate = 44100
    private val mSampleSize = 4096

    private var mAudioSpectrogram: SpectrogramView? = null
    private var mToolbar: Toolbar? = null

    private var mIsSampling:Boolean = false

    val disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)

        mAudioSpectrogram = findViewById(R.id.chartAudio)
        mAudioSpectrogram?.config(mSampleRate)
        mAudioSpectrogram?.setOnClickListener { view ->
            toggleToolbar()
        }
    }

    override fun onStop() {
        setSamplingState(false)
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.miStartStop -> setSamplingState(!mIsSampling)
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

    private fun setSamplingState(isSampling:Boolean) {
        if(isSampling) {
            if(disposable.size() != 0) return

            val audioSrc = AudioSamplesPublisher(mSampleRate, mSampleSize, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).stream()
            val noise = Noise.real(mSampleSize)
            disposable.add(audioSrc.observeOn(Schedulers.newThread())
                    .map { samples ->
                        var arr = FloatArray(samples.size)
                        for(i in 0 until samples.size) {
                            arr[i] = samples[i].toFloat()
                        }
                        return@map noise.fft(arr, FloatArray(mSampleSize+2))
                    }.map {fft ->
                        var magnitudes = FloatArray(fft.size)
                        for (i in 0 until fft.size/2) {
                            magnitudes[i] = complexAbs(fft[i*2],fft[i*2+1])
                        }
                        return@map magnitudes
                    }
                    .subscribe{ magnitudes ->
                        mAudioSpectrogram?.update(magnitudes) { index -> fft_frequenzy_bin(index, mSampleRate, mSampleSize)}
                    }
            )
        } else {
            disposable.clear()
        }

        var startStopMenuItem: MenuItem? = mToolbar?.menu?.findItem(R.id.miStartStop)
        if(isSampling) startStopMenuItem?.icon = getDrawable(R.drawable.pause_btn)
        else startStopMenuItem?.icon = getDrawable(R.drawable.play_btn)
        mIsSampling = isSampling
    }

    private fun complexAbs(Re:Float, Im:Float):Float {
        return sqrt(Re.pow(2)+Im.pow(2))
    }

    private fun fft_frequenzy_bin(index:Int, rate:Int, samples:Int):Float {
        return (index * (rate/samples)).toFloat()
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