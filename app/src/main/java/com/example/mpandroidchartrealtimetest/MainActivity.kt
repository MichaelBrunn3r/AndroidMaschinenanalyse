package com.example.mpandroidchartrealtimetest

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.paramsen.noise.Noise
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import androidx.core.app.ComponentActivity
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.util.*


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
            .subscribe{ samples ->
                updateAudioChart(samples)
            }
        )
    }

    private fun stopAudioPlot() {
        disposable.clear()
    }

    private fun configAudioChart() {
        mAudioChart = findViewById(R.id.chartAudio)

        mAudioChart!!.setTouchEnabled(false)
        mAudioChart!!.isDragEnabled = false
        mAudioChart!!.setScaleEnabled(false)
        mAudioChart!!.setDrawGridBackground(false)
        mAudioChart!!.setPinchZoom(false)
        mAudioChart!!.setBackgroundColor(Color.WHITE)
        mAudioChart!!.setDrawBorders(true)
        mAudioChart!!.setViewPortOffsets(0f,0f,0f,0f)
        mAudioChart!!.setVisibleXRangeMaximum(SAMPLE_SIZE.toFloat())

        // Chart Description
        mAudioChart!!.description.isEnabled = true
        mAudioChart!!.description.text = "Real Time Audio Amplitude"

        // Chart Data
        val data = LineData()
        data.setValueTextColor(Color.WHITE)
        mAudioChart!!.data = data

        // Legend
        mAudioChart!!.legend.isEnabled = false

        // X Axis
        //mAudioChart!!.setVisibleXRangeMaximum(40f)
        val xAxis = mAudioChart!!.xAxis
        xAxis.textColor = Color.WHITE
        xAxis.setDrawGridLines(true)
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.isEnabled = true

        // Y Axis
        val lAxis = mAudioChart!!.axisLeft
        lAxis.textColor = Color.WHITE
        lAxis.setDrawGridLines(true)
        lAxis.axisMaximum = 32768f
        lAxis.axisMinimum = -32768f

        // Other Axis
        val rAxis = mAudioChart!!.axisRight
        rAxis.isEnabled = false
    }

    @Synchronized
    private fun updateAudioChart(samples:ShortArray) {
        var data = mAudioChart?.data

        if(data != null) {
            data.clearValues()

            var set = createSet()
            data.addDataSet(set)


            for(sample in samples) {
                data.addEntry(Entry(set.entryCount.toFloat(), sample.toFloat()), 0)
            }
            data.notifyDataChanged()

            mAudioChart!!.notifyDataSetChanged()
            mAudioChart!!.moveViewToX(10f)
        }
    }

    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Dynamic")
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.lineWidth = 1f
        set.color = Color.MAGENTA
        set.isHighlightEnabled = false
        set.setDrawValues(false)
        set.setDrawCircles(false)
        set.mode = LineDataSet.Mode.LINEAR
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