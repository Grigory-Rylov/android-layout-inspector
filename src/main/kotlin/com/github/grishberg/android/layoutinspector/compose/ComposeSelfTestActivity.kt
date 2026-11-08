package com.github.grishberg.android.layoutinspector.compose

import com.android.ddmlib.AndroidDebugBridge
import com.android.ddmlib.IDevice
import org.jetbrains.android.sdk.AndroidSdkUtils
import com.android.layoutinspector.LayoutInspectorCaptureOptions
import com.android.layoutinspector.common.AdbFacade
import com.android.layoutinspector.common.PluginLogger
import com.android.layoutinspector.model.DisplayInfo
import com.android.layoutinspector.model.LayoutFileData
import com.android.layoutinspector.model.ViewNode
import com.github.grishberg.android.layoutinspector.domain.AbstractViewNode
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposableRoot
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposeNode
import com.github.grishberg.android.layoutinspector.process.providers.DeviceProvider
import com.github.grishberg.android.layoutinspector.ui.OpenWindowMode
import com.github.grishberg.android.layoutinspector.ui.WindowsManager
import com.github.grishberg.android.li.PluginState
import com.intellij.openapi.application.ex.ApplicationManagerEx
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.delay
import java.awt.image.BufferedImage
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.atomic.AtomicBoolean
import javax.imageio.ImageIO
import javax.swing.SwingUtilities

/**
 * Headless self-test hook for the Compose capture pipeline (E2E verification of the whole
 * App Inspection flow without clicking through the YALI dialog).
 *
 * Activation: launch the sandbox IDE with `-Dyali.compose.selftest=/tmp/yali-selftest.json`
 * (`./gradlew runIde -Pyali.selftest=/tmp/yali-selftest.json`). The property is absent in normal
 * sessions, so this activity is a no-op there.
 *
 * When active: waits for the first debuggable process visible to App Inspection (preferring the
 * device in `-Dyali.compose.selftest.device`, default [DEFAULT_SELFTEST_DEVICE]), captures the
 * compose hierarchy, writes the tree as JSON to the property path and exits the IDE with code 0.
 * Any failure is written to `<path>.err` (message + stack trace) and the IDE exits with code 1.
 */
class ComposeSelfTestActivity : ProjectActivity {

    companion object {
        const val PROPERTY = "yali.compose.selftest"
        const val DEVICE_PROPERTY = "yali.compose.selftest.device"
        const val PROCESS_PROPERTY = "yali.compose.selftest.process"

        /**
         * Direct production-path capture: `serial:pid|package` — calls capture() for that exact
         * process (not discovery-first), exercising the invisible-process auto-restart fallback
         * exactly like the New Layout dialog flow does.
         */
        const val DIRECT_PROPERTY = "yali.compose.selftest.direct"

        /**
         * Seconds to keep the real YALI result window open after capture (compose tree rendered
         * over a live device screenshot). External `import`/`xwd` screenshot of this window is
         * visual E2E proof. Absent/0: headless JSON-only selftest.
         */
        const val UI_PROPERTY = "yali.compose.selftest.ui"
        const val DEFAULT_SELFTEST_DEVICE = "192.168.1.102:5555"
        private val started = AtomicBoolean(false)
    }

    // 2025.3 platform: ProjectActivity's suspend entry point is `execute` (was `run` in older SDKs)
    override suspend fun execute(project: Project) {
        val path = System.getProperty(PROPERTY) ?: return
        if (!started.compareAndSet(false, true)) {
            return
        }
        val logger = PluginLogger()
        logger.d("YALI Compose selftest: starting, output=$path")
        val preferredDevice = System.getProperty(DEVICE_PROPERTY) ?: DEFAULT_SELFTEST_DEVICE
        val preferredProcess = System.getProperty(PROCESS_PROPERTY)
        try {
            val capture = System.getProperty(DIRECT_PROPERTY)?.let { spec ->
                runDirectCapture(spec, project, logger)
            } ?: ComposeCaptureService(logger).captureFirstAvailable(
                preferredDevice, preferredProcess, project
            )
            val mergedTree = MergedTreeProbe.buildMergedSample(capture)
            val json = ComposeSelfTestJson.toJson(capture, mergedTree)
            File(path).writeText(json, Charsets.UTF_8)
            val uiSeconds = System.getProperty(UI_PROPERTY)?.toIntOrNull() ?: 0
            if (uiSeconds > 0) {
                runUiProof(capture, project, uiSeconds, logger)
            }
            exit(0)
        } catch (t: Throwable) {
            logger.e("YALI Compose selftest: failed", t)
            try {
                val stack = StringWriter()
                t.printStackTrace(PrintWriter(stack))
                File("$path.err").writeText("$t\n$stack", Charsets.UTF_8)
            } catch (ioError: Throwable) {
                logger.e("YALI Compose selftest: cannot write $path.err", ioError)
            }
            exit(1)
        }
    }


    private suspend fun runDirectCapture(
        spec: String,
        project: Project,
        logger: PluginLogger,
    ): ComposeCaptureService.ComposeCaptureResult {
        // `serial:pid|package`; network serials contain ':' themselves -> split from the right.
        val specWithoutPkg = spec.substringBefore('|')
        val pkg = spec.substringAfter('|', "").ifEmpty { null }
        val serial = specWithoutPkg.substringBeforeLast(':')
        val pid = specWithoutPkg.substringAfterLast(':').toInt()
        // Get the same ddmlib bridge the production dialog flows use (AdbWrapperImpl route:
        // AndroidSdkUtils.getDebugBridge resolves AS's own initialized bridge; the extra ddmlib
        // and profiler lookups are fallbacks). ADB init is async in the sandbox -> poll.
        val deadline = System.currentTimeMillis() + 90_000
        var device: IDevice? = null
        while (device == null && System.currentTimeMillis() < deadline) {
            val candidates: List<AndroidDebugBridge?> = listOfNotNull(
                runCatching { AndroidSdkUtils.getDebugBridge(project) }.getOrNull(),
                AndroidDebugBridge.getBridge(),
                runCatching {
                    Class.forName("com.android.tools.profiler.AndroidProfiler")
                        .getMethod("getDebugBridge")
                        .invoke(null) as? AndroidDebugBridge
                }.getOrNull(),
            ).distinctBy { System.identityHashCode(it) }
            device = candidates.asSequence().filterNotNull()
                .flatMap { it.devices.asSequence() }
                .firstOrNull { it.serialNumber == serial }
            if (device == null) {
                delay(500L)
            }
        }
        logger.d("YALI Compose selftest: direct capture serial=$serial pid=$pid pkg=$pkg " +
            "idDevice=${device != null}")
        return ComposeCaptureService(logger).capture(serial, pid, project, device, pkg)
    }

    private fun exit(code: Int) {
        // let the log line reach the console before teardown
        Thread.sleep(1_000L)
        // Guarantee the headless run terminates so `runIde` returns: ApplicationManagerEx.exit
        // can stall under Xvfb waiting on background teardown. The JSON/.err file is already
        // flushed, so a hard halt is safe here (self-test process only).
        Thread {
            Thread.sleep(5_000L)
            Runtime.getRuntime().halt(code)
        }.apply { isDaemon = true; start() }
        val applicationEx = ApplicationManagerEx.getApplicationEx()
        if (applicationEx != null) {
            applicationEx.exit(code)
        } else {
            Runtime.getRuntime().exit(code)
        }
    }

    /**
     * Opens the production YALI result window (ui/Main) with the captured compose tree grafted
     * under a ComposeView host and a live device screenshot as preview - exactly what a user sees
     * after a real capture. An external screenshot of this window is end-to-end proof.
     */
    private suspend fun runUiProof(
        capture: ComposeCaptureService.ComposeCaptureResult,
        project: Project,
        keepSeconds: Int,
        logger: PluginLogger,
    ) {
        val preview = deviceScreenshot(capture.deviceSerial, logger)
        val mergedRoot = MergedTreeProbe.buildMergedRoot(capture)
            ?: throw IllegalStateException("no compose roots to show")
        SwingUtilities.invokeAndWait {
            val manager = WindowsManager(logger)
            val baseDir = File(
                project.basePath ?: System.getProperty("java.io.tmpdir"),
                "captures/YALI",
            ).apply { mkdirs() }
            val main = manager.createWindow(
                OpenWindowMode.DEFAULT,
                PluginState(),
                StubDeviceProvider,
                StubAdbFacade,
                baseDir,
            )
            main.showResult(
                LayoutFileData(preview, mergedRoot, LayoutInspectorCaptureOptions()),
                "YALI compose E2E proof",
                false,
            )
            main.toFront()
        }
        logger.d("YALI Compose selftest: result window shown, keeping ${keepSeconds}s")
        delay(keepSeconds * 1000L)
    }

    private fun deviceScreenshot(serial: String, logger: PluginLogger): BufferedImage? {
        val sdk = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        val adb = if (sdk != null) File(sdk, "platform-tools/adb").absolutePath else "adb"
        return runCatching {
            ProcessBuilder(adb, "-s", serial, "exec-out", "screencap", "-p")
                .start().inputStream.use { ImageIO.read(it) }
        }.onFailure { logger.e("YALI Compose selftest: screencap failed", it) }.getOrNull()
    }
}

/** No-op providers: the proof window never captures, it only renders an already-captured tree. */
private object StubDeviceProvider : DeviceProvider {
    override fun stop() = Unit
    override fun reconnect() = Unit
    override suspend fun requestDevices(): List<IDevice> = emptyList()
    override val deviceChangedActions = mutableSetOf<DeviceProvider.DeviceChangedAction>()
    override val isReconnectionAllowed = false
}

private object StubAdbFacade : AdbFacade {
    override fun isConnected() = false
    override fun getDevices(): List<IDevice> = emptyList()
    override fun hasInitialDeviceList() = false
    override fun connect() = Unit
    override fun connect(remoteAddress: String) = Unit
    override fun stop() = Unit
}

/**
 * Exercises [ComposeTreeMerger]/[ComposeNodeAdapter] end-to-end on the real captured data: builds
 * the minimal YALI-side view tree (DecorView -> ComposeView host per compose root, host bounds =
 * bounds of the root's top-level nodes, which are absolute screen pixels), merges and serializes
 * the result as `depth|name|x,y,wxh` lines. Failing here means the grafted tree would be wrong
 * or invisible in the UI even though the capture itself succeeded.
 */
internal object MergedTreeProbe {

    private const val DECOR = "com.android.internal.policy.DecorView"
    private const val COMPOSE_VIEW = "androidx.compose.ui.platform.ComposeView"

    /** Builds the merged tree (compose grafted under ComposeView) or null if no roots. */
    fun buildMergedRoot(capture: ComposeCaptureService.ComposeCaptureResult): AbstractViewNode? {
        if (capture.roots.isEmpty()) {
            return null
        }
        val allBounds = capture.roots.flatMap { root -> root.nodes.map { it.bounds } }
        val decor = node(
            parent = null, name = DECOR,
            left = 0, top = 0,
            width = allBounds.maxOf { it.right }.coerceAtLeast(1),
            height = allBounds.maxOf { it.bottom }.coerceAtLeast(1),
        )
        capture.roots.forEachIndexed { index, root ->
            val first = root.nodes.firstOrNull()?.bounds ?: return@forEachIndexed
            val host = node(
                parent = decor, name = COMPOSE_VIEW,
                left = first.left, top = first.top,
                width = first.right - first.left, height = first.bottom - first.top,
            )
            @Suppress("UNCHECKED_CAST")
            (decor.children as MutableList<AbstractViewNode>).add(host)
        }
        return ComposeTreeMerger(PluginLogger()).mergeNodes(decor, capture.roots, capture.semanticTexts)
    }

    fun buildMergedSample(capture: ComposeCaptureService.ComposeCaptureResult): List<String> {
        val merged = buildMergedRoot(capture) ?: return emptyList()
        val rows = mutableListOf<String>()
        walk(merged, 0, rows)
        return rows
    }

    private fun node(
        parent: ViewNode?,
        name: String,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
    ) = ViewNode(
        parent = parent,
        name = name,
        hash = "probe-$name",
        displayInfo = DisplayInfo(
            false, true, left, top, width, height, 0, 0, true, 0f, 0f, 0f, 0f, null,
        ),
    )

    private fun walk(node: AbstractViewNode, depth: Int, out: MutableList<String>) {
        // absolute screen coordinates: location properties / parent accumulation, like the UI uses
        val bounds = "|${node.locationOnScreenX},${node.locationOnScreenY},${node.width},${node.height}"
        out += "$depth|${node.name}$bounds"
        node.children.forEach { walk(it, depth + 1, out) }
    }
}

/** Tiny hand-rolled JSON writer (no new dependencies) for the self-test artifact. */
internal object ComposeSelfTestJson {

    fun toJson(
        result: ComposeCaptureService.ComposeCaptureResult,
        mergedTree: List<String>,
    ): String {
        val sb = StringBuilder(16 * 1024)
        sb.append("{\n")
        sb.append("  \"device\": ").append(quote(result.deviceSerial)).append(",\n")
        sb.append("  \"pid\": ").append(result.pid).append(",\n")
        sb.append("  \"process\": ").append(quote(result.processName)).append(",\n")
        sb.append("  \"composeVersion\": ").append(quote(result.composeVersion)).append(",\n")
        sb.append("  \"rootCount\": ").append(result.roots.size).append(",\n")
        sb.append("  \"requestedRootViewIds\": [")
        result.requestedRootViewIds.forEachIndexed { i, id ->
            if (i > 0) {
                sb.append(", ")
            }
            sb.append(id)
        }
        sb.append("],\n")
        sb.append("  \"mergedTree\": [\n")
        mergedTree.forEachIndexed { i, row ->
            if (i > 0) {
                sb.append(",\n")
            }
            sb.append("    ").append(quote(row))
        }
        sb.append("\n  ],\n")
        appendList(sb, result.roots, "  ", ::appendRoot)
        sb.append("\n}\n")
        return sb.toString()
    }

    private inline fun <T> appendList(
        sb: StringBuilder,
        items: List<T>,
        indent: String,
        appendItem: (StringBuilder, T, String) -> Unit,
    ) {
        sb.append(indent).append("\"roots\": ")
        if (items.isEmpty()) {
            sb.append("[]")
            return
        }
        sb.append("[\n")
        items.forEachIndexed { index, item ->
            if (index > 0) {
                sb.append(",\n")
            }
            appendItem(sb, item, "$indent  ")
        }
        sb.append("\n").append(indent).append("]")
    }

    private fun appendRoot(sb: StringBuilder, root: ComposableRoot, indent: String) {
        sb.append(indent).append("{\n")
        sb.append(indent).append("  \"viewId\": ").append(root.viewId).append(",\n")
        sb.append(indent).append("  \"nodeCount\": ").append(countNodes(root.nodes)).append(",\n")
        sb.append(indent).append("  \"viewsToSkip\": [")
        root.viewsToSkip.forEachIndexed { i, id ->
            if (i > 0) {
                sb.append(", ")
            }
            sb.append(id)
        }
        sb.append("],\n")
        sb.append(indent).append("  \"nodes\": ")
        if (root.nodes.isEmpty()) {
            sb.append("[]\n")
        } else {
            sb.append("[\n")
            root.nodes.forEachIndexed { index, node ->
                if (index > 0) {
                    sb.append(",\n")
                }
                appendNode(sb, node, "$indent    ")
            }
            sb.append("\n").append(indent).append("]\n")
        }
        sb.append(indent).append("}")
    }

    /** Total composable count of a root (recursively). */
    private fun countNodes(nodes: List<ComposeNode>): Int =
        nodes.sumOf { 1 + countNodes(it.children) }

    private fun appendNode(sb: StringBuilder, node: ComposeNode, indent: String) {
        sb.append(indent).append("{\n")
        sb.append(indent).append("  \"id\": ").append(node.id).append(",\n")
        sb.append(indent).append("  \"name\": ").append(quote(node.name)).append(",\n")
        sb.append(indent).append("  \"file\": ").append(quote(node.file)).append(",\n")
        sb.append(indent).append("  \"line\": ").append(node.line).append(",\n")
        sb.append(indent).append("  \"offset\": ").append(node.offset).append(",\n")
        sb.append(indent).append("  \"bounds\": {\"x\": ").append(node.bounds.x)
            .append(", \"y\": ").append(node.bounds.y)
            .append(", \"w\": ").append(node.bounds.width)
            .append(", \"h\": ").append(node.bounds.height).append("},\n")
        sb.append(indent).append("  \"flags\": ").append(node.flags).append(",\n")
        sb.append(indent).append("  \"recomposeCount\": ").append(node.recomposeCount).append(",\n")
        sb.append(indent).append("  \"viewId\": ").append(node.viewId)
        if (node.children.isEmpty()) {
            sb.append("\n").append(indent).append("}")
        } else {
            sb.append(",\n").append(indent).append("  \"children\": [\n")
            node.children.forEachIndexed { index, child ->
                if (index > 0) {
                    sb.append(",\n")
                }
                appendNode(sb, child, "$indent    ")
            }
            sb.append("\n").append(indent).append("  ]\n").append(indent).append("}")
        }
    }

    private fun quote(value: String): String {
        val sb = StringBuilder(value.length + 8)
        sb.append('"')
        value.forEach { c ->
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c.code < 0x20 -> sb.append("\\u%04x".format(c.code))
                else -> sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }
}
