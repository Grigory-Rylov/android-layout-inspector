package com.github.grishberg.android.layoutinspector.ui.info.flat.filter

import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class SimpleFilterTextView : FilterView {
    private var listener: (String) -> Unit = {}

    override val component = JPanel(BorderLayout())
    private val filterText = JTextField("")

    init {
        val panel = JPanel(BorderLayout())
        val label = JLabel("Filter")
        panel.add(label, BorderLayout.WEST);

        panel.add(filterText, BorderLayout.CENTER)

        filterText.document.addDocumentListener(object : DocumentListener {
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
                listener.invoke(filterText.text)
            }
        })
    }


    override fun getFilterText(): String = filterText.text

    override fun setOnTextChangedListener(listener: (String) -> Unit) {
        this.listener = listener
    }
}