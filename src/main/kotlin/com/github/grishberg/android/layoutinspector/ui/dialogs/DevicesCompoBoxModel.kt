package com.github.grishberg.android.layoutinspector.ui.dialogs

import com.android.ddmlib.IDevice
import javax.swing.MutableComboBoxModel
import javax.swing.event.ListDataListener

interface DeviceWrapper {
    val device: IDevice
}

class RealDeviceWrapper(override val device: IDevice) : DeviceWrapper {
    override fun toString(): String {
        return device.toString()
    }

    override fun equals(other: Any?): Boolean {
        if (other is RealDeviceWrapper) {
            return device.serialNumber == other.device.serialNumber
        }
        return false
    }
}

class DevicesCompoBoxModel : MutableComboBoxModel<DeviceWrapper> {
    private val devices = mutableListOf<DeviceWrapper>()
    private var selectedItem: DeviceWrapper? = null
    private val listeners = mutableListOf<ListDataListener>()

    override fun setSelectedItem(anItem: Any?) {
        if (anItem == null) {
            selectedItem = null
            return
        }
        if (anItem is DeviceWrapper) {
            selectedItem = anItem
            return
        }
        throw IllegalStateException("Trying to add $anItem")
    }

    override fun getSelectedItem(): Any? = selectedItem

    override fun getSize(): Int = devices.size

    override fun addElement(item: DeviceWrapper) {
        devices.add(item)
    }

    override fun addListDataListener(l: ListDataListener) {
        listeners.add(l)
    }

    override fun removeListDataListener(l: ListDataListener) {
        listeners.remove(l)
    }

    override fun getElementAt(index: Int): DeviceWrapper = devices[index]


    override fun removeElementAt(index: Int) {
        devices.removeAt(index)
    }

    override fun insertElementAt(item: DeviceWrapper, index: Int) {
        devices.add(index, item)
    }

    override fun removeElement(obj: Any) {
        devices.remove(obj)
    }

    fun contains(item: IDevice): Boolean {
        for (d in devices) {
            if (d.device.serialNumber == item.serialNumber) {
                return true
            }
        }
        return false
    }
}