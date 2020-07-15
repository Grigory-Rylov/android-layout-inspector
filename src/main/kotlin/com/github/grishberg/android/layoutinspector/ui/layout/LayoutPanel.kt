package com.github.grishberg.android.layoutinspector.ui.layout

import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import javax.swing.JPanel

private const val DEFAULT_SCALE = 0.25

class LayoutPanel(
    settings: SettingsFacade
) : JPanel() {
    private val logic = LayoutLogic(this, settings)
    private val zoomAndPanListener = ZoomAndPanListener(this)
    private val transformedPoint = Point()

    init {
        zoomAndPanListener.mouseEventsListener = object : ZoomAndPanListener.MouseEventsListener {
            override fun onMouseClicked(screenPoint: Point, tranformed: Point2D) {
                transformedPoint.setLocation(tranformed)
                logic.processMouseClicked(transformedPoint)
            }

            override fun onMouseMove(screenPoint: Point, tranformed: Point2D) {
                transformedPoint.setLocation(tranformed)
                logic.processMouseHover(transformedPoint)
            }

            override fun onMouseExited() {
                logic.onLayoutSelectedAction?.onMouseExited()
            }

            override fun onMouseShiftClicked(tranformed: Point2D) {
                transformedPoint.setLocation(tranformed)
                logic.processShiftMouseClicked(transformedPoint)
            }

            override fun onMouseRightClicked(tranformed: Point2D) {
                transformedPoint.setLocation(tranformed)
                logic.onMouseRightMove(tranformed)
            }

            override fun onMouseRightMoved(tranformed: Point2D) {
                transformedPoint.setLocation(tranformed)
                logic.onMouseRightMove(tranformed)
            }

            override fun onMouseUp() {
                logic.onMouseUp()
            }
        }
        zoomAndPanListener.setScale(DEFAULT_SCALE)
    }

    fun showLayoutResult(layoutData: LayoutFileData) {
        logic.showLayoutResult(layoutData)
        fitZoom()
        repaint()
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

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        if (ui != null) {
            val scratchGraphics = g.create() as Graphics2D
            try {
                ui.update(scratchGraphics, this)
                logic.draw(scratchGraphics, zoomAndPanListener.getCoordTransform())
            } finally {
                scratchGraphics!!.dispose()
            }
        }
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
}
