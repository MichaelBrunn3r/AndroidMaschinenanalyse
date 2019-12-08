package com.example.mpandroidchartrealtimetest

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

class SpectrogramView(context: Context, attrs: AttributeSet): LineChart(context, attrs) {

    private var mGraphColor = Color.BLACK
    private var mGraphLineWidth:Float = 1f

    init {
        data = LineData()
        setHardwareAccelerationEnabled(true)

        context.theme.obtainStyledAttributes(attrs, R.styleable.SpectrogramView, 0, 0).apply {
            val scale = resources.displayMetrics.scaledDensity

            isScaleXEnabled = isFlagSet(getInt(R.styleable.SpectrogramView_enableScaling, FLAG_SCALE_NONE), FLAG_SCALE_X_AXIS)
            isScaleYEnabled = isFlagSet(getInt(R.styleable.SpectrogramView_enableScaling, FLAG_SCALE_NONE), FLAG_SCALE_Y_AXIS)

            // Graph
            mGraphColor = getColor(R.styleable.SpectrogramView_graphColor, Color.WHITE)
            mGraphLineWidth = getDimensionPixelSize(R.styleable.SpectrogramView_graphWidth, 1)/scale

            // Background
            setDrawGridBackground(false)
            setBackgroundColor(getColor(R.styleable.SpectrogramView_bgColor, Color.WHITE))

            setViewPortOffsets(
                    getFloat(R.styleable.SpectrogramView_viewPortOffsetLeft,0f),
                    getFloat(R.styleable.SpectrogramView_viewPortOffsetTop,0f),
                    getFloat(R.styleable.SpectrogramView_viewPortOffsetRight,0f),
                    getFloat(R.styleable.SpectrogramView_viewPortOffsetBottom,0f))

            // Description
            description.isEnabled = hasValue(R.styleable.SpectrogramView_descr)
            if(description.isEnabled) {
                description.text = getString(R.styleable.SpectrogramView_descr)
                description.textColor = getColor(R.styleable.SpectrogramView_descrTextColor, Color.BLACK)
            }

            // Legend
            legend.isEnabled = getBoolean(R.styleable.SpectrogramView_showLegend, true)

            // Axis
            xAxis.setDrawGridLines(isFlagSet(getInt(R.styleable.SpectrogramView_drawGridLines, FLAG_DRAW_GRID_LINES_BOTH), FLAG_DRAW_GRID_LINES_X))
            xAxis.setDrawLabels(isFlagSet(getInt(R.styleable.SpectrogramView_drawLabels, FLAG_DRAW_LABELS_BOTH), FLAG_DRAW_LABELS_X))
            xAxis.setAvoidFirstLastClipping(true)
            xAxis.valueFormatter = LargeValueFormatter("Hz")
            xAxis.textColor = ContextCompat.getColor(context, R.color.colorTextOnPrimary)
            xAxis.axisMinimum = 0f

            axisLeft.setDrawGridLines(isFlagSet(getInt(R.styleable.SpectrogramView_drawGridLines, FLAG_DRAW_GRID_LINES_BOTH), FLAG_DRAW_GRID_LINES_Y))
            axisLeft.setDrawLabels(isFlagSet(getInt(R.styleable.SpectrogramView_drawLabels, FLAG_DRAW_LABELS_BOTH), FLAG_DRAW_LABELS_Y))
            axisLeft.valueFormatter = LargeValueFormatter()
            axisLeft.textColor = ContextCompat.getColor(context, R.color.colorTextOnPrimary)
            axisLeft.axisMinimum = 0f

            axisRight.isEnabled = false
        }
    }

    fun config(samplingRate:Int) {
        setTouchEnabled(true)
        isClickable = true
        isDragEnabled = false
        setPinchZoom(false)

        // X Axis
        xAxis.axisMaximum = samplingRate.toFloat()/2

        // Y Axis
        axisLeft.axisMaximum = 32768f*15
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
            } else {
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
        set.lineWidth = mGraphLineWidth
        set.color = mGraphColor
        set.isHighlightEnabled = false
        set.setDrawValues(false)
        set.setDrawCircles(false)
        set.mode = LineDataSet.Mode.CUBIC_BEZIER
        set.cubicIntensity = 1f
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

        inline fun isFlagSet(value:Int, flag:Int):Boolean {return value and flag != 0}
    }
}