package com.github.michaelbrunn3r.maschinenanalyse.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.github.michaelbrunn3r.maschinenanalyse.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

class RealtimeLinearView(context: Context, attrs: AttributeSet): LineChart(context, attrs) {

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

        isAutoScaleMinMaxEnabled = true

        xAxis.textColor = ContextCompat.getColor(context, R.color.nobel)
        xAxis.setDrawLabels(false)

        axisLeft.axisMinimum = 0f
        axisLeft.textColor = ContextCompat.getColor(context, R.color.nobel)

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

            axisLeft.setDrawGridLines(isFlagSet(getInt(R.styleable.MPAndroidChart_drawGridLines, FLAG_DRAW_GRID_LINES_BOTH), FLAG_DRAW_GRID_LINES_Y))
            axisLeft.setDrawLabels(isFlagSet(getInt(R.styleable.MPAndroidChart_drawLabels, FLAG_DRAW_LABELS_BOTH), FLAG_DRAW_LABELS_Y))
        }

        data.addDataSet(createSet())
        for(i in 0 until 500) {
            data.addEntry(Entry(i.toFloat(), 0f), 0)
        }
        invalidate()
    }

    @Synchronized
    fun update(value:Float) {
        if(data != null) {
            val set: ILineDataSet = data.getDataSetByIndex(0)

            for(i in 0 until set.entryCount-1) {
                set.getEntryForIndex(i).y = set.getEntryForIndex(i+1).y
            }
            set.getEntryForIndex(set.entryCount -1).y = value

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