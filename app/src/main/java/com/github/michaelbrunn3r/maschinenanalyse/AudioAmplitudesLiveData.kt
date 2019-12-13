package com.github.michaelbrunn3r.maschinenanalyse

import androidx.lifecycle.LiveData
import com.paramsen.noise.Noise
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class AudioAmplitudesLiveData(var mAudioSampleSource:AudioSampleSource) : LiveData<FloatArray>() {

    private val mDisposable: CompositeDisposable = CompositeDisposable()
    private var mOnSamplingStateChangedListener : ((Boolean) -> Unit)? = null
    var isSampling:Boolean = false
        private set(value) {
            field = value
            mOnSamplingStateChangedListener?.invoke(value)
        }

    override fun onActive() {
        if(isSampling) startSampling()
    }

    override fun onInactive() {
        if(isSampling) pauseSampling()
    }

    fun setOnSamplingStateChangedListener(l: (isSampling:Boolean) -> Unit) {
        mOnSamplingStateChangedListener = l
    }

    fun setSamplingState(isSampling:Boolean) {
        if(isSampling) startSampling()
        else stopSampling()
    }

    private fun startSampling() {
        if(mDisposable.size() != 0) return

        val noise = Noise.real(mAudioSampleSource.samples)

        mDisposable.add(mAudioSampleSource.stream().observeOn(Schedulers.newThread())
            .map { samples ->
                return@map noise.fft(shortArrToFloatArr(samples), FloatArray(samples.size+2))
            }.map {fft ->
                return@map calcFFTMagnitudes(fft)
            }.subscribe{ audioAmplitudes ->
                postValue(audioAmplitudes)
            })

        isSampling = true
    }

    private fun pauseSampling() {
        mDisposable.clear()
    }

    private fun stopSampling() {
        mDisposable.clear()
        isSampling = false
    }

    private fun shortArrToFloatArr(shortArr:ShortArray): FloatArray {
        val floatArr = FloatArray(shortArr.size)
        for(i in shortArr.indices) {
            floatArr[i] = shortArr[i].toFloat()
        }
        return floatArr
    }
}