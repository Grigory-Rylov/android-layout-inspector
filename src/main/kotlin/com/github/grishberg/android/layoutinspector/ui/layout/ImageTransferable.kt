package com.github.grishberg.android.layoutinspector.ui.layout

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.image.BufferedImage
import java.io.IOException

class ImageTransferable(val image: BufferedImage) : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return arrayOf(DataFlavor.imageFlavor)
    }

    override fun isDataFlavorSupported(flavor: DataFlavor): Boolean {
        return DataFlavor.imageFlavor.equals(flavor)
    }

    @kotlin.Throws(UnsupportedFlavorException::class, IOException::class)
    override fun getTransferData(flavor: DataFlavor): Any {
        if (isDataFlavorSupported(flavor)) {
            return image
        }
        throw UnsupportedFlavorException(flavor)
    }
}