package com.github.grishberg.android.layoutinspector.ui.common

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class LabeledGridBuilder {
    val content = JPanel()

    private val labelConstraints = GridBagConstraints()
    private val fieldConstraints = GridBagConstraints()

    init {
        content.border = EmptyBorder(8, 8, 8, 8)
        content.layout = GridBagLayout()

        labelConstraints.weightx = 0.0
        labelConstraints.gridwidth = 1
        labelConstraints.gridy = 0
        labelConstraints.gridx = 0

        fieldConstraints.gridwidth = 3
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL
        fieldConstraints.gridy = 0
    }

    fun addLabeledComponent(labelText: String, component: JComponent) {
        addLabeledComponent(JLabel(labelText), component)
    }

    fun addLabeledComponent(label: JLabel, component: JComponent) {
        fieldConstraints.gridwidth = 1
        content.add(label, labelConstraints)

        content.add(component, fieldConstraints)

        labelConstraints.gridy++
        fieldConstraints.gridy++
    }

    fun addSingleComponent(component: JComponent) {
        fieldConstraints.gridwidth = 4
        content.add(component, fieldConstraints)
        fieldConstraints.gridy++
        labelConstraints.gridy++
    }

    fun addMainAndSlaveComponent(mainComponent: JComponent, slaveComponent: JComponent) {
        fieldConstraints.fill = GridBagConstraints.HORIZONTAL
        fieldConstraints.gridwidth = 1
        fieldConstraints.gridx = 0
        fieldConstraints.weightx = 3/4.0
        content.add(mainComponent, fieldConstraints)

        fieldConstraints.gridwidth = 1
        fieldConstraints.fill = GridBagConstraints.PAGE_END
        fieldConstraints.gridx = 1
        fieldConstraints.weightx = 1/4.0
        content.add(slaveComponent, fieldConstraints)

        fieldConstraints.fill = GridBagConstraints.HORIZONTAL
        fieldConstraints.gridx = 0
        fieldConstraints.gridy++
        labelConstraints.gridy++
    }

}