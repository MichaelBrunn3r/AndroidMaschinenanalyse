package com.github.michaelbrunn3r.maschinenanalyse.sensors

import androidx.lifecycle.LiveData
import com.paramsen.noise.Noise
import io.reactivex.Flowable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers

class FrequenciesLiveData : LiveData<FloatArray>() {

    private var mSamplesSource:Flowable<FloatArray>? = null
    private val mDisposable: CompositeDisposable = CompositeDisposable()
    private var mOnSamplingStateChangedListener: ((Boolean) -> Unit)? = null
    var isSampling: Boolean = false
        private set(value) {
            field = value
            mOnSamplingStateChangedListener?.invoke(value)
        }

    override fun onActive() {
        if (isSampling) startSampling()
    }

    override fun onInactive() {
        if (isSampling) pauseSampling()
    }

    fun setOnSamplingStateChangedListener(l: (isSampling: Boolean) -> Unit) {
        mOnSamplingStateChangedListener = l
    }

    fun setSamplingState(isSampling: Boolean) {
        if (isSampling) startSampling()
        else stopSampling()
    }

    fun setSamplesSource(sampleSource:Flowable<FloatArray>) {
        mSamplesSource = sampleSource
        mDisposable.clear()
        if (isSampling) {
            startSampling()
        }
    }

    private fun startSampling() {
        if (mDisposable.size() != 0 || mSamplesSource == null) return

        mDisposable.add(FrequenciesSource(mSamplesSource!!).stream().observeOn(Schedulers.newThread())
                .subscribe { audioAmplitudes ->
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

class FrequenciesSource(val sampleSource:Flowable<FloatArray>) {

    private var mNoise: Noise? = null

    fun stream(): Flowable<FloatArray> {
        return sampleSource.map { samples ->
            if (mNoise == null) mNoise = Noise.real(samples.size)
            return@map FFT.sequenceToFrequencies(samples, FFT.hannWindow, mNoise!!)
        }
    }
}