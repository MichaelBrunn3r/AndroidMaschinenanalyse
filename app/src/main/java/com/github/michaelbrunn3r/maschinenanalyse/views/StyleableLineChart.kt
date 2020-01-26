package com.github.michaelbrunn3r.maschinenanalyse.views

import android.content.Context
import android.util.AttributeSet
import androidx.annotation.IntDef
import com.github.michaelbrunn3r.maschinenanalyse.R
import com.github.michaelbrunn3r.maschinenanalyse.util.getThemeAttr
import com.github.michaelbrunn3r.maschinenanalyse.util.isBitFlagSet
import com.github.michaelbrunn3r.maschinenanalyse.util.parseRange
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis


open class StyleableLineChart(context: Context, attrs: AttributeSet) : LineChart(context, attrs) {

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.StyleableLineChart, 0, 0).apply {
            // Axes
            xAxis.isEnabled = isBitFlagSet(getInt(R.styleable.StyleableLineChart_enabledAxes, EnabledAxes.DEFAULT), EnabledAxes.X)
            axisLeft.isEnabled = isBitFlagSet(getInt(R.styleable.StyleableLineChart_enabledAxes, EnabledAxes.DEFAULT), EnabledAxes.LEFT)
            axisRight.isEnabled = isBitFlagSet(getInt(R.styleable.StyleableLineChart_enabledAxes, EnabledAxes.DEFAULT), EnabledAxes.RIGHT)

            if (xAxis.isEnabled) xAxis.apply {
                setDrawLabels(isBitFlagSet(getInt(R.styleable.StyleableLineChart_showLabels, ShowLabels.DEFAULT), ShowLabels.X))
                setDrawGridLines(isBitFlagSet(getInt(R.styleable.StyleableLineChart_drawGridLines, ShowGrindlines.DEFAULT), ShowGrindlines.X))
                position = XAxis.XAxisPosition.values()[getInt(R.styleable.StyleableLineChart_xAxisPosition, XAxis.XAxisPosition.BOTTOM.ordinal)]
                setRange(getString(R.styleable.StyleableLineChart_xRange)?.parseRange()
                        ?: Pair(null, null))
                textColor = getColor(R.styleable.StyleableLineChart_textColor, context.getThemeAttr("textColor"))
            }
            if (axisLeft.isEnabled) axisLeft.apply {
                setDrawLabels(isBitFlagSet(getInt(R.styleable.StyleableLineChart_showLabels, ShowLabels.DEFAULT), ShowLabels.LEFT))
                setDrawGridLines(isBitFlagSet(getInt(R.styleable.StyleableLineChart_drawGridLines, ShowGrindlines.DEFAULT), ShowGrindlines.LEFT))
                setRange(getString(R.styleable.StyleableLineChart_leftRange)?.parseRange()
                        ?: Pair(null, null))
                textColor = getColor(R.styleable.StyleableLineChart_textColor, context.getThemeAttr("textColor"))
            }
            if (axisRight.isEnabled) axisRight.apply {
                setDrawLabels(isBitFlagSet(getInt(R.styleable.StyleableLineChart_showLabels, ShowLabels.DEFAULT), ShowLabels.RIGHT))
                setDrawGridLines(isBitFlagSet(getInt(R.styleable.StyleableLineChart_drawGridLines, ShowGrindlines.DEFAULT), ShowGrindlines.RIGHT))
                setRange(getString(R.styleable.StyleableLineChart_leftRange)?.parseRange()
                        ?: Pair(null, null))
                textColor = getColor(R.styleable.StyleableLineChart_textColor, context.getThemeAttr("textColor"))
            }

            // Behaviour
            isAutoScaleMinMaxEnabled = getBoolean(R.styleable.StyleableLineChart_autoScaleYAxis, false)

            // Scaling
            isScaleXEnabled = isBitFlagSet(getInt(R.styleable.StyleableLineChart_enableScaling, ScalingMode.DEFAULT), ScalingMode.X)
            isScaleYEnabled = isBitFlagSet(getInt(R.styleable.StyleableLineChart_enableScaling, ScalingMode.DEFAULT), ScalingMode.Y)

            // Draging
            isDragXEnabled = isBitFlagSet(getInt(R.styleable.StyleableLineChart_enableDragging, DraggingMode.DEFAULT), DraggingMode.X)
            isDragYEnabled = isBitFlagSet(getInt(R.styleable.StyleableLineChart_enableDragging, DraggingMode.DEFAULT), DraggingMode.Y)

            // Legend
            legend.isEnabled = getBoolean(R.styleable.StyleableLineChart_showLegend, false)

            // Description
            description.isEnabled = hasValue(R.styleable.StyleableLineChart_descr)
            if (description.isEnabled) description.apply {
                text = getString(R.styleable.StyleableLineChart_descr)
                textColor = getColor(R.styleable.StyleableLineChart_textColor, context.getThemeAttr("textColor"))
            }
        }

        if (isScaleXEnabled or isScaleYEnabled or isDragXEnabled or isDragYEnabled) this.setTouchEnabled(true)
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(flag = true, value = [EnabledAxes.NONE, EnabledAxes.X, EnabledAxes.LEFT, EnabledAxes.RIGHT, EnabledAxes.ALL, EnabledAxes.DEFAULT])
    annotation class EnabledAxes {
        companion object {
            const val NONE = 0
            const val X = 1
            const val LEFT = 2
            const val RIGHT = 4
            const val ALL = 7
            const val DEFAULT = X or LEFT
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(flag = true, value = [ScalingMode.NONE, ScalingMode.X, ScalingMode.Y, ScalingMode.BOTH])
    annotation class ScalingMode {
        companion object {
            const val NONE = 0
            const val X = 1
            const val Y = 2
            const val BOTH = X or Y
            const val DEFAULT = NONE
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(flag = true, value = [DraggingMode.NONE, DraggingMode.X, DraggingMode.Y, DraggingMode.BOTH])
    annotation class DraggingMode {
        companion object {
            const val NONE = 0
            const val X = 1
            const val Y = 2
            const val BOTH = X or Y
            const val DEFAULT = NONE
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(flag = true, value = [ShowGrindlines.NONE, ShowGrindlines.X, ShowGrindlines.LEFT, ShowGrindlines.ALL, ShowGrindlines.DEFAULT])
    annotation class ShowGrindlines {
        companion object {
            const val NONE = 0
            const val X = 1
            const val LEFT = 2
            const val RIGHT = 4
            const val ALL = X or LEFT or RIGHT
            const val DEFAULT = X or LEFT
        }
    }

    @Retention(AnnotationRetention.SOURCE)
    @IntDef(flag = true, value = [ShowLabels.NONE, ShowLabels.X, ShowLabels.LEFT, ShowLabels.RIGHT, ShowLabels.ALL, ShowLabels.DEFAULT])
    annotation class ShowLabels {
        companion object {
            const val NONE = 0
            const val X = 1
            const val LEFT = 2
            const val RIGHT = 4
            const val ALL = X or LEFT or RIGHT
            const val DEFAULT = X or LEFT
        }
    }
}

fun AxisBase.setRange(range: Pair<Int?, Int?>) {
    if (range.first != null) axisMinimum = range.first!!.toFloat()
    if (range.second != null) axisMaximum = range.second!!.toFloat()
}