package com.github.grishberg.android.layoutinspector.ui.layout

import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.lang.IllegalStateException
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.math.min

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
    private var measureRectangle: Shape? = null
    private val selectedColor = Color(248, 25, 25)
    private val measureColor = Color(248, 225, 25)

    fun processMouseHover(point: Point) {
        val foundNode = getChildAtPoint(point)
        if (foundNode != null) {
            onLayoutSelectedAction?.onNodeHovered(foundNode.node)
        }
    }

    fun processMouseClicked(point: Point) {
        measureRectangle = null
        panel.requestFocus()
        val foundNode = getChildAtPoint(point)
        if (foundNode != null) {
            selectedRectangle = foundNode.rect
            onLayoutSelectedAction?.onNodeSelected(foundNode.node)
        }
    }

    fun processShiftMouseClicked(point: Point) {
        panel.requestFocus()
        val foundNode = getChildAtPoint(point)
        if (foundNode != null) {
            if (selectedRectangle == null) {
                selectedRectangle = foundNode.rect
                onLayoutSelectedAction?.onNodeSelected(foundNode.node)
            } else {
                measureRectangle = foundNode.rect
                calculateDistance(foundNode.rect)
            }
            panel.repaint()
        }
    }

    private fun calculateDistance(measureRectangle: Shape) {
        val selected = selectedRectangle?: throw IllegalStateException("No selected element to measure distance")
        val selectedBounds = selected.bounds2D
        val selectedX1 = selectedBounds.x
        val selectedX2 = selectedBounds.x + selectedBounds.width
        val selectedY1 = selectedBounds.y
        val selectedY2 = selectedBounds.y + selectedBounds.height

        val targetBounds = measureRectangle.bounds2D
        val targetX1 = targetBounds.x
        val targetX2 = targetBounds.x + targetBounds.width
        val targetY1 = targetBounds.y
        val targetY2 = targetBounds.y + targetBounds.height

        val dx1 = min(abs(selectedX1 - targetX1), abs(selectedX1 - targetX2))
        val dx2 = min(abs(selectedX2 - targetX1), abs(selectedX2 - targetX2))

        val dy1 = min(abs(selectedY1 - targetY1), abs(selectedY1 - targetY2))
        val dy2 = min(abs(selectedY2 - targetY1), abs(selectedY2 - targetY2))

        val shortDistance = Dimension(min(dx1, dx2).toInt(), min(dy1, dy2).toInt())
        onLayoutSelectedAction?.onDistanceCalculated(shortDistance)
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
            g.stroke = BasicStroke(3f)
            g.color = selectedColor
            val bounds = at.createTransformedShape(it).bounds
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)
        }

        measureRectangle?.let {
            g.stroke = BasicStroke(3f)
            g.color = measureColor
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
        fun onDistanceCalculated(dimension: Dimension)
    }
}