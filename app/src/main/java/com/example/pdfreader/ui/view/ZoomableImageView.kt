package com.example.pdfreader.ui.view

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import androidx.appcompat.widget.AppCompatImageView

class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr), View.OnTouchListener,
    ScaleGestureDetector.OnScaleGestureListener {

    private val matrixValues = FloatArray(9)
    private var currMatrix = Matrix()
    private var mode = NONE

    private var maxScale = 4.0f
    private var minScale = 1.0f

    private val lastPoint = PointF()
    private val startPoint = PointF()

    private var scaleDetector = ScaleGestureDetector(context, this)
    private var gestureDetector = GestureDetector(context, GestureListener())

    companion object {
        private const val NONE = 0
        private const val DRAG = 1
        private const val ZOOM = 2
        private const val CLICK_THRESHOLD = 5
    }

    init {
        super.setOnTouchListener(this)
        scaleType = ScaleType.MATRIX
    }

    override fun setImageBitmap(bm: android.graphics.Bitmap?) {
        super.setImageBitmap(bm)
        resetZoom()
    }

    fun resetZoom() {
        currMatrix.reset()
        imageMatrix = currMatrix
        mode = NONE
        invalidate()
    }

    override fun onTouch(v: View, event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        val currentPoint = PointF(event.x, event.y)
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastPoint.set(currentPoint)
                startPoint.set(lastPoint)
                mode = DRAG
            }
            MotionEvent.ACTION_MOVE -> {
                if (mode == DRAG) {
                    val deltaX = currentPoint.x - lastPoint.x
                    val deltaY = currentPoint.y - lastPoint.y
                    
                    val scale = getScale()
                    if (scale > minScale) {
                        // Request parent (ViewPager2) not to intercept touches when zoomed
                        parent?.requestDisallowInterceptTouchEvent(true)
                        
                        val rect = getDrawableRect()
                        val viewWidth = width.toFloat()
                        val viewHeight = height.toFloat()

                        var translationX = deltaX
                        var translationY = deltaY

                        // Clamp X drag bounds
                        if (rect.width() <= viewWidth) {
                            translationX = 0f
                        } else {
                            val nextLeft = rect.left + deltaX
                            val nextRight = rect.right + deltaX
                            if (nextLeft > 0) translationX = -rect.left
                            if (nextRight < viewWidth) translationX = viewWidth - rect.right
                            
                            // If we hit the horizontal boundary, allow parent ViewPager to swipe
                            if (nextLeft >= 0 || nextRight <= viewWidth) {
                                parent?.requestDisallowInterceptTouchEvent(false)
                            }
                        }

                        // Clamp Y drag bounds
                        if (rect.height() <= viewHeight) {
                            translationY = 0f
                        } else {
                            val nextTop = rect.top + deltaY
                            val nextBottom = rect.bottom + deltaY
                            if (nextTop > 0) translationY = -rect.top
                            if (nextBottom < viewHeight) translationY = viewHeight - rect.bottom
                        }

                        currMatrix.postTranslate(translationX, translationY)
                        imageMatrix = currMatrix
                    } else {
                        parent?.requestDisallowInterceptTouchEvent(false)
                    }
                    lastPoint.set(currentPoint)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                mode = NONE
                val scale = getScale()
                if (scale <= minScale) {
                    resetZoom()
                }
            }
        }
        return true
    }

    override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
        mode = ZOOM
        parent?.requestDisallowInterceptTouchEvent(true)
        return true
    }

    override fun onScale(detector: ScaleGestureDetector): Boolean {
        var scaleFactor = detector.scaleFactor
        val origScale = getScale()
        var targetScale = origScale * scaleFactor

        if (targetScale > maxScale) {
            scaleFactor = maxScale / origScale
            targetScale = maxScale
        } else if (targetScale < minScale) {
            scaleFactor = minScale / origScale
            targetScale = minScale
        }

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        
        currMatrix.postScale(scaleFactor, scaleFactor, detector.focusX, detector.focusY)
        
        // Adjust translation after scale to keep it bounded
        val rect = getDrawableRect()
        var translationX = 0f
        var translationY = 0f

        if (rect.width() > viewWidth) {
            if (rect.left > 0) translationX = -rect.left
            if (rect.right < viewWidth) translationX = viewWidth - rect.right
        } else {
            translationX = viewWidth / 2f - (rect.left + rect.right) / 2f
        }

        if (rect.height() > viewHeight) {
            if (rect.top > 0) translationY = -rect.top
            if (rect.bottom < viewHeight) translationY = viewHeight - rect.bottom
        } else {
            translationY = viewHeight / 2f - (rect.top + rect.bottom) / 2f
        }

        currMatrix.postTranslate(translationX, translationY)
        imageMatrix = currMatrix
        return true
    }

    override fun onScaleEnd(detector: ScaleGestureDetector) {
        mode = NONE
        val scale = getScale()
        if (scale <= minScale) {
            resetZoom()
        }
    }

    private fun getScale(): Float {
        currMatrix.getValues(matrixValues)
        return matrixValues[Matrix.MSCALE_X]
    }

    private fun getDrawableRect(): RectF {
        val rect = RectF()
        drawable?.let {
            rect.set(0f, 0f, it.intrinsicWidth.toFloat(), it.intrinsicHeight.toFloat())
            currMatrix.mapRect(rect)
        }
        return rect
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val scale = getScale()
            val targetScale = if (scale > minScale) minScale else maxScale
            
            // Instantly zoom to target
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()
            
            if (targetScale == minScale) {
                resetZoom()
            } else {
                val scaleFactor = targetScale / scale
                currMatrix.postScale(scaleFactor, scaleFactor, e.x, e.y)
                
                // Adjust boundaries
                val rect = getDrawableRect()
                var translationX = 0f
                var translationY = 0f
                
                if (rect.width() > viewWidth) {
                    if (rect.left > 0) translationX = -rect.left
                    if (rect.right < viewWidth) translationX = viewWidth - rect.right
                }
                if (rect.height() > viewHeight) {
                    if (rect.top > 0) translationY = -rect.top
                    if (rect.bottom < viewHeight) translationY = viewHeight - rect.bottom
                }
                
                currMatrix.postTranslate(translationX, translationY)
                imageMatrix = currMatrix
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            invalidate()
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            // Forward tap to parent container to toggle overlay controls
            performClick()
            return true
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }
}
