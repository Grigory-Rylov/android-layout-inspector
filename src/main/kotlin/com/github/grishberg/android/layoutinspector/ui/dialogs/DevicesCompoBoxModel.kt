package com.github.grishberg.android.layoutinspector.ui.dialogs

import com.android.ddmlib.IDevice
import javax.swing.MutableComboBoxModel
import javax.swing.event.ListDataListener

class DevicesCompoBoxModel : MutableComboBoxModel<IDevice> {
    private val devices = mutableListOf<IDevice>()
    private var selectedItem: IDevice? = null
    private val listeners = mutableListOf<ListDataListener>()

    override fun setSelectedItem(anItem: Any?) {
        if (anItem == null) {
            selectedItem = null
            return
        }
        if (anItem is IDevice) {
            selectedItem = anItem
            return
        }
        throw IllegalStateException("Trying to add $anItem")
    }

    override fun getSelectedItem(): Any? = selectedItem

    override fun getSize(): Int = devices.size

    override fun addElement(item: IDevice) {
        devices.add(item)
    }

    override fun addListDataListener(l: ListDataListener) {
        listeners.add(l)
    }

    override fun removeListDataListener(l: ListDataListener) {
        listeners.remove(l)
    }

    override fun getElementAt(index: Int): IDevice = devices[index]


    override fun removeElementAt(index: Int) {
        devices.removeAt(index)
    }

    override fun insertElementAt(item: IDevice, index: Int) {
        devices.add(index, item)
    }

    override fun removeElement(obj: Any) {
        devices.remove(obj)
    }

    fun contains(item: IDevice): Boolean = devices.contains(item)
}