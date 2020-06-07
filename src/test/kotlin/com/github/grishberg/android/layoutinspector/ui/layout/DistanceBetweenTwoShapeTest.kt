package com.github.grishberg.android.layoutinspector.ui.layout

import org.junit.Assert.assertEquals
import org.junit.Test
import java.awt.Rectangle

class DistanceBetweenTwoShapeTest {
    private val underTest = DistanceBetweenTwoShape()

    @Test
    fun `selected inside target`() {
        val selected = Rectangle(0, 0, 100, 100)
        val target = Rectangle(10, 10, 20, 20)

        val result = underTest.calculateDistance(selected, target, dpPerPixels, sizeInDpEnabled)
        assertEquals(10, result.distance[DistanceType.LEFT])
        assertEquals(70, result.distance[DistanceType.RIGHT])
        assertEquals(10, result.distance[DistanceType.TOP])
        assertEquals(70, result.distance[DistanceType.BOTTOM])
    }

    @Test
    fun `selected between target`() {
        val target = Rectangle(0, 0, 10, 10)
        val selected = Rectangle(50, 50, 15, 15)

        val result = underTest.calculateDistance(selected, target, dpPerPixels, sizeInDpEnabled)
        assertEquals(40, result.distance[DistanceType.LEFT])
        assertEquals(null, result.distance[DistanceType.RIGHT])
        assertEquals(40, result.distance[DistanceType.TOP])
        assertEquals(null, result.distance[DistanceType.BOTTOM])
    }

    @Test
    fun `target between selected`() {
        val selected = Rectangle(0, 0, 10, 10)
        val target = Rectangle(50, 50, 15, 15)

        val result = underTest.calculateDistance(selected, target, dpPerPixels, sizeInDpEnabled)
        assertEquals(40, result.distance[DistanceType.RIGHT])
        assertEquals(null, result.distance[DistanceType.LEFT])
        assertEquals(40, result.distance[DistanceType.BOTTOM])
        assertEquals(null, result.distance[DistanceType.TOP])
    }


    @Test
    fun `selected intersect target`() {
        val selected = Rectangle(0, 0, 50, 50)
        val target = Rectangle(40, 40, 85, 85)

        val result = underTest.calculateDistance(selected, target, dpPerPixels, sizeInDpEnabled)
        assertEquals("right", 10, result.distance[DistanceType.RIGHT])
        assertEquals("left", 40, result.distance[DistanceType.LEFT])
        assertEquals("bottom", 10, result.distance[DistanceType.BOTTOM])
        assertEquals("top", 40, result.distance[DistanceType.TOP])
    }

    @Test
    fun `target intersect selected`() {
        val target = Rectangle(0, 0, 50, 50)
        val selected = Rectangle(40, 40, 85, 85)

        val result = underTest.calculateDistance(selected, target, dpPerPixels, sizeInDpEnabled)
        assertEquals("left", 10, result.distance[DistanceType.LEFT])
        assertEquals("right", 40, result.distance[DistanceType.RIGHT])
        assertEquals("bottom", 40, result.distance[DistanceType.BOTTOM])
        assertEquals("top", 10, result.distance[DistanceType.TOP])
    }
}
