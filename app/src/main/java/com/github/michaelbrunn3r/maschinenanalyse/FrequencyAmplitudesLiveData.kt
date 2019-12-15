package com.github.michaelbrunn3r.maschinenanalyse

import androidx.lifecycle.LiveData
import com.paramsen.noise.Noise
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class FrequencyAmplitudesLiveData : LiveData<FloatArray>() {

    private var mSamplesSource:Flowable<ShortArray>? = null
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

    fun setSamplesSource(sampleSource:Flowable<ShortArray>) {
        mSamplesSource = sampleSource
        mDisposable.clear()
        if(isSampling) {
            startSampling()
        }
    }

    private fun startSampling() {
        if(mDisposable.size() != 0 || mSamplesSource == null) return

        mDisposable.add(FrequencyAmplitudesSource(mSamplesSource!!).stream().observeOn(Schedulers.newThread())
                .subscribe {audioAmplitudes ->
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
}

class FrequencyAmplitudesSource(val sampleSource:Flowable<ShortArray>) {

    private var mNoise: Noise? = null

    fun stream() : Flowable<FloatArray> {
        return sampleSource.map {samples ->
            if(mNoise == null) mNoise = Noise.real(samples.size)

            val fft = FloatArray(samples.size+2)
            mNoise!!.fft(shortArrToFloatArr(samples), fft)
            return@map calcFFTMagnitudes(fft)
        }
    }

    private fun shortArrToFloatArr(shortArr:ShortArray): FloatArray {
        val floatArr = FloatArray(shortArr.size)
        for(i in shortArr.indices) {
            floatArr[i] = shortArr[i].toFloat()
        }
        return floatArr
    }
}