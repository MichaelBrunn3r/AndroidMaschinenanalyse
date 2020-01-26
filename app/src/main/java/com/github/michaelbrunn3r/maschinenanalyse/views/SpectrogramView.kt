package com.github.michaelbrunn3r.maschinenanalyse.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Range
import com.github.michaelbrunn3r.maschinenanalyse.R
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

public class SpectrogramView(context: Context, attrs: AttributeSet) : StyleableLineChart(context, attrs) {

    private var mGraphColor = Color.BLACK
    private var mGraphLineWidth: Float = 1f
    private var mFrequencyRange = Range(0f, 0f)

    init {
        data = LineData()
        setHardwareAccelerationEnabled(true)
        isClickable = true

        xAxis.valueFormatter = LargeValueFormatter("Hz")
        axisLeft.valueFormatter = LargeValueFormatter()

        context.theme.obtainStyledAttributes(attrs, R.styleable.Graph, 0, 0).apply {
            val scale = resources.displayMetrics.scaledDensity

            // Graph
            mGraphColor = getColor(R.styleable.Graph_graphColor, Color.WHITE)
            mGraphLineWidth = getDimensionPixelSize(R.styleable.Graph_graphWidth, 1) / scale

            // Axis
            xAxis.setAvoidFirstLastClipping(true)
        }
    }

    fun setFrequencyRange(min: Float, max: Float) {
        if (min != mFrequencyRange.lower || max != mFrequencyRange.upper) {
            mFrequencyRange = Range(min, max)
            xAxis.axisMinimum = mFrequencyRange.lower
            xAxis.axisMaximum = mFrequencyRange.upper
            data.clearValues()
            invalidate()
        }
    }

    @Synchronized
    fun update(magnitudes: FloatArray) {
        if (data != null) {
            var set: ILineDataSet? = data.getDataSetByIndex(0)
            if (set == null) {
                set = createSet()
                data.addDataSet(set)
            }

            if (set.entryCount != magnitudes.size) {
                set.clear()
                val stepSize = (mFrequencyRange.upper - mFrequencyRange.lower) / magnitudes.size
                for (i in magnitudes.indices) {
                    data.addEntry(Entry(i * stepSize, magnitudes[i]), 0)
                }
            } else {
                for (i in magnitudes.indices) {
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
        set.lineWidth = mGraphLineWidth
        set.color = mGraphColor
        set.isHighlightEnabled = false
        set.setDrawValues(false)
        set.setDrawCircles(false)
        set.mode = LineDataSet.Mode.LINEAR
        return set
    }
}