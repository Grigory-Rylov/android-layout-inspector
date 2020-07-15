package com.github.grishberg.android.layoutinspector.ui.layout

import java.awt.Component
import java.awt.Point
import java.awt.event.*
import java.awt.geom.AffineTransform
import java.awt.geom.NoninvertibleTransformException
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import javax.swing.SwingUtilities

private const val MOUSE_GAP = 2
private const val KEYBOARD_ZOOM_MULTIPLICATION_FACTOR = 1.5
private const val ZOOM_FACTOR = 0.1

class ZoomAndPanListener(
    private val targetComponent: Component,
    topOffset: Int = 0
) : MouseListener,
    MouseMotionListener, MouseWheelListener {
    private var dragStartScreen = Point()
    private var dragEndScreen: Point? = null
    private var clickStartScreen = Point()
    private var coordTransform = AffineTransform()
    private val leftTopPoint: Point
    var mouseEventsListener: MouseEventsListener? = null

    init {
        leftTopPoint = Point(0, topOffset)
        targetComponent.addMouseListener(this)
        targetComponent.addMouseMotionListener(this)
        targetComponent.addMouseWheelListener(this)
    }

    fun setCoordTransform(coordTransform: AffineTransform) {
        this.coordTransform = coordTransform
        try {
            val leftTop = transformPoint(leftTopPoint)
            coordTransform.translate(leftTop.x.toDouble(), leftTop.y.toDouble())
        } catch (e: NoninvertibleTransformException) {
            e.printStackTrace()
        }
    }

    override fun mouseClicked(e: MouseEvent) = Unit
    override fun mousePressed(e: MouseEvent) {
        dragStartScreen = e.point
        clickStartScreen = dragStartScreen
        dragEndScreen = null
        if (e.isShiftDown) {
            val point = e.point
            try {
                val transformedPoint = transformPoint(point)
                if (SwingUtilities.isRightMouseButton(e)) {
                    mouseEventsListener?.onMouseRightClicked(transformedPoint)
                } else {
                    mouseEventsListener?.onMouseShiftClicked(transformedPoint)
                }
            } catch (ex: NoninvertibleTransformException) {
                ex.printStackTrace()
            }
            return
        }
    }

    override fun mouseReleased(e: MouseEvent) {
        if (e.isShiftDown) {
            return
        }
        val dx = Math.abs(e.x - clickStartScreen.x)
        val dy = Math.abs(e.y - clickStartScreen.y)
        clickStartScreen = e.point
        if (MOUSE_GAP > Math.max(dx, dy)) {
            val point = e.point
            try {
                val transformedPoint = transformPoint(point)
                if (SwingUtilities.isRightMouseButton(e)) {
                    mouseEventsListener?.onMouseRightClicked(transformedPoint)
                } else {
                    mouseEventsListener?.onMouseClicked(point, transformedPoint)
                }
                mouseEventsListener?.onMouseUp()
            } catch (ex: NoninvertibleTransformException) {
                ex.printStackTrace()
            }
        }
        targetComponent.repaint()
    }

    override fun mouseEntered(e: MouseEvent) = Unit

    override fun mouseExited(e: MouseEvent) {
        mouseEventsListener?.onMouseExited()
    }

    override fun mouseMoved(e: MouseEvent) {
        if (e.isShiftDown) {
            return
        }

        val point = e.point
        try {
            val transformedPoint = transformPoint(point)
            if (SwingUtilities.isRightMouseButton(e)) {
                mouseEventsListener?.onMouseRightMoved(transformedPoint)
            } else {
                mouseEventsListener?.onMouseMove(point, transformedPoint)
            }
        } catch (ex: NoninvertibleTransformException) {
            ex.printStackTrace()
        }
    }

    override fun mouseDragged(e: MouseEvent) {
        if (e.isShiftDown) {
            return
        }

        if (SwingUtilities.isRightMouseButton(e)) {
            mouseMoved(e)
            return
        }
        moveCamera(e)
    }

    private fun moveCamera(e: MouseEvent) {
        try {
            val leftTop = transformPoint(leftTopPoint)
            dragEndScreen = e.point
            val dragStart = transformPoint(dragStartScreen)
            val dragEnd = transformPoint(dragEndScreen!!)
            var dx = dragEnd.getX() - dragStart.getX()
            var dy = dragEnd.getY() - dragStart.getY()
            if (dx > 0) { // pane right
                dx = Math.min(dx, leftTop.x.toDouble())
            }
            if (dy > 0) { // pane down
                dy = Math.min(dy, leftTop.y.toDouble())
            }
            coordTransform.translate(dx, dy)
            dragStartScreen = dragEndScreen!!
            dragEndScreen = null
            targetComponent.repaint()
        } catch (ex: NoninvertibleTransformException) {
            ex.printStackTrace()
        }
    }

    override fun mouseWheelMoved(e: MouseWheelEvent) {
        zoomCamera(e)
    }

    private fun zoomCamera(e: MouseWheelEvent) {
        try {
            val wheelRotation = e.preciseWheelRotation
            val p = e.point
            if (wheelRotation > 0.0f) {
                zoomIn(p, 1 + Math.abs(wheelRotation) * ZOOM_FACTOR)
            } else {
                zoomOut(p, 1 + Math.abs(wheelRotation) * ZOOM_FACTOR)
            }
        } catch (ex: NoninvertibleTransformException) {
            ex.printStackTrace()
        }
    }

    @Throws(NoninvertibleTransformException::class)
    private fun zoomOut(p: Point, zoomMultiplicationFactor: Double) {
        val p1: Point2D = transformPoint(p)
        coordTransform.scale(zoomMultiplicationFactor, zoomMultiplicationFactor)
        val p2: Point2D = transformPoint(p)
        coordTransform.translate(p2.x - p1.x, p2.y - p1.y)
        targetComponent.repaint()
    }

    @Throws(NoninvertibleTransformException::class)
    private fun zoomIn(p: Point, zoomMultiplicationFactor: Double) {
        val p1: Point2D = transformPoint(p)
        coordTransform.scale(1 / zoomMultiplicationFactor, 1 / zoomMultiplicationFactor)
        val p2: Point2D = transformPoint(p)
        val leftTop = transformPoint(leftTopPoint)
        var dx = p2.x - p1.x
        var dy = p2.y - p1.y
        if (dx > 0) { // pane right
            dx = Math.min(dx, leftTop.x.toDouble())
        }
        if (dy > 0) { // pane down
            dy = Math.min(dy, leftTop.y.toDouble())
        }
        coordTransform.translate(dx, dy)
        targetComponent.repaint()
    }

    /**
     * Converts screen coordinates [p1] into world coordinates.
     */
    @Throws(NoninvertibleTransformException::class)
    fun transformPoint(p1: Point): Point2D.Float {
        val inverse = coordTransform.createInverse()
        val p2 = Point2D.Float()
        inverse.transform(p1, p2)
        return p2
    }

    @Throws(NoninvertibleTransformException::class)
    fun transformPoint(p1: Point2D.Double?): Point2D.Float {
        val inverse = coordTransform.createInverse()
        val p2 = Point2D.Float()
        inverse.transform(p1, p2)
        return p2
    }

    fun getScaleX(x: Double): Double {
        return x * coordTransform.scaleX
    }

    fun getCoordTransform(): AffineTransform {
        return coordTransform
    }

    fun zoomIn() {
        val center = Point(targetComponent.width / 2, targetComponent.height / 2)
        try {
            zoomIn(center, KEYBOARD_ZOOM_MULTIPLICATION_FACTOR)
        } catch (ex: NoninvertibleTransformException) {
            ex.printStackTrace()
        }
    }

    private fun moveCameraHorizontal(dx: Int) {
        try {
            val dragEndScreen = Point(dx, 0)
            val dragStart = transformPoint(leftTopPoint)
            val dragEnd = transformPoint(dragEndScreen)
            var tdx = dragEnd.getX() - dragStart.getX()
            val leftTop = transformPoint(leftTopPoint)
            if (tdx > 0) { // pane right
                tdx = Math.min(tdx, leftTop.x.toDouble())
            }
            coordTransform.translate(tdx, 0.0)
            targetComponent.repaint()
        } catch (ex: NoninvertibleTransformException) {
            ex.printStackTrace()
        }
    }

    private fun moveCameraVertical(dy: Int) {
        try {
            val targetPoint = Point(0, dy)
            val currentPoint = transformPoint(Point(0, 0))
            val transformedTargetPoint = transformPoint(targetPoint)
            var tdy = transformedTargetPoint.getY() - currentPoint.getY()
            val leftTopWithOffset = transformPoint(leftTopPoint)
            if (tdy > 0) { // pane down
                tdy = Math.min(tdy, leftTopWithOffset.y.toDouble())
            }
            coordTransform.translate(0.0, tdy)
            targetComponent.repaint()
        } catch (ex: NoninvertibleTransformException) {
            ex.printStackTrace()
        }
    }

    /**
     * Navigate to current [bounds].
     */
    fun navigateToRectangle(bounds: Rectangle2D, ignoreVerticalOffset: Boolean) {
        try {
            val targetElementCenter =
                Point2D.Double(bounds.centerX, bounds.centerY)
            val screenCenter =
                Point2D.Double(targetComponent.width / 2.0, targetComponent.height / 2.0)
            val mappedToWorldScreenCenter = transformPoint(screenCenter)
            var tdx = mappedToWorldScreenCenter.getX() - targetElementCenter.getX()
            var tdy = mappedToWorldScreenCenter.getY() - targetElementCenter.getY()
            val leftTop = transformPoint(leftTopPoint)
            if (tdx > 0) { // pane right
                tdx = Math.min(tdx, leftTop.x.toDouble())
            }
            if (tdy > 0) { // pane down
                tdy = Math.min(tdy, leftTop.y.toDouble())
            }
            coordTransform.translate(tdx, if (ignoreVerticalOffset) 0.0 else tdy)
            targetComponent.repaint()
        } catch (ex: NoninvertibleTransformException) {
            ex.printStackTrace()
        }
    }

    /**
     * fit current [bounds] on screen.
     */
    fun fitZoom(
        bounds: Rectangle2D.Double,
        sidePadding: Int,
        ignoreVerticalOffset: Boolean
    ) {
        val screenTransformedWidth =
            (targetComponent.width - 2 * sidePadding).toDouble() / coordTransform.scaleX
        val scaleFactor = screenTransformedWidth / bounds.width
        coordTransform.scale(scaleFactor, scaleFactor)
        navigateToRectangle(bounds, ignoreVerticalOffset)
    }

    fun resetZoom() {
        coordTransform = AffineTransform()
        setCoordTransform(coordTransform)
    }

    fun setScale(scale: Double) {
        coordTransform.scale(scale, scale)
    }

    interface MouseEventsListener {
        fun onMouseShiftClicked(tranformed: Point2D)
        fun onMouseClicked(screenPoint: Point, tranformed: Point2D)
        fun onMouseMove(screenPoint: Point, tranformed: Point2D)
        fun onMouseExited()
        fun onMouseRightClicked(tranformed: Point2D)
        fun onMouseRightMoved(tranformed: Point2D)
        fun onMouseUp()
    }
}