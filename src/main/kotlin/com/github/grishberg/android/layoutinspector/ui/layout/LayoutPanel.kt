package com.github.grishberg.android.layoutinspector.ui.layout

import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.Point
import java.awt.geom.Point2D
import javax.swing.JPanel


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

            override fun onMouseExited(){
                logic.onLayoutSelectedAction?.onMouseExited()
            }
        }
    }

    fun showLayoutResult(layoutData: LayoutFileData) {
        logic.showLayoutResult(layoutData)
        repaint()
    }

    fun selectNode(viewNode: ViewNode) {
        logic.selectNode(viewNode)
    }

    override fun getPreferredSize() = logic.getPreferredSize()

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)

        logic.draw((g.create() as Graphics2D), zoomAndPanListener.getCoordTransform())
    }

    fun setOnLayoutSelectedAction(action: LayoutLogic.OnLayoutSelectedAction) {
        logic.onLayoutSelectedAction = action
    }
}
