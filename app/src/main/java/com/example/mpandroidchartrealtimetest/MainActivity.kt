package com.example.mpandroidchartrealtimetest

import android.Manifest.permission.RECORD_AUDIO
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
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
    private var mSampleRate = 44100
    private val mSampleSize = 4096

    private var mAudioSpectrogram: LineChart? = null
    private var mToolbar: Toolbar? = null

    private var mIsSampling:Boolean = false

    val disposable: CompositeDisposable = CompositeDisposable()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)

        mAudioSpectrogram = findViewById(R.id.chartAudio)
        configAudioSpectrogram()
        mAudioSpectrogram?.setOnClickListener { view ->
            toggleToolbar()
        }
    }

    override fun onStop() {
        setSamplingState(false)
        super.onStop()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.miStartStop -> setSamplingState(!mIsSampling)
        }
        return true
    }

    private fun toggleToolbar() {
        if(mToolbar?.visibility == View.VISIBLE) {
            mToolbar?.visibility = View.GONE
        } else {
            mToolbar?.visibility = View.VISIBLE
        }
    }

    private fun setSamplingState(isSampling:Boolean) {
        if(isSampling) {
            if(disposable.size() != 0) return

            val audioSrc = AudioSamplesPublisher(mSampleRate, mSampleSize, MediaRecorder.AudioSource.MIC, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT).stream()
            val noise = Noise.real(mSampleSize)
            disposable.add(audioSrc.observeOn(Schedulers.newThread())
                    .map { samples ->
                        var arr = FloatArray(samples.size)
                        for(i in 0 until samples.size) {
                            arr[i] = samples[i].toFloat()
                        }
                        return@map noise.fft(arr, FloatArray(mSampleSize+2))
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
        } else {
            disposable.clear()
        }

        var startStopMenuItem: MenuItem? = mToolbar?.menu?.findItem(R.id.miStartStop)
        if(isSampling) startStopMenuItem?.icon = getDrawable(R.drawable.pause_btn)
        else startStopMenuItem?.icon = getDrawable(R.drawable.play_btn)
        mIsSampling = isSampling
    }

    private fun configAudioSpectrogram() {
        mAudioSpectrogram!!.setHardwareAccelerationEnabled(true)
        mAudioSpectrogram!!.setTouchEnabled(true)
        mAudioSpectrogram!!.isClickable = true
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
        xAxis.axisMaximum = mSampleRate.toFloat()/2
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
                    data.addEntry(Entry(fft_frequenzy_bin(i, mSampleRate, mSampleSize), magnitudes[i]), 0)
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