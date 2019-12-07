package com.example.mpandroidchartrealtimetest

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.paramsen.noise.Noise
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlin.math.pow
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {
    private val SAMPLE_RATE = 44100
    private val SAMPLE_SIZE = 4096

    private var mAudioSpectrogram: LineChart? = null

    val disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configAudioChart()
    }

    override fun onResume() {
        super.onResume()
        if(requestAudioPermissions()) {
            startAudioSampling()
        }
    }

    override fun onStop() {
        stopAudioSampling()
        super.onStop()
    }

    private fun startAudioSampling() {
        if(disposable.size() != 0) return

        val audioSrc = AudioSamplesPublisher(SAMPLE_RATE, SAMPLE_SIZE, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).stream()
        val noise = Noise.real(SAMPLE_SIZE)
        disposable.add(audioSrc.observeOn(Schedulers.newThread())
            .map { samples ->
                var arr = FloatArray(samples.size)
                for(i in 0 until samples.size) {
                    arr[i] = samples[i].toFloat()
                }
                return@map noise.fft(arr, FloatArray(SAMPLE_SIZE+2))
            }.map {fft ->
                var magnitudes = FloatArray(fft.size)
                for (i in 0 until fft.size/2) {
                    magnitudes[i] = complexAbs(fft[i*2],fft[i*2+1])
                }
                    return@map magnitudes
            }
            .subscribe{ magnitudes ->
                updateAudioSpectrogram(magnitudes)
            }
        )
    }

    private fun stopAudioSampling() {
        disposable.clear()
    }

    private fun configAudioChart() {
        mAudioSpectrogram = findViewById(R.id.chartAudio)

        mAudioSpectrogram!!.setHardwareAccelerationEnabled(true)
        mAudioSpectrogram!!.setTouchEnabled(false)
        mAudioSpectrogram!!.isDragEnabled = false
        mAudioSpectrogram!!.setScaleEnabled(false)
        mAudioSpectrogram!!.setDrawGridBackground(false)
        mAudioSpectrogram!!.setPinchZoom(false)
        mAudioSpectrogram!!.setViewPortOffsets(80f,50f,0f,10f)
        mAudioSpectrogram!!.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.colorPrimaryDark))

        // Chart Description
        mAudioSpectrogram!!.description.isEnabled = false

        // Chart Data
        mAudioSpectrogram!!.data =  LineData()

        // Legend
        mAudioSpectrogram!!.legend.isEnabled = false

        // X Axis
        val xAxis = mAudioSpectrogram!!.xAxis
        xAxis.setDrawGridLines(true)
        xAxis.axisMaximum = SAMPLE_RATE.toFloat()/2
        xAxis.axisMinimum = 0f
        xAxis.setDrawLabels(true)
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.textColor = ContextCompat.getColor(applicationContext, R.color.colorTextOnPrimary)
        xAxis.valueFormatter = LargeValueFormatter("Hz")

        // Y Axis
        val lAxis = mAudioSpectrogram!!.axisLeft
        lAxis.setDrawGridLines(true)
        lAxis.axisMaximum = 32768f*15
        lAxis.axisMinimum = 0f
        lAxis.setDrawTopYLabelEntry(true)
        lAxis.textColor = ContextCompat.getColor(applicationContext, R.color.colorTextOnPrimary)
        lAxis.valueFormatter = LargeValueFormatter()

        // Other Axis
        val rAxis = mAudioSpectrogram!!.axisRight
        rAxis.isEnabled = false
    }

    @Synchronized
    private fun updateAudioSpectrogram(magnitudes:FloatArray) {
        var data = mAudioSpectrogram?.data

        if(data != null) {
            var set: ILineDataSet? = data.getDataSetByIndex(0)
            if(set == null) {
                set = createSet()
                data.addDataSet(set)
            }

            if(set.entryCount == 0) {
                for(i in 0 until magnitudes.size) {
                    data.addEntry(Entry(fft_frequenzy_bin(i, SAMPLE_RATE, SAMPLE_SIZE), magnitudes[i]), 0)
                }
            } else {
                for(i in 0 until magnitudes.size) {
                    set.getEntryForIndex(i).y = magnitudes[i]
                }
            }

            data.notifyDataChanged()

            mAudioSpectrogram!!.notifyDataSetChanged()
            mAudioSpectrogram!!.invalidate()
        }
    }

    private fun fft_frequenzy_bin(index:Int, rate:Int, samples:Int):Float {
        return (index * (rate/samples)).toFloat()
    }

    private fun complexAbs(Re:Float, Im:Float):Float {
        return sqrt(Re.pow(2)+Im.pow(2))
    }

    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Dynamic")
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.lineWidth = 1f
        set.color = ContextCompat.getColor(applicationContext, R.color.colorAccent)
        set.isHighlightEnabled = false
        set.setDrawValues(false)
        set.setDrawCircles(false)
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.cubicIntensity = 1f
        return set
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