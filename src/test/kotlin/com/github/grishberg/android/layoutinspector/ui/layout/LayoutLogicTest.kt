package com.github.grishberg.android.layoutinspector.ui.layout

import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.Utils
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert.assertEquals
import org.junit.Test
import java.awt.Point
import javax.swing.JPanel

class LayoutLogicTest {
    private val panel = mock<JPanel>()
    var selectedNode: ViewNode? = null
    private val selectAction = object : LayoutLogic.OnLayoutSelectedAction {
        override fun onNodeHovered(node: ViewNode) {
            selectedNode = node
        }

        override fun onNodeSelected(node: ViewNode) {
            selectedNode = node
        }

        override fun onMouseExited() = Unit

        override fun onDistanceCalculated(dimensions: Map<DistanceType, Int>) = Unit
    }

    private val underTest = LayoutLogic(panel).apply {
        onLayoutSelectedAction = selectAction
    }

    @Test
    fun `click to root view`() {
        val sample = Utils.createSample1()
        underTest.showLayoutResult(sample)

        underTest.processMouseClicked(Point(2, 2))

        assertEquals("root", selectedNode?.name)
    }

    @Test
    fun `click to child1`() {
        val sample = Utils.createSample1()
        underTest.showLayoutResult(sample)

        underTest.processMouseClicked(Point(11, 11))

        assertEquals("child1", selectedNode?.name)
    }

    @Test
    fun `click to child2`() {
        val sample = Utils.createSample1()
        underTest.showLayoutResult(sample)

        underTest.processMouseClicked(Point(20, 20))

        assertEquals("child2", selectedNode?.name)
    }

    @Test
    fun `click to child3`() {
        val sample = Utils.createSample1()
        underTest.showLayoutResult(sample)

        underTest.processMouseClicked(Point(20, 60))

        assertEquals("child3", selectedNode?.name)
    }
}
