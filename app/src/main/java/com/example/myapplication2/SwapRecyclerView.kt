package com.example.myapplication2

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class SwapRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {

    private var isSwipeEnabled = true
    private var initialX = 0f
    private var initialY = 0f
    private val swipeThreshold = 50 // Soglia per differenziare tra swipe orizzontale e scroll verticale

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                initialX = e.x
                initialY = e.y
            }
            MotionEvent.ACTION_MOVE -> {
                val deltaX = e.x - initialX
                val deltaY = e.y - initialY

                if (isSwipeEnabled && Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > swipeThreshold) {

                    return false
                }
            }
        }

        return super.onInterceptTouchEvent(e)
    }

    fun setSwipeEnabled(enabled: Boolean) {
        isSwipeEnabled = enabled
    }
}