package com.github.grishberg.android.layoutinspector.ui.dialogs

import com.android.layoutinspector.common.AppLogger
import java.awt.Dimension
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class LogPanel : JPanel(), AppLogger {
    private val logArea = JTextArea()
    private val scrollPane = JScrollPane(logArea)
    private val clearButton = JButton("Clear")
    private val autoScrollCheckBox = JCheckBox("Auto-scroll", true)
    
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    
    init {
        layout = java.awt.BorderLayout()
        
        logArea.isEditable = false
        logArea.lineWrap = true
        logArea.wrapStyleWord = true
        logArea.font = java.awt.Font("Monospaced", java.awt.Font.PLAIN, 12)
        
        scrollPane.preferredSize = Dimension(300, 0)
        
        clearButton.addActionListener {
            logArea.text = ""
        }
        
        val topPanel = JPanel(java.awt.FlowLayout(java.awt.FlowLayout.LEFT))
        topPanel.add(JLabel("Logs:"))
        topPanel.add(autoScrollCheckBox)
        topPanel.add(Box.createHorizontalStrut(10))
        topPanel.add(clearButton)
        
        add(topPanel, java.awt.BorderLayout.NORTH)
        add(scrollPane, java.awt.BorderLayout.CENTER)
        
        logArea.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                if (autoScrollCheckBox.isSelected) {
                    scrollToBottom()
                }
            }
            
            override fun removeUpdate(e: DocumentEvent?) {}
            
            override fun changedUpdate(e: DocumentEvent?) {}
        })
    }
    
    private fun scrollToBottom() {
        scheduler.schedule({
            SwingUtilities.invokeLater {
                val caretPosition = logArea.document.length
                logArea.caretPosition = caretPosition
            }
        }, 50, TimeUnit.MILLISECONDS)
    }
    
    override fun d(msg: String) {
        appendLog("[DEBUG] $msg")
    }
    
    override fun e(msg: String) {
        appendLog("[ERROR] $msg")
    }
    
    override fun e(msg: String, t: Throwable) {
        appendLog("[ERROR] $msg: ${t.message}")
        appendLog(t.stackTraceToString())
    }
    
    override fun w(msg: String) {
        appendLog("[WARN] $msg")
    }
    
    private fun appendLog(msg: String) {
        SwingUtilities.invokeLater {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())
            logArea.append("[$timestamp] $msg\n")
            if (autoScrollCheckBox.isSelected) {
                scrollToBottom()
            }
        }
    }
    
    fun clear() {
        logArea.text = ""
    }
    
    fun getLogText(): String {
        return logArea.text
    }
}
