package com.github.grishberg.android.layoutinspector.ui.layout

import java.awt.Component
import java.awt.Point
import java.awt.event.*
import java.awt.event.InputEvent.CTRL_DOWN_MASK
import java.awt.event.InputEvent.META_DOWN_MASK
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

    fun setCoordinatesTransform(coordinatedTransform: AffineTransform) {
        this.coordTransform = coordinatedTransform
        try {
            val leftTop = transformPoint(leftTopPoint)
            coordinatedTransform.translate(leftTop.x.toDouble(), leftTop.y.toDouble())
        } catch (e: NoninvertibleTransformException) {
            e.printStackTrace()
        }
    }

    override fun mouseClicked(e: MouseEvent) = Unit
    override fun mousePressed(e: MouseEvent) {
        val point = e.point
        try {
            val transformedPoint = transformPoint(point)
            if (isCtrlPressed(e)) {
                mouseEventsListener?.onMouseCtrlClicked(transformedPoint)
                return
            }
            if (SwingUtilities.isRightMouseButton(e)) {
                mouseEventsListener?.onMouseRightClicked(transformedPoint)
                return
            } else if (e.isShiftDown) {
                mouseEventsListener?.onMouseShiftClicked(transformedPoint)
                return
            }
        } catch (ex: NoninvertibleTransformException) {
            ex.printStackTrace()
        }

        dragStartScreen = e.point
        clickStartScreen = dragStartScreen
        dragEndScreen = null
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
        val point = e.point
        try {
            val transformedPoint = transformPoint(point)
            if(e.isShiftDown){
                mouseEventsListener?.onMouseShiftMoved(point, transformedPoint)
                return
            }
            mouseEventsListener?.onMouseMove(point, transformedPoint)
        } catch (ex: NoninvertibleTransformException) {
            ex.printStackTrace()
        }
    }

    override fun mouseDragged(e: MouseEvent) {
        if (SwingUtilities.isRightMouseButton(e) || isCtrlPressed(e) || e.isShiftDown) {
            val point = e.point
            try {
                val transformedPoint = transformPoint(point)
                if (e.isShiftDown) {
                    mouseEventsListener?.onMouseShiftDragged(point, transformedPoint)
                    return
                }
                if (isCtrlPressed(e)) {
                    mouseEventsListener?.onMouseCtrlMoved(transformedPoint)
                    return
                }
                if (SwingUtilities.isRightMouseButton(e)) {
                    mouseEventsListener?.onMouseRightMoved(transformedPoint)
                    return
                }
            } catch (ex: NoninvertibleTransformException) {
                ex.printStackTrace()
            }
            return
        }
        moveCamera(e)
    }

    private fun isCtrlPressed(e: MouseEvent): Boolean {
        return e.modifiersEx and META_DOWN_MASK > 0 || e.modifiersEx and CTRL_DOWN_MASK > 0
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
        setCoordinatesTransform(coordTransform)
    }

    fun setScale(scale: Double) {
        coordTransform.scale(scale, scale)
    }

    interface MouseEventsListener {
        fun onMouseShiftClicked(transformed: Point2D)
        fun onMouseShiftMoved(screenPoint: Point, transformed: Point2D)
        fun onMouseShiftDragged(screenPoint: Point, transformed: Point2D)
        fun onMouseClicked(screenPoint: Point, transformed: Point2D)
        fun onMouseMove(screenPoint: Point, transformed: Point2D)
        fun onMouseExited()
        fun onMouseRightClicked(transformed: Point2D)
        fun onMouseRightMoved(transformed: Point2D)
        fun onMouseCtrlClicked(transformed: Point2D)
        fun onMouseCtrlMoved(transformed: Point2D) = Unit
        fun onMouseUp()
    }
}
