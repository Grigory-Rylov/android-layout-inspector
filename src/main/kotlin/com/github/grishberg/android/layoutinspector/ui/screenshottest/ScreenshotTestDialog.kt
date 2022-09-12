package com.github.grishberg.android.layoutinspector.ui.screenshottest

import com.github.grishberg.android.layoutinspector.ui.dialogs.CloseByEscapeDialog
import java.awt.BorderLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.awt.image.BufferedImage
import javax.swing.BorderFactory
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.WindowConstants

private const val TOP_OFFSET = 16

class ScreenshotTestDialog(
    private val owner: JFrame,
    private val screenshotPainter: ScreenshotPainter,
) : CloseByEscapeDialog(owner, "Screenshot test", false), ScreenshotTestView {

    private val logic = ScreenshotTestLogic(this)
    val label = JLabel("Comparing...")
    val panel = JPanel()

    init {
        panel.layout = BorderLayout(4, 4)
        panel.border = BorderFactory.createEmptyBorder(32, 32, 32, 32)

        label.horizontalAlignment = JLabel.CENTER;
        panel.add(label, BorderLayout.CENTER)
        setContentPane(panel)
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        pack()

        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) = Unit

            override fun windowClosing(e: WindowEvent) {
                screenshotPainter.clearDifferences()
            }
        })
    }

    fun showDialog(
        reference: BufferedImage,
        comparable: BufferedImage,
    ) {
        val x = (owner.width) / 2 - (width / 2)
        val y = TOP_OFFSET
        setLocation(x, y)
        logic.compare(reference, comparable, screenshotPainter)
        isVisible = true
    }

    override fun onDialogClosed() {
        super.onDialogClosed()
        screenshotPainter.clearDifferences()
    }

    override fun showNoDifferences() {
        label.text = "Screenshots are equals"
        pack()
    }

    override fun showHasDifferences(differencesCount: Int) {
        val pixelText = if (differencesCount > 1) "pixels" else "pixel"
        label.text = "There are some differences: $differencesCount $pixelText"
        pack()
    }
}