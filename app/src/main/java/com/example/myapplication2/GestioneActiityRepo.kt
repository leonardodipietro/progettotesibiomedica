import android.content.Context
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs

open class OnSwipeTouchListener(ctx: Context) : View.OnTouchListener {

    private var downX: Float = 0f
    private var downY: Float = 0f
    private val SWIPE_THRESHOLD = 100

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x
                downY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val upX = event.x
                val upY = event.y

                val deltaX = upX - downX
                val deltaY = upY - downY

                if (abs(deltaX) > abs(deltaY)) {
                    // Swipe orizzontale
                    if (abs(deltaX) > SWIPE_THRESHOLD) {
                        if (deltaX > 0) {
                            onSwipeRight()
                        } else {
                            onSwipeLeft()
                        }
                    }
                    return true
                }
            }
        }
        return false
    }

    open fun onSwipeRight() {
        // Azione per lo swipe a destra
    }

    open fun onSwipeLeft() {
        // Azione per lo swipe a sinistra
    }
}