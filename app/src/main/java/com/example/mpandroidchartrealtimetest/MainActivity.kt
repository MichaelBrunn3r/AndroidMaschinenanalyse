package com.example.mpandroidchartrealtimetest

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager.PERMISSION_GRANTED
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
import com.paramsen.noise.Noise
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import kotlin.math.pow
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {
    private val TAG = "MainActivity"

    private var mAudioChart: LineChart? = null

    private val SAMPLE_RATE = 44100
    private val SAMPLE_SIZE = 4096

    val disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        configAudioChart()
    }

    override fun onResume() {
        super.onResume()
        if(requestAudioPermissions() && disposable.size() == 0) {
            startAudioPlot()
        }
    }

    override fun onStop() {
        stopAudioPlot()
        super.onStop()
    }

    private fun startAudioPlot() {
        val audioSrc = AudioSource(SAMPLE_RATE, SAMPLE_SIZE).stream()
        val noise = Noise.real(SAMPLE_SIZE)

        disposable.add(audioSrc.observeOn(Schedulers.newThread())
            .map { samples ->
                var arr = FloatArray(samples.size)
                for(i in 0 until samples.size) {
                    arr[i] = samples[i].toFloat()
                }
                return@map arr
            }
            .subscribe{ samples ->
                val fft = noise.fft(samples, FloatArray(SAMPLE_SIZE+2))
                updateAudioChart(fft)
            }
        )
    }

    private fun stopAudioPlot() {
        disposable.clear()
    }

    private fun configAudioChart() {
        mAudioChart = findViewById(R.id.chartAudio)

        mAudioChart!!.setHardwareAccelerationEnabled(true)
        mAudioChart!!.setTouchEnabled(false)
        mAudioChart!!.isDragEnabled = false
        mAudioChart!!.setScaleEnabled(false)
        mAudioChart!!.setDrawGridBackground(false)
        mAudioChart!!.setPinchZoom(false)
        mAudioChart!!.setViewPortOffsets(80f,50f,0f,10f)
        mAudioChart!!.setBackgroundColor(ContextCompat.getColor(applicationContext, R.color.colorPrimaryDark))

        // Chart Description
        mAudioChart!!.description.isEnabled = false

        // Chart Data
        mAudioChart!!.data =  LineData()

        // Legend
        mAudioChart!!.legend.isEnabled = false

        // X Axis
        val xAxis = mAudioChart!!.xAxis
        xAxis.setDrawGridLines(true)
        xAxis.axisMaximum = SAMPLE_RATE.toFloat()/2
        xAxis.axisMinimum = 0f
        xAxis.setDrawLabels(true)
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.textColor = ContextCompat.getColor(applicationContext, R.color.colorTextOnPrimary)
        xAxis.valueFormatter = LargeValueFormatter("Hz")

        // Y Axis
        val lAxis = mAudioChart!!.axisLeft
        lAxis.setDrawGridLines(true)
        lAxis.axisMaximum = 32768f*15
        lAxis.axisMinimum = 0f
        lAxis.setDrawTopYLabelEntry(true)
        lAxis.textColor = ContextCompat.getColor(applicationContext, R.color.colorTextOnPrimary)
        lAxis.valueFormatter = LargeValueFormatter()

        // Other Axis
        val rAxis = mAudioChart!!.axisRight
        rAxis.isEnabled = false
    }

    @Synchronized
    private fun updateAudioChart(ffts:FloatArray) {
        var data = mAudioChart?.data

        if(data != null) {
            data.clearValues()

            var set = createSet()
            data.addDataSet(set)

            for(i in 0 until ffts.size/2) {
                data.addEntry(Entry(fftToFreq(i, SAMPLE_RATE, SAMPLE_SIZE), fftMagnitude(ffts[i*2],ffts[i*2+1])), 0)
            }
            data.notifyDataChanged()

            mAudioChart!!.notifyDataSetChanged()
            mAudioChart!!.moveViewToX(0f)
        }
    }

    private fun fftToFreq(index:Int, rate:Int, samples:Int):Float {
        return (index * (rate/samples)).toFloat()
    }

    private fun fftMagnitude(real:Float, imaginary:Float):Float {
        return sqrt(real.pow(2)+imaginary.pow(2))
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
}