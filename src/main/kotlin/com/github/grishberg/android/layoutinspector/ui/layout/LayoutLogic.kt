package com.github.grishberg.android.layoutinspector.ui.layout

import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.swing.JPanel

class LayoutLogic(
    private val panel: JPanel
) {
    var onLayoutSelectedAction: OnLayoutSelectedAction? = null

    private var screenshot: BufferedImage? = null
    private var root: ViewNode? = null
    private val layoutModelRoots = mutableListOf<LayoutModel>()
    private val rectangles = mutableListOf<LayoutModel>()
    private val allRectangles = mutableMapOf<ViewNode, Shape>()
    private val borderColor = Color.GRAY

    private var selectedRectangle: Shape? = null
    private val selectedColor = Color(248, 25, 25)

    fun processMouseHover(point: Point) {
        val foundNode = getChildAtPoint(point)
        if (foundNode != null) {
            onLayoutSelectedAction?.onNodeHovered(foundNode.node)
        }
    }

    fun processMouseClicked(point: Point) {
        panel.requestFocus()
        val foundNode = getChildAtPoint(point)
        if (foundNode != null) {
            selectedRectangle = foundNode!!.rect
            onLayoutSelectedAction?.onNodeSelected(foundNode.node)
        }
    }

    fun showLayoutResult(layoutData: LayoutFileData) {
        root = layoutData.node
        screenshot = layoutData.bufferedImage
        rectangles.clear()
        layoutModelRoots.clear()

        addFromViewNode(layoutModelRoots, layoutData.node, 0, 0)
    }

    private fun addFromViewNode(
        parentsChildren: MutableList<LayoutModel>,
        node: ViewNode?,
        parentLeft: Int,
        parentTop: Int
    ) {
        if (node == null) {
            return
        }
        val left = node.displayInfo.left + parentLeft
        val top = node.displayInfo.top + parentTop
        val children = mutableListOf<LayoutModel>()
        val rect = Rectangle(left, top, node.displayInfo.width, node.displayInfo.height)
        val newLayoutModel = LayoutModel(rect, node, children)
        parentsChildren.add(newLayoutModel)
        if (node.isDrawn) {
            rectangles.add(newLayoutModel)
        }
        allRectangles[node] = rect
        if (node.isLeaf) {
            return
        }

        val count = node.childCount
        for (i in 0 until count) {
            addFromViewNode(children, node.getChildAt(i), left, top)
        }
    }

    private fun getChildAtPoint(point: Point): LayoutModel? {
        for (element in layoutModelRoots) {
            val itemByPos = findFirstElementByPosition(point, element)
            if (itemByPos != null) {
                return itemByPos
            }
        }
        return null
    }

    private fun findFirstElementByPosition(point: Point, parent: LayoutModel): LayoutModel? {
        val childCount = parent.children.size
        for (i in childCount - 1 downTo 0) {
            val child = parent.children[i]
            val rect = child.rect
            if (rect.contains(point) && child.node.isDrawn) {
                return findFirstElementByPosition(point, child)
            }
        }

        if (parent.rect.contains(point)) {
            return parent
        }
        return null
    }

    fun getPreferredSize(): Dimension {
        if (screenshot == null) {
            return Dimension(400, 640)
        }
        return Dimension(screenshot!!.width, screenshot!!.height)
    }

    fun draw(g: Graphics2D, at: AffineTransform) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        if (screenshot != null) {
            g.drawImage(screenshot, at, panel) // see javadoc for more info on the parameters
        }

        g.stroke = BasicStroke(1f)
        for (element in rectangles) {
            g.color = borderColor
            val transformedShape: Shape = at.createTransformedShape(element.rect)
            val bounds = transformedShape.bounds

            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)
        }

        selectedRectangle?.let {
            g.stroke = BasicStroke(5f)
            g.color = selectedColor
            val bounds = at.createTransformedShape(it).bounds
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)
        }
    }

    fun selectNode(viewNode: ViewNode) {
        selectedRectangle = allRectangles[viewNode]
    }

    interface OnLayoutSelectedAction {
        fun onNodeHovered(node: ViewNode)
        fun onNodeSelected(node: ViewNode)
        fun onMouseExited()
    }
}