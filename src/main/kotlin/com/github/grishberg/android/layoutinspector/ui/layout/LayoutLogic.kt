package com.github.grishberg.android.layoutinspector.ui.layout

import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round


class LayoutLogic(
    private val panel: JPanel,
    private val settings: SettingsFacade
) {
    private val GFX_CONFIG = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration

    var onLayoutSelectedAction: OnLayoutSelectedAction? = null

    private var screenshot: BufferedImage? = null
    val imageSize: Dimension
        get() {
            val image = screenshot
            if (image != null) {
                return Dimension(image.width, image.height)
            }
            return Dimension(0, 0)
        }
    private var root: ViewNode? = null
    private val layoutModelRoots = mutableListOf<LayoutModel>()
    private val rectangles = mutableListOf<LayoutModel>()
    private val allRectangles = mutableMapOf<ViewNode, Shape>()
    private val borderColor = Color.GRAY

    private var selectedRectangle: Shape? = null
    private var measureRectangle: Shape? = null
    private var hoveredRectangle: Shape? = null
    private var selectionRectangle: Shape? = null
    private var rulerRectangle: Rectangle2D.Double? = null
    private val rulerFirstPoint = Point2D.Double()

    private val measureLines = mutableListOf<Shape>()
    private val selectedColor = Color(41, 105, 248)
    private val hoverColor = Color(248, 25, 25)
    private val measureColor = Color(248, 225, 25)
    private val rulerColor = Color(4, 246, 187, 128)
    private val rulerFillColor = Color(77, 246, 206, 128)
    private val measureLineColor = selectedColor
    private val measureLineStroke: Stroke =
        BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, floatArrayOf(9f), 0f)

    private val distances = DistanceBetweenTwoShape()
    private var recalculateDistanceAction: CalculateDistanceAction? = null

    fun processMouseHover(point: Point) {
        val foundNode = getChildAtPoint(point)
        if (foundNode != null) {
            onLayoutSelectedAction?.onNodeHovered(foundNode.node)
        }
    }

    fun processMouseClicked(point: Point) {
        rulerRectangle = null
        measureRectangle = null
        measureLines.clear()
        panel.requestFocus()
        val foundNode = getChildAtPoint(point)
        if (foundNode != null) {
            recalculateDistanceAction = null
            selectedRectangle = foundNode.rect
            onLayoutSelectedAction?.onNodeSelected(foundNode.node)
        }
    }

    fun selectElementAndMeasureIfNeede(point: Point) {
        panel.requestFocus()
        val foundNode = getChildAtPoint(point)
        if (foundNode != null) {
            if (selectedRectangle == null) {
                selectedRectangle = foundNode.rect
                onLayoutSelectedAction?.onNodeSelected(foundNode.node)
                panel.repaint()
            } else {
                calculateDistance(foundNode.rect)
            }
        }
    }

    private fun calculateDistance(measureRectangle: Shape) {
        val selected = selectedRectangle ?: throw IllegalStateException("No selected element to measure distance")
        calculateDistance(selected, measureRectangle)
    }

    fun calculateDistanceBetweenTwoViewNodes(startViewNode: ViewNode, endViewNode: ViewNode) {
        val node1 = allRectangles[startViewNode]
        val node2 = allRectangles[endViewNode]
        if (node1 != null && node2 != null) {
            calculateDistance(node1, node2)
        }
    }

    private fun calculateDistance(selected: Shape, measureRectangle: Shape, reverseLineSource: Boolean = false) {
        recalculateDistanceAction = CalculateDistanceAction(selected, measureRectangle)
        val selectedBounds = selected.bounds2D
        val targetBounds = measureRectangle.bounds2D
        this.measureRectangle = measureRectangle

        val distances = if (reverseLineSource) {
            distances.calculateDistance(targetBounds, selectedBounds)
        } else {
            distances.calculateDistance(selectedBounds, targetBounds)
        }
        measureLines.clear()
        measureLines.addAll(distances.lines)
        onLayoutSelectedAction?.onDistanceCalculated(distances.distance)
        panel.repaint()
    }

    fun showLayoutResult(layoutData: LayoutFileData) {
        distances.dpPerPixels = layoutData.dpPerPixels
        root = layoutData.node

        layoutData.bufferedImage?.let {
            screenshot = toCompatibleImage(it)
        }

        rectangles.clear()
        layoutModelRoots.clear()

        addFromViewNode(layoutModelRoots, layoutData.node, 0, 0)
    }

    private fun toCompatibleImage(image: BufferedImage): BufferedImage {
        /*
        * if image is already compatible and optimized for current system settings, simply return it
        */
        if (image.colorModel == GFX_CONFIG.colorModel) {
            return image
        }

        // image is not optimized, so create a new image that is
        val newImage = GFX_CONFIG.createCompatibleImage(image.width, image.height, image.transparency)

        // get the graphics context of the new image to draw the old image on
        val g2d = newImage.graphics as Graphics2D

        // actually draw the image and dispose of context no longer needed
        g2d.drawImage(image, 0, 0, null)
        g2d.dispose()

        // return the new optimized image
        return newImage
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
            if (rect.contains(point) && (child.node.isDrawn || settings.allowedSelectHiddenView)) {
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

    fun draw(
        g: Graphics2D,
        at: AffineTransform,
        screenTransformedRectangle: Rectangle2D.Double
    ) {
        val startDraw = System.currentTimeMillis()
        g.setRenderingHint(
            RenderingHints.KEY_INTERPOLATION,
            RenderingHints.VALUE_INTERPOLATION_BICUBIC
        )
        val renderingDuration = System.currentTimeMillis() - startDraw

        val drawImageStart = System.currentTimeMillis()

        screenshot?.let {
            g.drawImage(it, at, null)
        }
        val drawImageDuration = System.currentTimeMillis() - drawImageStart

        val drawRectanglesStart = System.currentTimeMillis()
        g.stroke = BasicStroke(1f)
        for (element in rectangles) {
            val rect = element.rect
            val rectBound = rect.bounds2D
            if (rectBound.maxX < screenTransformedRectangle.minX ||
                rectBound.minX > screenTransformedRectangle.maxX ||
                rectBound.maxY < screenTransformedRectangle.minY ||
                rectBound.minY > screenTransformedRectangle.maxY
            ) {
                continue
            }

            g.color = borderColor
            val transformedShape: Shape = at.createTransformedShape(rect)
            val bounds = transformedShape.bounds

            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)
        }
        val drawRectanglesDuration = System.currentTimeMillis() - drawRectanglesStart

        val drawOtherStart = System.currentTimeMillis()
        // draw measure lines
        g.stroke = measureLineStroke
        for (line in measureLines) {
            g.color = measureLineColor
            val line = at.createTransformedShape(line)
            g.draw(line)
        }

        hoveredRectangle?.let {
            g.stroke = BasicStroke(2f)
            g.color = hoverColor
            val bounds = at.createTransformedShape(it).bounds
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)
        }

        // draw selected item
        selectedRectangle?.let {
            g.stroke = BasicStroke(3f)
            g.color = selectedColor
            val bounds = at.createTransformedShape(it).bounds
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)
        }

        // draw measure target item
        measureRectangle?.let {
            g.stroke = BasicStroke(3f)
            g.color = measureColor
            val bounds = at.createTransformedShape(it).bounds
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)
        }

        rulerRectangle?.let {
            g.stroke = BasicStroke(3f)
            g.color = rulerColor
            val bounds = at.createTransformedShape(it).bounds
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)
            g.color = rulerFillColor
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
        }
        val drawOtherDuration = System.currentTimeMillis() - drawOtherStart

        //println("Draw = ${System.currentTimeMillis() - startDraw}, renderingDuration = $renderingDuration, drawImageDuration = $drawImageDuration, drawRectanglesDuration = $drawRectanglesDuration, drawOtherDuration = $drawOtherDuration")
    }

    fun selectNode(viewNode: ViewNode) {
        selectedRectangle = allRectangles[viewNode]
        recalculateDistanceAction = null
    }

    fun hoverNode(viewNode: ViewNode) {
        hoveredRectangle = allRectangles[viewNode]
    }

    fun removeNodeHover(): Boolean {
        val shouldRepaint = hoveredRectangle != null
        hoveredRectangle = null
        return shouldRepaint
    }

    fun setSizeDpMode(enabled: Boolean) {
        distances.sizeInDpEnabled = enabled
        recalculateDistanceAction?.recalculate()
    }

    fun onMouseRightMove(tranformed: Point2D) {
        selectedRectangle?.let {
            val fakeShape = Rectangle2D.Double(round(tranformed.x), round(tranformed.y), 0.0, 0.0)
            calculateDistance(it, fakeShape, reverseLineSource = true)
        }
    }

    fun onMouseUp() {
        selectionRectangle = null
    }

    fun enableRuler(transformed: Point2D) {
        rulerFirstPoint.setLocation(Point2D.Double(round(transformed.x), round(transformed.y)))
        rulerRectangle = Rectangle2D.Double(round(transformed.x), round(transformed.y), 0.0, 0.0)
    }

    fun expandRulerAndShowSize(tranformed: Point2D) {
        rulerRectangle?.let {
            val w = abs(tranformed.x - rulerFirstPoint.x)
            val h = abs(tranformed.y - rulerFirstPoint.y)
            val left = min(tranformed.x, rulerFirstPoint.x)
            val top = min(tranformed.y, rulerFirstPoint.y)
            it.setRect(round(left), round(top), round(w), round(h))
            measureRuler(it)
            panel.repaint()
        }
    }

    private fun measureRuler(ruler: Rectangle2D.Double) {
        //recalculateDistanceAction = CalculateDistanceAction(selected, measureRectangle)
        val distances = distances.calculateDistance(ruler)
        onLayoutSelectedAction?.onDistanceCalculated(distances.distance)
    }

    interface OnLayoutSelectedAction {
        fun onNodeHovered(node: ViewNode)
        fun onNodeSelected(node: ViewNode)
        fun onMouseExited()
        fun onDistanceCalculated(dimensions: Map<DistanceType, Double>)
    }

    inner class CalculateDistanceAction(val selected: Shape, val target: Shape) {
        fun recalculate() {
            calculateDistance(selected, target)
        }
    }
}
