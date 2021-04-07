package com.github.grishberg.android.layoutinspector.ui.dialogs

import java.awt.BorderLayout
import java.awt.Frame
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JDialog
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.WindowConstants

interface LoadingDialogClosedEventListener {
    fun onLoadingDialogClosed()
}

class LoadingDialog(
    owner: Frame,
    eventListener: LoadingDialogClosedEventListener
) : JDialog(owner, false) {

    init {
        val panel = JPanel()
        panel.layout = BorderLayout(4, 4)
        panel.border = BorderFactory.createEmptyBorder(32, 32, 32, 32)

        val cldr = this.javaClass.classLoader
        val imageURL = cldr.getResource("icons/loading.gif")
        val imageIcon = ImageIcon(imageURL)
        val iconLabel = JLabel()
        iconLabel.setIcon(imageIcon)
        imageIcon.imageObserver = iconLabel

        val label = JLabel("Loading...")
        label.setHorizontalAlignment(JLabel.CENTER);
        iconLabel.setHorizontalAlignment(JLabel.CENTER);
        panel.add(iconLabel, BorderLayout.CENTER)
        panel.add(label, BorderLayout.PAGE_START)
        setContentPane(panel)
        defaultCloseOperation = WindowConstants.DO_NOTHING_ON_CLOSE
        pack()

        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) = Unit

            override fun windowClosing(e: WindowEvent) {
                eventListener.onLoadingDialogClosed()
            }
        })
    }


}