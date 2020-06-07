package com.github.grishberg.android.layoutinspector.ui.layout

import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.geom.Point2D
import javax.swing.JPanel

private const val DEFAULT_SCALE = 0.25

class LayoutPanel : JPanel() {
    private val logic = LayoutLogic(this)
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
        }
        zoomAndPanListener.setScale(DEFAULT_SCALE)
    }

    fun showLayoutResult(layoutData: LayoutFileData) {
        logic.showLayoutResult(layoutData)
        repaint()
    }

    fun selectNode(viewNode: ViewNode) {
        logic.selectNode(viewNode)
        repaint()
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

    fun resetZoom() {
        zoomAndPanListener.resetZoom()
        zoomAndPanListener.setScale(0.25)
        repaint()
    }
}
