package com.github.grishberg.android.layoutinspector.ui.layout

import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.domain.MetaRepository
import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import com.github.grishberg.android.layoutinspector.ui.screenshottest.ScreenshotPainter
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Dimension
import java.awt.Graphics2D
import java.awt.GraphicsEnvironment
import java.awt.Point
import java.awt.Rectangle
import java.awt.Shape
import java.awt.Stroke
import java.awt.geom.AffineTransform
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import javax.swing.JPanel
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.round


class LayoutLogic(
    private val panel: JPanel,
    private val meta: MetaRepository,
    private val settings: SettingsFacade,
    private val latyoutsState: LayoutsEnabledState,
    private val imgageHelper: ImageHelper
): ScreenshotPainter {
    private val GFX_CONFIG = GraphicsEnvironment.getLocalGraphicsEnvironment().defaultScreenDevice.defaultConfiguration
    private val skippedNodes = mutableMapOf<ViewNode, Boolean>()

    var onLayoutSelectedAction: OnLayoutSelectedAction? = null

    private var screenshotBuffer: BufferedImage? = null

    var screenshot: BufferedImage? = null
        private set
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
    private var rulerPointer: Rectangle2D.Double? = null
    private val rulerFirstPoint = Point2D.Double()

    private val measureLines = mutableListOf<Shape>()
    private val selectedColor = Color(41, 105, 248)
    private val hoverColor = Color(248, 25, 25)
    private val measureColor = Color(248, 225, 25)
    private val rulerColor = Color(4, 246, 187, 128)
    private val rulerFillColor = Color(77, 246, 206, 128)
    private val rulerPointerColor = Color(0, 0, 0, 128)
    private val measureLineColor = selectedColor
    private val measureLineStroke: Stroke =
        BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0f, floatArrayOf(9f), 0f)

    private val distances = DistanceBetweenTwoShape(meta)
    private var recalculateDistanceAction: CalculateDistanceAction? = null
    private var screenshotOffsetX: Int = 0
    private var screenshotOffsetY: Int = 0
    private var screenshotOffsetTransform: AffineTransform = AffineTransform.getTranslateInstance(0.0, 0.0)

    init {
        meta.hiddenChangedAction.add { panel.invalidate() }
    }

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
        clearSkippedTouches()

        val foundNode = getChildAtPoint(point, settings.ignoreLastClickedView)

        removeSkippedNodesThatWasNotClicked()

        if (foundNode != null) {
            recalculateDistanceAction = null
            selectedRectangle = foundNode.rect
            onLayoutSelectedAction?.onNodeSelected(foundNode.node)

            if (settings.ignoreLastClickedView) {
                skippedNodes[foundNode.node] = true
            }
        }
    }

    private fun clearSkippedTouches() {
        for (n in skippedNodes) {
            n.setValue(false)
        }
    }

    private fun removeSkippedNodesThatWasNotClicked() {
        val nodesToRemove = skippedNodes.filter { !it.value }
        for (n in nodesToRemove) {
            skippedNodes.remove(n.key)
        }
    }

    fun selectElementAndMeasureIfNeeded(point: Point) {
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
        removeSelection()

        root = layoutData.node

        layoutData.bufferedImage?.let {
            screenshot = toCompatibleImage(it)
            screenshotBuffer = imgageHelper.copyImage(screenshot)
        }

        rectangles.clear()
        layoutModelRoots.clear()

        layoutData.node?.let { node ->
            screenshotOffsetX = node.locationOnScreenX
            screenshotOffsetY = node.locationOnScreenY
        }
        addFromViewNode(layoutModelRoots, layoutData.node)
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
    ) {
        if (node == null) {
            return
        }

        val left = node.locationOnScreenX
        val top = node.locationOnScreenY
        val children = mutableListOf<LayoutModel>()
        val rect = Rectangle(left, top, node.displayInfo.width, node.displayInfo.height)
        val newLayoutModel = LayoutModel(rect, node, children)
        parentsChildren.add(newLayoutModel)
        if (isNodeVisible(node, false)) {
            rectangles.add(newLayoutModel)
        }
        allRectangles[node] = rect
        if (node.isLeaf) {
            return
        }

        val count = node.childCount
        for (i in 0 until count) {
            addFromViewNode(children, node.getChildAt(i))
        }
    }

    private fun isNodeVisible(
        node: ViewNode,
        shouldSkipViews: Boolean
    ): Boolean {
        if (shouldSkipViews) {
            markSkippedAsTouched(node)

            if (shouldSkipNode(node)) {
                return false
            }
        }
        if (meta.shouldHideInLayout(node)) {
            return false
        }
        return node.displayInfo.isVisible
    }

    private fun shouldSkipNode(node: ViewNode): Boolean {
        return skippedNodes.contains(node)
    }

    private fun getChildAtPoint(point: Point, shouldSkipViews: Boolean = false): LayoutModel? {
        for (element in layoutModelRoots) {
            if (shouldSkipViews) {
                markSkippedAsTouched(element.node)

                if (shouldSkipNode(element.node)) {
                    continue
                }
            }
            val itemByPos = findFirstElementByPosition(point, element, shouldSkipViews)
            if (itemByPos != null) {
                return itemByPos
            }
        }
        if (shouldSkipViews) {
            skippedNodes.clear()
            return getChildAtPoint(point)
        }
        return null
    }

    private fun findFirstElementByPosition(
        point: Point,
        parent: LayoutModel,
        shouldSkipViews: Boolean
    ): LayoutModel? {
        val childCount = parent.children.size
        for (i in childCount - 1 downTo 0) {
            val child = parent.children[i]
            val rect = child.rect
            if (rect.contains(point) && (isNodeVisible(
                    child.node,
                    shouldSkipViews
                ) || settings.allowedSelectHiddenView)
            ) {
                return findFirstElementByPosition(point, child, shouldSkipViews)
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

    private fun markSkippedAsTouched(element: ViewNode) {
        if (skippedNodes[element] != null) {
            skippedNodes[element] = true
        }
    }

    fun draw(
        g: Graphics2D,
        at: AffineTransform,
        screenTransformedRectangle: Rectangle2D.Double
    ) {
        screenshotBuffer?.let { screenshot ->
            screenshotOffsetTransform.setToIdentity()
            screenshotOffsetTransform.setTransform(at)
            screenshotOffsetTransform.translate(screenshotOffsetX.toDouble(), screenshotOffsetY.toDouble())
            g.drawImage(screenshot, screenshotOffsetTransform, null)
        }

        if (!latyoutsState.isEnabled) {
            return
        }

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

            g.stroke = BasicStroke(1f)
            g.color = borderColor
            val transformedShape: Shape = at.createTransformedShape(rect)
            val bounds = transformedShape.bounds

            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)

            if (settings.showSerifsInTheMiddleAll) {
                drawSerifs(g, bounds)
            }

        }
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

            if (settings.showSerifsInTheMiddleOfSelected) {
                drawSerifs(g, bounds)
            }
        }

        // draw measure target item
        measureRectangle?.let {
            g.stroke = BasicStroke(3f)
            g.color = measureColor
            val bounds = at.createTransformedShape(it).bounds
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)
        }

        rulerPointer?.let {
            g.stroke = BasicStroke(3f)
            g.color = rulerPointerColor
            val bounds = at.createTransformedShape(it).bounds
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
        }
        rulerRectangle?.let {
            g.stroke = BasicStroke(3f)
            g.color = rulerColor
            val bounds = at.createTransformedShape(it).bounds
            g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height)
            g.color = rulerFillColor
            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height)
        }
    }

    private fun drawSerifs(g: Graphics2D, bounds: Rectangle) {
        g.stroke = BasicStroke(1f)

        val serifs = mutableListOf<Shape>()
        serifs.add(
            Line2D.Double(
                bounds.x - SERIF_SIZE_IN_PIX, bounds.y + bounds.height / 2.0,
                bounds.x + SERIF_SIZE_IN_PIX, bounds.y + bounds.height / 2.0
            )
        )
        serifs.add(
            Line2D.Double(
                bounds.x + bounds.width - SERIF_SIZE_IN_PIX, bounds.y + bounds.height / 2.0,
                bounds.x + bounds.width + SERIF_SIZE_IN_PIX, bounds.y + bounds.height / 2.0
            )

        )
        serifs.add(
            Line2D.Double(
                bounds.x + bounds.width / 2.0, bounds.y - SERIF_SIZE_IN_PIX,
                bounds.x + bounds.width / 2.0, bounds.y + SERIF_SIZE_IN_PIX
            )
        )

        serifs.add(
            Line2D.Double(
                bounds.x + bounds.width / 2.0, bounds.y + bounds.height - SERIF_SIZE_IN_PIX,
                bounds.x + bounds.width / 2.0, bounds.y + bounds.height + SERIF_SIZE_IN_PIX
            )
        )

        serifs.forEach {
            g.draw(it)
        }
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

    fun roundDp() {
        if (!distances.sizeInDpEnabled) {
            return
        }

        recalculateDistanceAction?.recalculate()
    }

    fun onMouseRightMove(tranformed: Point2D) {
        clearSkippedTouches()

        selectedRectangle?.let {
            val fakeShape = Rectangle2D.Double(round(tranformed.x), round(tranformed.y), 0.0, 0.0)
            calculateDistance(it, fakeShape, reverseLineSource = true)
        }
    }

    fun onMouseUp() {
        selectionRectangle = null
    }

    fun enableRuler(transformed: Point2D) {
        rulerPointer = null
        rulerFirstPoint.setLocation(Point2D.Double(round(transformed.x), round(transformed.y)))
        rulerRectangle = Rectangle2D.Double(round(transformed.x), round(transformed.y), 0.0, 0.0)
    }

    fun expandRulerAndShowSize(transformed: Point2D) {
        rulerRectangle?.let {
            var w = abs(transformed.x - rulerFirstPoint.x)
            var h = abs(transformed.y - rulerFirstPoint.y)
            val left = min(transformed.x, rulerFirstPoint.x)
            val top = min(transformed.y, rulerFirstPoint.y)
            if (transformed.x < rulerFirstPoint.x) {
                w += 1.0
            }
            if (transformed.y < rulerFirstPoint.y) {
                h += 1.0
            }
            it.setRect(round(left), round(top), round(w), round(h))
            measureRuler(it)
            panel.repaint()
        }
    }

    fun showPointer(transformed: Point2D) {
        if (rulerRectangle == null) {
            rulerPointer?.let { rect ->
                rect.x = round(transformed.x)
                rect.y = round(transformed.y)
                panel.repaint()
            }
            if (rulerPointer == null) {
                rulerPointer = Rectangle2D.Double(round(transformed.x), round(transformed.y), 1.0, 1.0)
            }
        }
    }

    private fun measureRuler(ruler: Rectangle2D.Double) {
        val distances = distances.calculateDistance(ruler)
        onLayoutSelectedAction?.onDistanceCalculated(distances.distance)
    }

    fun hasSelection() = selectedRectangle != null

    fun removeSelection() {
        selectedRectangle = null
        selectionRectangle = null
        rulerRectangle = null
        rulerPointer = null
        hoveredRectangle = null
        recalculateDistanceAction = null
        skippedNodes.clear()
    }

    override fun paintDifferencePixel(x: Int, y: Int) {
        screenshotBuffer?.let {
            val newColor = blend(Color(it.getRGB(x,y)), Color.magenta)
            it.setRGB(x, y, newColor.rgb)
        }
    }

    private fun blend(c0: Color, c1: Color): Color {
        val totalAlpha = (c0.alpha + c1.alpha).toDouble()
        val weight0 = c0.alpha / totalAlpha
        val weight1 = c1.alpha / totalAlpha
        val r = weight0 * c0.red + weight1 * c1.red
        val g = weight0 * c0.green + weight1 * c1.green
        val b = weight0 * c0.blue + weight1 * c1.blue
        val a = Math.max(c0.alpha, c1.alpha).toDouble()
        return Color(r.toInt(), g.toInt(), b.toInt(), a.toInt())
    }

    override fun invalidate() {
        panel.repaint()
    }

    override fun clearDifferences() {
        screenshotBuffer = imgageHelper.copyImage(screenshot)
        panel.repaint()
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

    private companion object {
        private const val SERIF_SIZE_IN_PIX = 4.0
    }
}
