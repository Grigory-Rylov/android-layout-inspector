package com.github.grishberg.android.layoutinspector.ui.layout

import com.android.layoutinspector.common.AppLogger
import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.domain.MetaRepository
import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import com.github.grishberg.android.layoutinspector.ui.common.SimpleComponentListener
import com.github.grishberg.android.layoutinspector.ui.screenshottest.ScreenshotPainter
import java.awt.Dimension
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.event.ComponentEvent
import java.awt.geom.NoninvertibleTransformException
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.swing.JPanel

private const val DEFAULT_SCALE = 0.25

class LayoutPanel(
    meta: MetaRepository,
    settings: SettingsFacade,
    layoutsState: LayoutsEnabledState,
    logger: AppLogger,
) : JPanel() {
    private val imageHelper = ImageHelper()
    private val clipboardManager = ScreenshotClipboardManager(imageHelper, logger)
    private val logic = LayoutLogic(this, meta, settings, layoutsState, imageHelper)
    private val zoomAndPanListener = ZoomAndPanListener(this)
    private val transformedPoint = Point()
    private var screenSize: Dimension = size
    private val screenTransformedRectangle = Rectangle2D.Double()

    val screenshot: BufferedImage?
        get() = logic.screenshot

    val screenshotPainter: ScreenshotPainter = logic

    init {
        zoomAndPanListener.mouseEventsListener = object : ZoomAndPanListener.MouseEventsListener {
            override fun onMouseClicked(screenPoint: Point, transformed: Point2D) {
                transformedPoint.setLocation(transformed)
                logic.processMouseClicked(transformedPoint)
            }

            override fun onMouseMove(screenPoint: Point, transformed: Point2D) {
                transformedPoint.setLocation(transformed)
                logic.processMouseHover(transformedPoint)
            }

            override fun onMouseExited() {
                logic.onLayoutSelectedAction?.onMouseExited()
            }

            override fun onMouseCtrlClicked(transformed: Point2D) {
                transformedPoint.setLocation(transformed)
                logic.selectElementAndMeasureIfNeeded(transformedPoint)
            }

            override fun onMouseShiftMoved(screenPoint: Point, transformed: Point2D) {
                logic.showPointer(transformed)
            }

            override fun onMouseShiftDragged(screenPoint: Point, transformed: Point2D) {
                transformedPoint.setLocation(transformed)
                logic.expandRulerAndShowSize(transformed)
            }

            override fun onMouseShiftClicked(transformed: Point2D) {
                transformedPoint.setLocation(transformed)
                logic.enableRuler(transformed)
            }

            override fun onMouseRightClicked(transformed: Point2D) {
                transformedPoint.setLocation(transformed)
                logic.onMouseRightMove(transformed)
            }

            override fun onMouseRightMoved(transformed: Point2D) {
                transformedPoint.setLocation(transformed)
                logic.onMouseRightMove(transformed)
            }

            override fun onMouseUp() {
                logic.onMouseUp()
            }
        }
        zoomAndPanListener.setScale(DEFAULT_SCALE)

        addComponentListener(object : SimpleComponentListener() {
            override fun componentResized(e: ComponentEvent) {
                screenSize = this@LayoutPanel.size
            }
        })
    }

    fun showLayoutResult(layoutData: LayoutFileData) {
        logic.showLayoutResult(layoutData)
        fitZoom()
        repaint()
        invalidate()
    }

    fun selectNode(viewNode: ViewNode) {
        logic.selectNode(viewNode)
        repaint()
    }

    fun hoverNode(viewNode: ViewNode) {
        logic.hoverNode(viewNode)
        repaint()
    }

    fun removeNodeHover() {
        if (logic.removeNodeHover()) {
            repaint()
        }
    }

    fun calculateDistanceBetweenTwoViewNodes(startViewNode: ViewNode, endViewNode: ViewNode) {
        logic.calculateDistanceBetweenTwoViewNodes(startViewNode, endViewNode)
    }

    override fun getPreferredSize() = logic.getPreferredSize()

    override fun paintComponent(graphics: Graphics) {
        super.paintComponent(graphics)

        val g = graphics as Graphics2D
        try {
            val leftTop = zoomAndPanListener.transformPoint(Point(0, 0))
            val rightBottom = zoomAndPanListener.transformPoint(Point(screenSize.width, screenSize.height))
            screenTransformedRectangle.setRect(
                leftTop.x.toDouble(),
                leftTop.y.toDouble(),
                rightBottom.x.toDouble(),
                rightBottom.y.toDouble()
            )
        } catch (e: NoninvertibleTransformException) {
            e.printStackTrace()
        }
        logic.draw(g, zoomAndPanListener.getCoordTransform(), screenTransformedRectangle)
    }

    fun setOnLayoutSelectedAction(action: LayoutLogic.OnLayoutSelectedAction) {
        logic.onLayoutSelectedAction = action
    }

    fun fitZoom() {
        val imageSize = logic.imageSize
        if (imageSize.width == 0) {
            return
        }
        val rect = Rectangle2D.Double(0.0, 0.0, imageSize.width.toDouble(), imageSize.height.toDouble())
        zoomAndPanListener.fitZoom(rect, 0, false)
        repaint()
    }

    fun resetZoom() {
        zoomAndPanListener.resetZoom()
        repaint()
    }

    fun setSizeDpMode(sizeInDpEnabled: Boolean) {
        logic.setSizeDpMode(sizeInDpEnabled)
    }


    fun roundDp() {
        logic.roundDp()
    }

    fun hasSelection() = logic.hasSelection()

    fun removeSelection() {
        logic.removeSelection()
        repaint()
    }

    fun copyScreenshotToClipboard() {
        clipboardManager.copyToClipboard(screenshot)
    }
}
