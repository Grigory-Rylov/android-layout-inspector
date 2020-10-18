package com.github.grishberg.android.layoutinspector.ui.dialogs

import com.android.ddmlib.Client

data class ClientWrapper(val client: Client) {
    override fun toString(): String {
        val pkgName = client.clientData.clientDescription

        return pkgName ?: "${client.clientData.pid}"
    }
}