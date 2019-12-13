package com.github.michaelbrunn3r.maschinenanalyse

import android.media.AudioRecord
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import java.lang.RuntimeException

class AudioSampleSource(var sampleRate:Int, var samples:Int, var audioSrc:Int, var channelCfg:Int, var audioFormat:Int) {
    private val flowable: Flowable<ShortArray>

    init {
        flowable = Flowable.create<ShortArray>({emitter ->

            val minAudioBufSizeInBytes = AudioRecord.getMinBufferSize(sampleRate, channelCfg, audioFormat)

            if(minAudioBufSizeInBytes <= 0) {
                emitter.onError(RuntimeException("Could not allocate audio buffer on this device. Emulator? No Mic?"))
                return@create
            }

            val recorder = AudioRecord(audioSrc, sampleRate, channelCfg, audioFormat, minAudioBufSizeInBytes)

            recorder.startRecording()
            emitter.setCancellable {
                recorder.stop()
                recorder.release()
            }

            val audioDataBuffer = ShortArray(samples)
            var samplesRead = 0

            while(!emitter.isCancelled) {
                // Try filling the samplesBuffer with samples. Continues in the next loop, if the buffer couldn't be filled completely
                samplesRead += recorder.read(audioDataBuffer, samplesRead, audioDataBuffer.size - samplesRead)

                // Supply audio samples, if samplesBuffer.size samples have been read
                if(samplesRead == audioDataBuffer.size) {
                    val cpy = ShortArray(audioDataBuffer.size)
                    System.arraycopy(audioDataBuffer, 0, cpy, 0, audioDataBuffer.size)
                    emitter.onNext(cpy)

                    samplesRead = 0
                }
            }

        }, BackpressureStrategy.DROP).subscribeOn(Schedulers.io()).share()
    }

    fun stream(): Flowable<ShortArray> {
        return flowable
    }
}