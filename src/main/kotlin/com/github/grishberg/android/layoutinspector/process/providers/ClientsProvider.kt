package com.github.grishberg.android.layoutinspector.process.providers

import com.android.ddmlib.Client
import com.android.ddmlib.IDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

class ClientsProvider {
    private val timeout: Long = 30

    suspend fun requestClients(device: IDevice): Array<Client> {
        val clients = GlobalScope.async(Dispatchers.IO) {
            Thread.sleep(2000)
            val clients = device.clients
            return@async clients
        }
        return clients.await()
    }
}
