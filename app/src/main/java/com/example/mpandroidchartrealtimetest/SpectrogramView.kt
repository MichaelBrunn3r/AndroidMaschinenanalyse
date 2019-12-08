package com.example.mpandroidchartrealtimetest

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

class SpectrogramView: LineChart {

    constructor(context:Context) : super(context)
    constructor(context:Context, attrs:AttributeSet) : super(context, attrs)
    constructor(context:Context, attrs:AttributeSet, defStyle:Int) : super(context, attrs, defStyle)


    fun config(samplingRate:Int) {
        setHardwareAccelerationEnabled(true)
        setTouchEnabled(true)
        isClickable = true
        isDragEnabled = false
        setScaleEnabled(false)
        setDrawGridBackground(false)
        setPinchZoom(false)
        setViewPortOffsets(80f,50f,0f,10f)
        setBackgroundColor(ContextCompat.getColor(context, R.color.colorPrimaryDark))

        // Chart Description
        description.isEnabled = false

        // Chart Data
        data =  LineData()

        // Legend
        legend.isEnabled = false

        // X Axis
        xAxis.setDrawGridLines(true)
        xAxis.axisMaximum = samplingRate.toFloat()/2
        xAxis.axisMinimum = 0f
        xAxis.setDrawLabels(true)
        xAxis.setAvoidFirstLastClipping(true)
        xAxis.textColor = ContextCompat.getColor(context, R.color.colorTextOnPrimary)
        xAxis.valueFormatter = LargeValueFormatter("Hz")

        // Y Axis
        axisLeft.setDrawGridLines(true)
        axisLeft.axisMaximum = 32768f*15
        axisLeft.axisMinimum = 0f
        axisLeft.setDrawTopYLabelEntry(true)
        axisLeft.textColor = ContextCompat.getColor(context, R.color.colorTextOnPrimary)
        axisLeft.valueFormatter = LargeValueFormatter()

        // Other Axis
        axisRight.isEnabled = false
    }

    @Synchronized
    fun update(magnitudes:FloatArray, frequenzyForIndex: (index:Int) -> Float) {
        if(data != null) {
            var set: ILineDataSet? = data.getDataSetByIndex(0)
            if(set == null) {
                set = createSet()
                data.addDataSet(set)
            }

            if(set.entryCount == 0) {
                for(i in 0 until magnitudes.size) {
                    data.addEntry(Entry(frequenzyForIndex(i), magnitudes[i]), 0)
                }
            } else {9
                for(i in 0 until magnitudes.size) {
                    set.getEntryForIndex(i).y = magnitudes[i]
                }
            }

            data.notifyDataChanged()

            notifyDataSetChanged()
            invalidate()
        }
    }

    private fun createSet(): LineDataSet {
        val set = LineDataSet(null, "Dynamic")
        set.axisDependency = YAxis.AxisDependency.LEFT
        set.lineWidth = 1f
        set.color = ContextCompat.getColor(context, R.color.colorAccent)
        set.isHighlightEnabled = false
        set.setDrawValues(false)
        set.setDrawCircles(false)
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.cubicIntensity = 1f
        return set
    }

}