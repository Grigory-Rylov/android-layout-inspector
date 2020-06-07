package com.github.grishberg.android.layoutinspector.ui.layout

import java.awt.geom.Rectangle2D
import kotlin.math.abs

enum class DistanceType {
    LEFT,
    TOP,
    RIGHT,
    BOTTOM
}

class DistanceBetweenTwoShape {
    fun calculateDistance(selectedBounds: Rectangle2D, targetBounds: Rectangle2D): Map<DistanceType, Int> {
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

        if (selectedBounds.contains(targetBounds) || targetBounds.contains(selectedBounds)) {
            result[DistanceType.LEFT] =  abs(selLeftToTargetLeft.toInt())
            result[DistanceType.RIGHT] =  abs(selRightToTargetRight.toInt())
            result[DistanceType.TOP] =  abs(selTopToTargetTop.toInt())
            result[DistanceType.BOTTOM] =  abs(selBottomToTargetBottom.toInt())

            return result
        }

        if (selectedBounds.intersects(targetBounds) || targetBounds.intersects(selectedBounds)) {
            if (selectedLeft > targetLeft) {
                result[DistanceType.LEFT] =  abs(selLeftToTargetRight.toInt())
                result[DistanceType.RIGHT] =  abs(selLeftToTargetLeft.toInt())
            } else {
                result[DistanceType.RIGHT] =  abs(selRightToTargetLeft.toInt())
                result[DistanceType.LEFT] =  abs(selLeftToTargetLeft.toInt())
            }

            if (selectedTop > targetTop) {
                result[DistanceType.BOTTOM] =  abs(selTopToTargetTop.toInt())
                result[DistanceType.TOP] =  abs(selTopToTargetBottom.toInt())

            } else {
                result[DistanceType.TOP] =  abs(selTopToTargetTop.toInt())
                result[DistanceType.BOTTOM] =  abs(selBottomToTargetTop.toInt())
            }

            return result
        }

        if (selectedBounds.minX >= targetBounds.maxX) {
            result[DistanceType.LEFT] =  abs(selLeftToTargetRight.toInt())
        } else if (selectedBounds.maxX <= targetBounds.minX) {
            result[DistanceType.RIGHT] =  abs(selRightToTargetLeft.toInt())
        }
        if (selectedBounds.minY >= targetBounds.maxY) {
            result[DistanceType.TOP] =  abs(selTopToTargetBottom.toInt())
        } else if (selectedBounds.maxY <= targetBounds.minY) {
            result[DistanceType.BOTTOM] =  abs(selBottomToTargetTop.toInt())
        }

        return result
    }
}
