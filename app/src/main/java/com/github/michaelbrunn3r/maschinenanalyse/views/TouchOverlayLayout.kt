package com.github.michaelbrunn3r.maschinenanalyse.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.LinearLayout
import com.github.michaelbrunn3r.maschinenanalyse.R

class TouchOverlayLayout(context:Context, attrs:AttributeSet): LinearLayout(context, attrs) {

    private var mLastAction:Int? = -1
    private var mOnShortClickListener:(()->Unit)? = null
    private var mMaxShortClickDurationMs:Int = DEFAULT_MAX_SHORT_CLICK_DURATION

    init {
        context.theme.obtainStyledAttributes(attrs, R.styleable.TouchOverlayLayout, 0, 0).apply {
            mMaxShortClickDurationMs = getInt(R.styleable.TouchOverlayLayout_maxShortClickDurationMs, DEFAULT_MAX_SHORT_CLICK_DURATION)
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent?): Boolean {

        mLastAction = when(event?.action) {
            MotionEvent.ACTION_DOWN -> MotionEvent.ACTION_DOWN
            MotionEvent.ACTION_UP -> {
                if(mLastAction == MotionEvent.ACTION_DOWN) {
                    if(event.eventTime - event.downTime <= mMaxShortClickDurationMs) mOnShortClickListener?.invoke()
                }
                MotionEvent.ACTION_UP
            }
            else -> event?.action
        }

        return super.onInterceptTouchEvent(event)
    }

    fun setOnShortClickListener(l: () -> Unit) {
        mOnShortClickListener = l
    }

    companion object {
        const val DEFAULT_MAX_SHORT_CLICK_DURATION = 300
    }
}