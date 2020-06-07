package com.github.grishberg.android.layoutinspector.ui.layout

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

data class DistanceData(val distance: Map<DistanceType, Int>, val lines: List<Shape>)

class DistanceBetweenTwoShape {
    fun calculateDistance(selectedBounds: Rectangle2D, targetBounds: Rectangle2D): DistanceData {
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

        val result = mutableMapOf<DistanceType, Int>()
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
            lines.add(Line2D.Double(verticalLineX,selectedTop, verticalLineX,  targetTop))
            lines.add(Line2D.Double(verticalLineX,selectedBottom, verticalLineX, targetBottom))

            result[DistanceType.LEFT] = abs(selLeftToTargetLeft.toInt())
            result[DistanceType.RIGHT] = abs(selRightToTargetRight.toInt())
            result[DistanceType.TOP] = abs(selTopToTargetTop.toInt())
            result[DistanceType.BOTTOM] = abs(selBottomToTargetBottom.toInt())

            return DistanceData(result, lines)
        }

        if (selectedBounds.intersects(targetBounds) || targetBounds.intersects(selectedBounds)) {
            if (selectedLeft > targetLeft) {
                result[DistanceType.LEFT] = abs(selLeftToTargetRight.toInt())
                result[DistanceType.RIGHT] = abs(selLeftToTargetLeft.toInt())
            } else {
                result[DistanceType.RIGHT] = abs(selRightToTargetLeft.toInt())
                result[DistanceType.LEFT] = abs(selLeftToTargetLeft.toInt())
            }

            if (selectedTop > targetTop) {
                result[DistanceType.BOTTOM] = abs(selTopToTargetTop.toInt())
                result[DistanceType.TOP] = abs(selTopToTargetBottom.toInt())
            } else {
                result[DistanceType.TOP] = abs(selTopToTargetTop.toInt())
                result[DistanceType.BOTTOM] = abs(selBottomToTargetTop.toInt())
            }

            return DistanceData(result, lines)
        }
        val verticalLineX = selectedBounds.centerX
        val horizontalLineY = selectedBounds.centerY

        if (selectedBounds.minX >= targetBounds.maxX) {
            lines.add(Line2D.Double(selectedLeft, horizontalLineY, targetRight, horizontalLineY))
            result[DistanceType.LEFT] = abs(selLeftToTargetRight.toInt())
        } else if (selectedBounds.maxX <= targetBounds.minX) {
            lines.add(Line2D.Double(targetLeft, horizontalLineY, selectedRight, horizontalLineY))
            result[DistanceType.RIGHT] = abs(selRightToTargetLeft.toInt())
        }
        if (selectedBounds.minY >= targetBounds.maxY) {
            lines.add(Line2D.Double(verticalLineX, selectedTop, verticalLineX, targetBottom))
            result[DistanceType.TOP] = abs(selTopToTargetBottom.toInt())
        } else if (selectedBounds.maxY <= targetBounds.minY) {
            lines.add(Line2D.Double(verticalLineX, selectedBottom, verticalLineX, targetTop))
            result[DistanceType.BOTTOM] = abs(selBottomToTargetTop.toInt())
        }

        return DistanceData(result, lines)
    }
}
