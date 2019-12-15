package com.github.michaelbrunn3r.maschinenanalyse

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
        setDrawGridBackground(false)
        setTouchEnabled(true)
        isClickable = true
        isDragEnabled = false
        setPinchZoom(false)

        xAxis.textColor = ContextCompat.getColor(context, R.color.colorTextOnPrimary)
        xAxis.valueFormatter = LargeValueFormatter("Hz")
        setFrequencyRange(0f, 4096.toFloat()/2) // Default Value

        axisLeft.axisMinimum = 0f
        axisLeft.axisMaximum = 300f
        axisLeft.textColor = ContextCompat.getColor(context, R.color.colorTextOnPrimary)
        axisLeft.valueFormatter = LargeValueFormatter()

        axisRight.isEnabled = false

        context.theme.obtainStyledAttributes(attrs, R.styleable.SpectrogramView, 0, 0).apply {
            val scale = resources.displayMetrics.scaledDensity

            // Interaction
            isScaleXEnabled = isFlagSet(getInt(R.styleable.SpectrogramView_enableScaling, FLAG_SCALE_NONE), FLAG_SCALE_X_AXIS)
            isScaleYEnabled = isFlagSet(getInt(R.styleable.SpectrogramView_enableScaling, FLAG_SCALE_NONE), FLAG_SCALE_Y_AXIS)
            isDragXEnabled = isFlagSet(getInt(R.styleable.SpectrogramView_enableDragging, FLAG_DRAG_NONE), FLAG_DRAG_X)
            isDragYEnabled = isFlagSet(getInt(R.styleable.SpectrogramView_enableDragging, FLAG_DRAG_NONE), FLAG_DRAG_Y)

            // Graph
            mGraphColor = getColor(R.styleable.SpectrogramView_graphColor, Color.WHITE)
            mGraphLineWidth = getDimensionPixelSize(R.styleable.SpectrogramView_graphWidth, 1)/scale

            // Background
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

            axisLeft.setDrawGridLines(isFlagSet(getInt(R.styleable.SpectrogramView_drawGridLines, FLAG_DRAW_GRID_LINES_BOTH), FLAG_DRAW_GRID_LINES_Y))
            axisLeft.setDrawLabels(isFlagSet(getInt(R.styleable.SpectrogramView_drawLabels, FLAG_DRAW_LABELS_BOTH), FLAG_DRAW_LABELS_Y))
        }
    }

    fun setFrequencyRange(min:Float, max:Float) {
        xAxis.axisMinimum = min
        xAxis.axisMaximum = max
        data.clearValues()
        invalidate()
    }

    @Synchronized
    fun update(magnitudes:FloatArray, frequenzyForIndex: (index:Int) -> Float) {
        if(data != null) {
            var set: ILineDataSet? = data.getDataSetByIndex(0)
            if(set == null) {
                set = createSet()
                data.addDataSet(set)
            }

            if(set.entryCount != magnitudes.size) {
                set.clear()
                for(i in magnitudes.indices) {
                    data.addEntry(Entry(frequenzyForIndex(i), magnitudes[i]), 0)
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