package com.github.grishberg.android.layoutinspector.ui.layout

import com.github.grishberg.android.layoutinspector.domain.MetaRepository
import java.awt.Shape
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import kotlin.math.abs

enum class DistanceType {
    LEFT,
    TOP,
    RIGHT,
    BOTTOM
}

data class DistanceData(val distance: Map<DistanceType, Double>, val lines: List<Shape>)

class DistanceBetweenTwoShape(
    private val meta: MetaRepository
) {
    var sizeInDpEnabled = false

    fun calculateDistance(
        selectedBounds: Rectangle2D,
        targetBounds: Rectangle2D
    ): DistanceData {
        val selectedLeft = selectedBounds.x
        val selectedRight = selectedBounds.x + selectedBounds.width
        val selectedTop = selectedBounds.y
        val selectedBottom = selectedBounds.y + selectedBounds.height

        val targetLeft = targetBounds.x
        val targetRight = targetBounds.x + targetBounds.width
        val targetTop = targetBounds.y
        val targetBottom = targetBounds.y + targetBounds.height

        val selLeftToTargetLeft = selectedLeft - targetLeft
        val selLeftToTargetRight = selectedLeft - targetRight
        val selRightToTargetRight = selectedRight - targetRight
        val selRightToTargetLeft = selectedRight - targetLeft

        val selTopToTargetTop = selectedTop - targetTop
        val selTopToTargetBottom = selectedTop - targetBottom
        val selBottomToTargetBottom = selectedBottom - targetBottom
        val selBottomToTargetTop = selectedBottom - targetTop

        val result = mutableMapOf<DistanceType, Double>()
        val lines = mutableListOf<Shape>()

        if (selectedBounds.contains(targetBounds) || targetBounds.contains(selectedBounds)) {
            val verticalLineX: Double
            val horizontalLineY: Double
            if (selectedBounds.contains(targetBounds)) {
                verticalLineX = targetBounds.centerX
                horizontalLineY = targetBounds.centerY
            } else {
                verticalLineX = selectedBounds.centerX
                horizontalLineY = selectedBounds.centerY
            }
            lines.add(Line2D.Double(selectedLeft, horizontalLineY, targetLeft, horizontalLineY))
            lines.add(Line2D.Double(selectedRight, horizontalLineY, targetRight, horizontalLineY))
            lines.add(Line2D.Double(verticalLineX, selectedTop, verticalLineX, targetTop))
            lines.add(Line2D.Double(verticalLineX, selectedBottom, verticalLineX, targetBottom))

            result[DistanceType.LEFT] = abs(convertValueToDpIfNeeded(selLeftToTargetLeft))
            result[DistanceType.RIGHT] = abs(convertValueToDpIfNeeded(selRightToTargetRight))
            result[DistanceType.TOP] = abs(convertValueToDpIfNeeded(selTopToTargetTop))
            result[DistanceType.BOTTOM] = abs(convertValueToDpIfNeeded(selBottomToTargetBottom))

            return DistanceData(result, lines)
        }

        if (selectedBounds.intersects(targetBounds) || targetBounds.intersects(selectedBounds)) {
            if (selectedLeft > targetLeft) {
                result[DistanceType.LEFT] = abs(convertValueToDpIfNeeded(selLeftToTargetRight))
                result[DistanceType.RIGHT] = abs(convertValueToDpIfNeeded(selLeftToTargetLeft))
            } else {
                result[DistanceType.RIGHT] = abs(convertValueToDpIfNeeded(selRightToTargetLeft))
                result[DistanceType.LEFT] = abs(convertValueToDpIfNeeded(selLeftToTargetLeft))
            }

            if (selectedTop > targetTop) {
                result[DistanceType.BOTTOM] = abs(convertValueToDpIfNeeded(selTopToTargetTop))
                result[DistanceType.TOP] = abs(convertValueToDpIfNeeded(selTopToTargetBottom))
            } else {
                result[DistanceType.TOP] = abs(convertValueToDpIfNeeded(selTopToTargetTop))
                result[DistanceType.BOTTOM] = abs(convertValueToDpIfNeeded(selBottomToTargetTop))
            }

            return DistanceData(result, lines)
        }
        val verticalLineX = selectedBounds.centerX
        val horizontalLineY = selectedBounds.centerY

        if (selectedBounds.minX >= targetBounds.maxX) {
            lines.add(Line2D.Double(selectedLeft, horizontalLineY, targetRight, horizontalLineY))
            result[DistanceType.LEFT] = abs(convertValueToDpIfNeeded(selLeftToTargetRight))
        } else if (selectedBounds.maxX <= targetBounds.minX) {
            lines.add(Line2D.Double(targetLeft, horizontalLineY, selectedRight, horizontalLineY))
            result[DistanceType.RIGHT] = abs(convertValueToDpIfNeeded(selRightToTargetLeft))
        }
        if (selectedBounds.minY >= targetBounds.maxY) {
            lines.add(Line2D.Double(verticalLineX, selectedTop, verticalLineX, targetBottom))
            result[DistanceType.TOP] = abs(convertValueToDpIfNeeded(selTopToTargetBottom))
        } else if (selectedBounds.maxY <= targetBounds.minY) {
            lines.add(Line2D.Double(verticalLineX, selectedBottom, verticalLineX, targetTop))
            result[DistanceType.BOTTOM] = abs(convertValueToDpIfNeeded(selBottomToTargetTop))
        }

        return DistanceData(result, lines)
    }

    fun calculateDistance(rulerBounds: Rectangle2D): DistanceData {
        val result = mutableMapOf<DistanceType, Double>()
        result[DistanceType.LEFT] = abs(convertValueToDpIfNeeded(rulerBounds.width))
        result[DistanceType.TOP] = abs(convertValueToDpIfNeeded(rulerBounds.height))
        return DistanceData(result, emptyList())
    }

    private fun convertValueToDpIfNeeded(value: Double): Double {
        if (sizeInDpEnabled) {
            return value / meta.dpPerPixels
        }
        return value
    }
}
