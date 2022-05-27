package com.github.grishberg.android.layoutinspector.ui.info.flat.filter

import com.github.grishberg.android.layoutinspector.settings.SettingsFacade
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SimpleFilterTextView(
    private val settings: SettingsFacade
) : FilterView {
    private var listener: (String) -> Unit = {}

    private val filterTextField = JTextField(settings.lastFilter)

    override val component = JPanel(BorderLayout())
    override val filterText: String
        get() = filterTextField.text

    init {
        val panel = component
        val label = JLabel("Filter")
        panel.add(label, BorderLayout.WEST)

        panel.add(filterTextField, BorderLayout.CENTER)

        val clearButton = JButton("x")
        clearButton.toolTipText = "clear text"
        panel.add(clearButton, BorderLayout.EAST)

        filterTextField.document.addDocumentListener(object : DocumentListener {
            override fun changedUpdate(e: DocumentEvent) {
                onTextChanged()
            }

            override fun removeUpdate(e: DocumentEvent) {
                onTextChanged()
            }

            override fun insertUpdate(e: DocumentEvent) {
                onTextChanged()
            }

            private fun onTextChanged() {
                settings.lastFilter = filterTextField.text
                listener.invoke(filterTextField.text)
            }
        })

        clearButton.addActionListener {
            filterTextField.text = ""
        }
    }


    override fun setOnTextChangedListener(listener: (String) -> Unit) {
        this.listener = listener
    }
}