package com.github.michaelbrunn3r.maschinenanalyse.views

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import com.github.michaelbrunn3r.maschinenanalyse.R
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet

class RealtimeLinearView(context: Context, attrs: AttributeSet) : StyleableLineChart(context, attrs) {

    private var mGraphColor = Color.BLACK
    private var mGraphLineWidth: Float = 1f

    init {
        data = LineData()
        setHardwareAccelerationEnabled(true)
        isClickable = true

        context.theme.obtainStyledAttributes(attrs, R.styleable.Graph, 0, 0).apply {
            val scale = resources.displayMetrics.scaledDensity

            // Graph
            mGraphColor = getColor(R.styleable.Graph_graphColor, Color.WHITE)
            mGraphLineWidth = getDimensionPixelSize(R.styleable.Graph_graphWidth, 1) / scale
        }

        data.addDataSet(createSet())
        for (i in 0 until 500) {
            data.addEntry(Entry(i.toFloat(), 0f), 0)
        }
        invalidate()
    }

    @Synchronized
    fun update(value: Float) {
        if (data != null) {
            val set: ILineDataSet = data.getDataSetByIndex(0)

            for (i in 0 until set.entryCount - 1) {
                set.getEntryForIndex(i).y = set.getEntryForIndex(i + 1).y
            }
            set.getEntryForIndex(set.entryCount - 1).y = value

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