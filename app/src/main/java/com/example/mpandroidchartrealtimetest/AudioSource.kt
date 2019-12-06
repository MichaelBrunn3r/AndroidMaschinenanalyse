package com.example.mpandroidchartrealtimetest

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import java.lang.RuntimeException
import java.nio.FloatBuffer
import kotlin.math.max

const val AUDIO_SRC = MediaRecorder.AudioSource.MIC
const val AUDIO_CHANNEL_CFG = AudioFormat.CHANNEL_IN_MONO
const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT

class AudioSource(private val SAMPLE_RATE:Int, private val SAMPLE_SIZE:Int) {
    private val flowable: Flowable<FloatArray>

    init {
        flowable = Flowable.create<FloatArray>({emitter ->

            val minAudioBufSizeInBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, AUDIO_CHANNEL_CFG, AUDIO_FORMAT)

            if(minAudioBufSizeInBytes <= 0) {
                emitter.onError(RuntimeException("Could not allocate audio buffer on this device. Emulator? No Mic?"))
                return@create
            }

            val recorder = AudioRecord(AUDIO_SRC, SAMPLE_RATE, AUDIO_CHANNEL_CFG, AUDIO_FORMAT, minAudioBufSizeInBytes)

            recorder.startRecording()
            emitter.setCancellable {
                recorder.stop()
                recorder.release()
            }

            val samplesBuffer = ShortArray(SAMPLE_SIZE/8) // Intermediate audio samples buffer
            val audioDataBuffer = FloatBuffer.allocate(SAMPLE_SIZE)
            var samplesRead = 0

            while(!emitter.isCancelled) {
                // Try filling the samplesBuffer with samples. Continues in the next loop, if the buffer couldn't be filled completely
                samplesRead += recorder.read(samplesBuffer, samplesRead, samplesBuffer.size - samplesRead)

                // Supply audio samples, if samplesBuffer.size samples have been read
                if(samplesRead == samplesBuffer.size) {

                    for(s in samplesBuffer) {
                        audioDataBuffer.put(s.toFloat())
                    }

                    // Supply audio data to subscribers
                    if(!audioDataBuffer.hasRemaining()) {
                        val cpy = FloatArray(audioDataBuffer.array().size)
                        System.arraycopy(audioDataBuffer.array(), 0, cpy, 0, audioDataBuffer.array().size)
                        emitter.onNext(cpy)
                        audioDataBuffer.clear()
                    }

                    samplesRead = 0
                }
            }

        }, BackpressureStrategy.DROP).subscribeOn(Schedulers.io()).share()
    }

    fun stream(): Flowable<FloatArray> {
        return flowable
    }
}