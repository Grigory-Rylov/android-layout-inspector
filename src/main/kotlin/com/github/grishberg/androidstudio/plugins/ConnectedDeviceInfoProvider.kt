package com.github.grishberg.androidstudio.plugins

import com.android.ddmlib.IDevice
import com.android.tools.idea.gradle.project.model.AndroidModuleModel
import com.android.tools.idea.run.ApkProviderUtil
import com.github.grishberg.androidstudio.plugins.ui.DeviceChooserDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import org.jetbrains.android.facet.AndroidFacet
import org.jetbrains.android.util.AndroidUtils

data class ConnectedDeviceInfo(
    val devices: List<IDevice>,
    val facet: AndroidFacet,
    val packageName: String?
)

class ConnectedDeviceInfoProvider(
    private val project: Project,
    private val adb: AdbWrapper,
    private val notificationHelper: NotificationHelper
) {
    fun provideDeviceInfo(): ConnectedDeviceInfo? {
        val facets = AndroidUtils.getApplicationFacets(project)
        if (facets.isNotEmpty()) {
            val facet = getFacet(facets) ?: return null
            var packageName: String? = null
            try {
                packageName = ApkProviderUtil.computePackageName(facet) ?: AndroidModuleModel.get(facet)?.applicationId ?: return null
            } catch (e: Throwable ) {
                e.printStackTrace()
            }

            if (!adb.isReady()) {
                notificationHelper.error("No platform configured")
                return null
            }

            /*
            val rememberedDevices = useSameDevicesHelper.getRememberedDevices()
            if (rememberedDevices.isNotEmpty()) {
                return ConnectedDeviceInfo(rememberedDevices, facet, packageName)
            }
             */

            val devices = adb.connectedDevices()
            if (devices.size == 1) {
                return ConnectedDeviceInfo(devices, facet, packageName)
            } else if (devices.size > 1) {
                return showDeviceChooserDialog(facet, packageName)
            } else {
                return null
            }
        }
        return null
    }

    private fun getFacet(facets: List<AndroidFacet>): AndroidFacet? {
        val facet: AndroidFacet?
        if (facets.size > 1) {
            facet = ModuleChooserDialogHelper.showDialogForFacets(project, facets)
            if (facet == null) {
                return null
            }
        } else {
            facet = facets[0]
        }

        return facet
    }

    private fun showDeviceChooserDialog(facet: AndroidFacet, packageName: String?): ConnectedDeviceInfo? {
        val chooser = DeviceChooserDialog(facet)
        chooser.show()

        if (chooser.exitCode != DialogWrapper.OK_EXIT_CODE) {
            return null
        }


        val selectedDevices = chooser.selectedDevices

        /*
        if (chooser.useSameDevices()) {
            useSameDevicesHelper.rememberDevices()
        }
         */

        if (selectedDevices.isEmpty()) {
            return null
        }

        return ConnectedDeviceInfo(selectedDevices.asList(), facet, packageName)
    }
}
