package com.example.mpandroidchartrealtimetest

import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import java.util.*

class MainActivity : AppCompatActivity(), SensorEventListener {
    private val TAG = "MainActivity"

    private var mSensorManager: SensorManager? = null
    private var mAccelerometer: Sensor? = null

    private var mChartAccel: LineChart? = null
    private var mChartAudio: LineChart? = null
    private var mThreadAccel: Thread? = null
    private var plotData = true

    private val mSampleRate = 44100
    private val mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private val mAudioChannel = AudioFormat.CHANNEL_IN_MONO
    private val mMinAudioBufferSize = AudioRecord.getMinBufferSize(mSampleRate, mAudioChannel, mAudioEncoding)
    private var mAudioInput: AudioRecord? = null
    private val mAudioBufferSize = mMinAudioBufferSize
    private val mAudioBuffer = ShortArray(mAudioBufferSize)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager!!.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

        if (mAccelerometer != null) {
            mSensorManager!!.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)
        }

        configAccelChart()
        plotAccelChart()

        println("Min Audio Buffer Size = $mMinAudioBufferSize")
        if (mMinAudioBufferSize == AudioRecord.ERROR_BAD_VALUE) println("ERROR Bad Value")
        if (mMinAudioBufferSize == AudioRecord.ERROR_DEAD_OBJECT) println("ERROR Dead Object")
        if (mMinAudioBufferSize == AudioRecord.ERROR_INVALID_OPERATION) println("ERROR Invalid Operation")

        mAudioInput = AudioRecord(MediaRecorder.AudioSource.MIC,
                mSampleRate, mAudioChannel, mAudioEncoding, mMinAudioBufferSize)
        mAudioInput!!.startRecording()

        configAudioChart()
        plotAudioChart()
    }

    private fun configAudioChart() {
        mChartAudio = findViewById(R.id.chartAudio)

        mChartAudio!!.description.isEnabled = true
        mChartAudio!!.description.text = "Real Time Audio Amplitude"

        mChartAudio!!.setTouchEnabled(false)
        mChartAudio!!.isDragEnabled = false
        mChartAudio!!.setScaleEnabled(false)
        mChartAudio!!.setDrawGridBackground(false)
        mChartAudio!!.setPinchZoom(false)
        mChartAudio!!.setBackgroundColor(Color.WHITE)
        mChartAudio!!.setDrawBorders(false)

        val data = LineData()
        data.setValueTextColor(Color.WHITE)
        mChartAudio!!.data = data

        mChartAudio!!.legend.isEnabled = false

        val xAxis = mChartAudio!!.xAxis
        xAxis.textColor = Color.WHITE
        xAxis.setDrawGridLines(true)
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.isEnabled = true

        val lAxis = mChartAudio!!.axisLeft
        lAxis.textColor = Color.WHITE
        lAxis.setDrawGridLines(true)
        lAxis.axisMaximum = 32768f
        lAxis.axisMinimum = -32768f

        val rAxis = mChartAudio!!.axisRight
        rAxis.isEnabled = false
    }

    private fun configAccelChart() {
        mChartAccel = findViewById(R.id.chartAccel)
        mChartAccel!!.description.isEnabled = true
        mChartAccel!!.description.text = "Real Time Accelerometer Data Plot"

        mChartAccel!!.setTouchEnabled(false)
        mChartAccel!!.isDragEnabled = false
        mChartAccel!!.setScaleEnabled(false)
        mChartAccel!!.setDrawGridBackground(false)
        mChartAccel!!.setPinchZoom(false)
        mChartAccel!!.setBackgroundColor(Color.WHITE)
        mChartAccel!!.setDrawBorders(false)

        val data = LineData()
        data.setValueTextColor(Color.WHITE)
        mChartAccel!!.data = data

        mChartAccel!!.legend.isEnabled = false

        val xAxis = mChartAccel!!.xAxis
        xAxis.textColor = Color.WHITE
        xAxis.setDrawGridLines(true)
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.isEnabled = true

        val lAxis = mChartAccel!!.axisLeft
        lAxis.textColor = Color.WHITE
        lAxis.setDrawGridLines(true)
        lAxis.axisMaximum = 10f
        lAxis.axisMinimum = 0f

        val rAxis = mChartAccel!!.axisRight
        rAxis.isEnabled = false
    }

    private fun plotAccelChart() {
        mThreadAccel?.interrupt()

        mThreadAccel = Thread(Runnable {
            while (true) {
                plotData = true
                try {
                    Thread.sleep(10)
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }

            }
        })

        mThreadAccel!!.start()
    }

    private fun plotAudioChart() {
        Timer().scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    mAudioInput!!.read(mAudioBuffer, 0, mMinAudioBufferSize)
                    updateAudioChart()
                }
            },
            0,
            100
        )
    }

    private fun updateAudioChart() {
        val data = mChartAudio?.data

        if (data != null) {
            var set: ILineDataSet? = data.getDataSetByIndex(0)

            if (set == null) {
                set = createSet()
                data.addDataSet(set)
            }

            for (i in 0 until mAudioBufferSize) {
                data.addEntry(Entry(set.entryCount.toFloat(), mAudioBuffer[i].toFloat()), 0)
            }
            data.notifyDataChanged()

            mChartAudio!!.notifyDataSetChanged()
            mChartAudio!!.setVisibleXRangeMaximum(150f)
            mChartAudio!!.moveViewToX(data.entryCount.toFloat())
        }
    }

    private fun addEntry(event: SensorEvent) {
        val data = mChartAccel!!.data

        if (data != null) {
            var set: ILineDataSet? = data.getDataSetByIndex(0)

            if (set == null) {
                set = createSet()
                data.addDataSet(set)
            }

            val yOffset = 5
            data.addEntry(Entry(set.entryCount.toFloat(), event.values[0] + yOffset), 0)
            data.notifyDataChanged()

            mChartAccel!!.notifyDataSetChanged()
            mChartAccel!!.setVisibleXRangeMaximum(150f)
            mChartAccel!!.moveViewToX(data.entryCount.toFloat())
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
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.cubicIntensity = 0.2f
        return set
    }

    override fun onPause() {
        super.onPause()

        mThreadAccel?.interrupt()
        mSensorManager?.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume()
        mSensorManager?.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME)
    }

    override fun onDestroy() {
        mSensorManager?.unregisterListener(this@MainActivity)
        mThreadAccel?.interrupt()
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (plotData) {
            addEntry(event!!)
            plotData = false
        }
    }
}