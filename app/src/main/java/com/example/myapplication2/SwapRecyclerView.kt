package com.example.myapplication2

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView

class SwapRecyclerView (context: Context, attrs: AttributeSet) : RecyclerView(context, attrs) {

    private var isSwipeEnabled = true

    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        // Se lo swipe è abilitato, evita che la RecyclerView intercetti l'evento
        return if (isSwipeEnabled) false else super.onInterceptTouchEvent(e)
    }

    fun setSwipeEnabled(enabled: Boolean) {
        isSwipeEnabled = enabled
    }


}