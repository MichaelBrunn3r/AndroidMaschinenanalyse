package com.github.michaelbrunn3r.maschinenanalyse

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.paramsen.noise.Noise
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class MonitorFragment : Fragment() {

    private var mAudioSpectrogram: SpectrogramView? = null
    private var mAccelSpectrogram: SpectrogramView? = null

    private var mIsSampling:Boolean = true
    private val mDisposable: CompositeDisposable = CompositeDisposable()

    private var mAudioSampleRate = 44100
    private var mAudioSampleSize = 4096

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_monitor, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mAudioSpectrogram = view.findViewById(R.id.chartAudio)
        mAccelSpectrogram = view.findViewById(R.id.chartAccel)

        if(savedInstanceState != null) {
            mIsSampling = savedInstanceState.getBoolean("isSampling", false)
        }
    }

    override fun onResume() {
        super.onResume()

        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        mAudioSampleSize = preferences.getString("fftAudioSamples", "4096")!!.toInt()
        mAudioSpectrogram?.setFrequencyRange(0f, (mAudioSampleRate/2).toFloat())

        if(mIsSampling) startAudioSampling()
    }

    override fun onStop() {
        pauseAudioSampling()
        super.onStop()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isSampling", mIsSampling)
    }

    private fun startAudioSampling() {
        if(!requestAudioPermissions() || mDisposable.size() != 0) return

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
        mDisposable.clear()
        mIsSampling = false
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