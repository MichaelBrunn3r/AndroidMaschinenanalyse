package com.github.michaelbrunn3r.maschinenanalyse.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Range
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.github.michaelbrunn3r.maschinenanalyse.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

public class SpectrogramView(context: Context, attrs: AttributeSet): LineChart(context, attrs) {

    private var mGraphColor = Color.BLACK
    private var mGraphLineWidth:Float = 1f
    private var mFrequencyRange = Range(0f,0f)

    init {
        data = LineData()
        setHardwareAccelerationEnabled(true)
        setDrawGridBackground(false)
        setTouchEnabled(true)
        isClickable = true
        isDragEnabled = false
        setPinchZoom(false)

        xAxis.textColor = ContextCompat.getColor(context, R.color.nobel)
        xAxis.valueFormatter = LargeValueFormatter("Hz")

        axisLeft.axisMinimum = 0f
        axisLeft.axisMaximum = 300f
        axisLeft.textColor = ContextCompat.getColor(context, R.color.nobel)
        axisLeft.valueFormatter = LargeValueFormatter()

        axisRight.isEnabled = false

        context.theme.obtainStyledAttributes(attrs, R.styleable.MPAndroidChart, 0, 0).apply {
            val scale = resources.displayMetrics.scaledDensity

            // Interaction
            isScaleXEnabled = isFlagSet(getInt(R.styleable.MPAndroidChart_enableScaling, FLAG_SCALE_NONE), FLAG_SCALE_X_AXIS)
            isScaleYEnabled = isFlagSet(getInt(R.styleable.MPAndroidChart_enableScaling, FLAG_SCALE_NONE), FLAG_SCALE_Y_AXIS)
            isDragXEnabled = isFlagSet(getInt(R.styleable.MPAndroidChart_enableDragging, FLAG_DRAG_NONE), FLAG_DRAG_X)
            isDragYEnabled = isFlagSet(getInt(R.styleable.MPAndroidChart_enableDragging, FLAG_DRAG_NONE), FLAG_DRAG_Y)

            // Graph
            mGraphColor = getColor(R.styleable.MPAndroidChart_graphColor, Color.WHITE)
            mGraphLineWidth = getDimensionPixelSize(R.styleable.MPAndroidChart_graphWidth, 1)/scale

            // Background
            setBackgroundColor(getColor(R.styleable.MPAndroidChart_bgColor, Color.WHITE))

            setExtraOffsets(
                    0f,0f,0f,0f)

            // Description
            description.isEnabled = hasValue(R.styleable.MPAndroidChart_descr)
            if(description.isEnabled) {
                description.text = getString(R.styleable.MPAndroidChart_descr)
                description.textColor = getColor(R.styleable.MPAndroidChart_descrTextColor, Color.BLACK)
            }

            // Legend
            legend.isEnabled = getBoolean(R.styleable.MPAndroidChart_showLegend, true)

            // Axis
            xAxis.setDrawGridLines(isFlagSet(getInt(R.styleable.MPAndroidChart_drawGridLines, FLAG_DRAW_GRID_LINES_BOTH), FLAG_DRAW_GRID_LINES_X))
            xAxis.setDrawLabels(isFlagSet(getInt(R.styleable.MPAndroidChart_drawLabels, FLAG_DRAW_LABELS_BOTH), FLAG_DRAW_LABELS_X))
            xAxis.setAvoidFirstLastClipping(true)

            axisLeft.setDrawGridLines(isFlagSet(getInt(R.styleable.MPAndroidChart_drawGridLines, FLAG_DRAW_GRID_LINES_BOTH), FLAG_DRAW_GRID_LINES_Y))
            axisLeft.setDrawLabels(isFlagSet(getInt(R.styleable.MPAndroidChart_drawLabels, FLAG_DRAW_LABELS_BOTH), FLAG_DRAW_LABELS_Y))
        }
    }

    fun setFrequencyRange(min:Float, max:Float) {
        if(min != mFrequencyRange.lower || max != mFrequencyRange.upper) {
            mFrequencyRange = Range(min, max)
            xAxis.axisMinimum = mFrequencyRange.lower
            xAxis.axisMaximum = mFrequencyRange.upper
            data.clearValues()
            invalidate()
        }
    }

    @Synchronized
    fun update(magnitudes:FloatArray) {
        if(data != null) {
            var set: ILineDataSet? = data.getDataSetByIndex(0)
            if(set == null) {
                set = createSet()
                data.addDataSet(set)
            }

            if(set.entryCount != magnitudes.size) {
                set.clear()
                val stepSize = (mFrequencyRange.upper-mFrequencyRange.lower)/magnitudes.size
                for(i in magnitudes.indices) {
                    data.addEntry(Entry(i*stepSize, magnitudes[i]), 0)
                }
            } else {
                for(i in magnitudes.indices) {
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

    companion object {
        private const val FLAG_SCALE_NONE = 0
        private const val FLAG_SCALE_X_AXIS = 1
        private const val FLAG_SCALE_Y_AXIS = 2
        private const val FLAG_SCALE_BOTH = 3

        private const val FLAG_DRAW_NO_GRID_NONE = 0
        private const val FLAG_DRAW_GRID_LINES_X = 1
        private const val FLAG_DRAW_GRID_LINES_Y = 2
        private const val FLAG_DRAW_GRID_LINES_BOTH = 3

        private const val FLAG_DRAW_LABELS_NONE = 0
        private const val FLAG_DRAW_LABELS_X = 1
        private const val FLAG_DRAW_LABELS_Y = 2
        private const val FLAG_DRAW_LABELS_BOTH = 3

        private const val FLAG_DRAG_NONE = 0
        private const val FLAG_DRAG_X = 1
        private const val FLAG_DRAG_Y = 2
        private const val FLAG_DRAG_BOTH = 3

        inline fun isFlagSet(value:Int, flag:Int):Boolean {return value and flag != 0}
    }
}