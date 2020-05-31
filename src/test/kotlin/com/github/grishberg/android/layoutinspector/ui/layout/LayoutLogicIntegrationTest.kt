package com.github.grishberg.android.layoutinspector.ui.layout

import com.android.layoutinspector.TestUtil
import com.android.layoutinspector.model.ViewNode
import com.android.layoutinspector.parser.LayoutFileDataParser
import com.nhaarman.mockitokotlin2.mock
import org.junit.Assert
import org.junit.Test
import java.awt.Point
import javax.swing.JPanel

class LayoutLogicIntegrationTest {
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
    }

    private val underTest = LayoutLogic(panel).apply {
        onLayoutSelectedAction = selectAction
    }

    @Test
    fun `click to root view`() {
        val file = TestUtil.getTestFile("test.li")
        val data = LayoutFileDataParser.parseFromFile(file)

        underTest.showLayoutResult(data)

        //underTest.processMouseClicked(Point(270, 1120))
        underTest.processMouseClicked(Point(54, 484))

        Assert.assertEquals("ru.yandex.searchplugin.morda.bender.services.navigationpanel.BenderServicesNavigationPanelItemIconContainer", selectedNode?.name)
    }

}