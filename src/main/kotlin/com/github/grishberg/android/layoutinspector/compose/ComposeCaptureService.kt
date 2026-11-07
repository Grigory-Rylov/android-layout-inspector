package com.github.grishberg.android.layoutinspector.compose

import com.android.layoutinspector.common.AppLogger
import com.android.tools.idea.appinspection.api.AppInspectionApiServices
import com.android.tools.idea.appinspection.api.process.ProcessListener
import com.android.tools.idea.appinspection.ide.AppInspectionDiscoveryService
import com.android.tools.idea.appinspection.ide.InspectorArtifactService
import com.android.tools.idea.appinspection.inspector.api.AppInspectorJar
import com.android.tools.idea.appinspection.inspector.api.AppInspectorMessenger
import com.android.tools.idea.appinspection.inspector.api.launch.LaunchParameters
import com.android.tools.idea.appinspection.inspector.api.launch.LibraryCompatibility
import com.android.tools.idea.appinspection.inspector.api.launch.LibraryCompatibilityInfo
import com.android.tools.idea.appinspection.inspector.api.launch.MinimumArtifactCoordinate
import com.android.tools.idea.appinspection.inspector.api.launch.RunningArtifactCoordinate
import com.android.tools.idea.appinspection.inspector.api.process.ProcessDescriptor
import com.android.ddmlib.CollectingOutputReceiver
import com.android.ddmlib.IDevice
import com.android.tools.idea.appinspection.internal.AppInspectionTarget
import com.android.tools.idea.layoutinspector.pipeline.appinspection.view.ViewLayoutInspectorClient
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposableRoot
import com.github.grishberg.android.layoutinspector.compose.ComposeInspectionCodec.ComposeResponse
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.withTimeout
import com.android.tools.idea.appinspection.inspector.api.AppInspectionAgentUnattachableException
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executor
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

/** Every failure of the compose capture pipeline surfaces as this exception with actionable text. */
class ComposeCaptureException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Captures the Jetpack Compose hierarchy of a running *debuggable* process by reusing Android
 * Studio's own App Inspection machinery (this plugin runs inside Android Studio):
 *
 *  1. `AppInspectionDiscoveryService` -> `AppInspectionApiServices` (application service);
 *  2. find the [ProcessDescriptor] matching device serial + pid via `ProcessDiscovery` listeners
 *     (process discovery only sees debuggable apps - the profiler agent is what exposes them);
 *  3. `attachToProcess(...)` + `getLibraryVersions(...)` to read the *running app's*
 *     androidx.compose.ui version;
 *  4. resolve the matching inspector.jar with `InspectorArtifactService.getOrResolveInspectorArtifact`
 *     (downloads androidx.compose.ui:ui(-android):<version> AAR and extracts root inspector.jar,
 *     exactly like AS does; artifact naming boundary: `ui-android` since the 1.5.0-beta01 KMP
 *     migration - mirrors ComposeLayoutInspectorClient.determineArtifactCoordinate, with a fallback
 *     to the other coordinate when resolution fails);
 *  5. bootstrap the **view** inspector (`layoutinspector.view.inspection`, jar shipped at
 *     `<installRoot>/plugins/android/resources/app-inspection/layoutinspector-view-inspection.jar`,
 *     library compatibility = null like AS) and read the window root view ids from its event flow
 *     (WindowRootsEvent.ids, falling back to StartFetchCommand -> LayoutEvent.root_view.node.id);
 *     the compose agent keys its tree map by View.getUniqueDrawingId() and answers
 *     `root_view_id=0` with an EMPTY tree (verified E2E), so these ids are mandatory input;
 *  6. stopInspectors, then `launchInspector` with inspector id `layoutinspector.compose.inspection`
 *     and `sendRawCommand(Command{get_composables_command{root_view_id=<id>, generation=0,
 *     skip_system_composables=true}})` per root id, concatenating the non-empty responses;
 *     the compose [ComposeInspectionCodec]/Response bytes travel raw; the RawCommand /
 *     AppInspectionCommand envelope is applied *inside* DefaultAppInspectionTransport (verified from
 *     AppInspectorConnection bytecode: the payload becomes RawCommand{content} and the returned
 *     bytes are RawResponse{content}), so YALI passes/reads only the packed compose messages;
 *  7. `stopInspectors` in finally.
 *
 * All calls are suspend and must run off the EDT (callers use a worker dispatcher).
 *
 * NOTE on process discovery: `addProcessListener` immediately replays already-discovered processes
 * to the new listener (verified from AppInspectionProcessDiscovery bytecode), so no polling is
 * needed for processes the profiler already knows about; the timeout only bounds that first
 * profiler discovery after IDE start.
 */
class ComposeCaptureService(private val logger: AppLogger) {

    /** Rotating generation for get_composables_command; see [requestComposables]. */
    private val generationCounter = AtomicInteger()

    companion object {
        const val COMPOSE_INSPECTOR_ID = "layoutinspector.compose.inspection"
        private const val EXPECTED_CLASS_IN_COMPOSE_LIBRARY = "androidx.compose.ui.Modifier"
        private const val COMPOSE_GROUP_ID = "androidx.compose.ui"

        /** KMP migration: inspector.jar ships inside ui-android since 1.5.0-beta01 (AS boundary). */
        private const val KMP_MIGRATION_VERSION = "1.5.0-beta01"

        /**
         * Second inspector needed to learn the window root View.getUniqueDrawingId() values: the
         * compose agent answers `map.get(root_view_id)`, so `root_view_id=0` yields an EMPTY tree
         * regardless of generation (verified E2E 2026-11-07 against the compose agent).
         */
        const val VIEW_INSPECTOR_ID = "layoutinspector.view.inspection"
        private const val VIEW_INSPECTOR_JAR_NAME = "layoutinspector-view-inspection.jar"

        /** Event-flow collection windows for the view-inspector bootstrap (ms). */
        const val ROOTS_EVENT_WINDOW_MS = 4_000L
        private const val ATTACH_RETRY_LIMIT = 4
        private const val ATTACH_RETRY_DELAY_MS = 4_000L
        const val FETCH_EVENT_WINDOW_MS = 10_000L

        private const val PROCESS_DISCOVERY_TIMEOUT_MS = 15_000L
        private const val RELAUNCH_DISCOVERY_TIMEOUT_MS = 20_000L
        private const val COMMAND_TIMEOUT_MS = 30_000L

        /**
         * Compose capture needs Android Studio's App Inspection services; used to enable/disable
         * the UI affordance outside the AS IDE (or in broken plugin setups).
         */
        fun isAvailable(): Boolean = try {
            ApplicationManager.getApplication()
                ?.getService(AppInspectionDiscoveryService::class.java) != null
        } catch (t: Throwable) {
            false
        }

        private val DIRECT_EXECUTOR = Executor { command -> command.run() }
    }

    data class ComposeCaptureResult(
        val deviceSerial: String,
        val pid: Int,
        val processName: String,
        val composeVersion: String,
        val roots: List<ComposableRoot>,
        /** Window root view ids (View.getUniqueDrawingId) the compose agent was queried with. */
        val requestedRootViewIds: List<Long> = emptyList(),
    )

    /**
     * Single-shot capture for the debuggable process [pid] on device [deviceSerial].
     * @param project used for inspector artifact resolution (progress/state); any open project is
     *   picked when null.
     * @throws ComposeCaptureException with a user-presentable, actionable message.
     */
    suspend fun capture(
        deviceSerial: String,
        pid: Int,
        project: Project? = null,
        device: IDevice? = null,
        packageName: String? = null,
    ): ComposeCaptureResult {
        val apiServices = requireApiServices()
        val (process, relaunched) = findProcess(apiServices, deviceSerial, pid, device, packageName)

        try {
            val resolvedProject = resolveProject(project)

            val target = attachWithRetry(apiServices, process, relaunched)

            // The compose agent only answers for real window-root uniqueDrawingIds — learn them
            // from the view inspector first (class doc step 5); its inspector is stopped again
            // before the compose one is launched. Runs after attachToProcess: as in AS's
            // pipeline, no inspector can launch against an unattached process.
            val rootViewIds = bootstrapWindowRootViewIds(apiServices, process, resolvedProject.name)

            val composeVersion = detectComposeVersion(target)
            val (jar, coordinate) = resolveInspectorJar(composeVersion, resolvedProject)
            logger.d(
                "YALI Compose: inspector jar ${jar.name} for ${coordinate.toCoordinateString()} $composeVersion"
            )

            // NOTE: the LibraryCompatibility coordinate is checked *inside the app* by the
            // app-inspection agent against META-INF/<group>_<artifact>.version of the APK. KMP
            // builds publish the runtime artifact as ui-android but the APK version file keeps the
            // module identity "ui" (androidx.compose.ui_ui.version), so the compatibility
            // coordinate must always be COMPOSE_UI — exactly like Android Studio does. The
            // resolved jar coordinate (possibly ui-android) only affects maven artifact download.
            val messenger = runCatchingSuspend("launch compose inspector") {
                apiServices.launchInspector(
                    LaunchParameters(
                        process,
                        COMPOSE_INSPECTOR_ID,
                        jar,
                        resolvedProject.name,
                        LibraryCompatibility(
                            MinimumArtifactCoordinate.COMPOSE_UI,
                            listOf(EXPECTED_CLASS_IN_COMPOSE_LIBRARY),
                        ),
                        false,
                    )
                )
            }.getOrThrow()

            val roots = requestComposables(messenger, rootViewIds)
            logger.d("YALI Compose: captured ${roots.size} compose root(s) for ids $rootViewIds")
            return ComposeCaptureResult(
                deviceSerial = deviceSerial,
                pid = process.pid,
                processName = process.name,
                composeVersion = composeVersion,
                roots = roots,
                requestedRootViewIds = rootViewIds,
            )
        } catch (t: Throwable) {
            if (t is ComposeCaptureException) {
                throw t
            }
            throw ComposeCaptureException(
                "Jetpack Compose capture failed for ${process.packageName} (pid=$pid): ${t.message}", t
            )
        } finally {
            runCatching { apiServices.stopInspectors(process) }
                .onFailure { logger.w("YALI Compose: could not stop inspectors for pid=$pid: ${it.message}") }
        }
    }

    /**
     * Lists debuggable processes currently visible to App Inspection (registration replays the
     * already-known ones, then `collectWindowMs` collects further events). Used by self-test.
     */
    suspend fun discoverProcesses(collectWindowMs: Long = 2_000L): List<ProcessDescriptor> {
        val apiServices = requireApiServices()
        return collectProcesses(apiServices, collectWindowMs)
    }

    /**
     * Self-test helper: tries debuggable processes on [preferredDeviceSerial] (else any device),
     * optionally filtered by package name, until one captures successfully. Individual failures
     * (e.g. system processes reported as 'unattachable') are skipped; the last collected errors
     * are reported only when every candidate fails.
     */
    suspend fun captureFirstAvailable(
        preferredDeviceSerial: String?,
        preferredProcessName: String? = null,
        project: Project? = null,
        overallTimeoutMs: Long = 120_000L,
    ): ComposeCaptureResult {
        val deadline = System.currentTimeMillis() + overallTimeoutMs
        var lastSeen: List<ProcessDescriptor> = emptyList()
        val failures = mutableListOf<String>()
        val tried = mutableSetOf<Int>()
        while (System.currentTimeMillis() < deadline) {
            val apiServices = requireApiServices()
            lastSeen = collectProcesses(apiServices, 2_000L)
                .filter { it.isRunning }
                .filter { process ->
                    preferredDeviceSerial == null || process.device.serial == preferredDeviceSerial
                }
                .filter { process ->
                    preferredProcessName == null || process.packageName == preferredProcessName
                }
            for (candidate in lastSeen.filter { tried.add(it.pid) }) {
                try {
                    return capture(
                        candidate.device.serial,
                        candidate.pid,
                        project,
                        packageName = candidate.packageName,
                        // no IDevice here: candidates are already visible to App Inspection,
                        // the restart fallback can never trigger and self-test has none handy
                    )
                } catch (t: Throwable) {
                    logger.w(
                        "YALI Compose: candidate ${candidate.packageName} (pid=${candidate.pid}) " +
                            "failed: ${t.message}"
                    )
                    failures += "${candidate.packageName}/${candidate.pid}: ${t.message}"
                }
            }
            delay(1_000L)
        }
        val devices = lastSeen.map { it.device.serial }.distinct()
        throw ComposeCaptureException(
            "No Jetpack Compose process could be captured" +
                (if (preferredDeviceSerial != null) " on device $preferredDeviceSerial" else "") +
                (if (preferredProcessName != null) " with package $preferredProcessName" else "") +
                ". Seen devices: ${if (devices.isEmpty()) "<none>" else devices.joinToString()}. " +
                "Is the app installed with android:debuggable=\"true\" and running?" +
                (if (failures.isEmpty()) "" else "\nCandidate failures:\n${failures.joinToString("\n")}")
        )
    }

    // ==== internals ====

    private fun requireApiServices(): AppInspectionApiServices {
        val application = ApplicationManager.getApplication()
            ?: throw ComposeCaptureException(
                "Jetpack Compose capture must run inside Android Studio (no IntelliJ application found)."
            )
        val discovery = try {
            application.getService(AppInspectionDiscoveryService::class.java)
        } catch (t: Throwable) {
            throw ComposeCaptureException(
                "Android Studio App Inspection services are not available in this IDE.", t
            )
        } ?: throw ComposeCaptureException(
            "Android Studio App Inspection discovery service is not registered in this IDE."
        )
        return discovery.apiServices
    }

    private suspend fun findProcess(
        apiServices: AppInspectionApiServices,
        serial: String,
        pid: Int,
        device: IDevice?,
        packageName: String?,
    ): Pair<ProcessDescriptor, Boolean> {
        // 1) listener registration replays already-discovered processes synchronously
        val known = collectProcesses(apiServices, 250L).firstOrNull { process ->
            process.device.serial == serial && process.pid == pid && process.isRunning
        }
        if (known != null) {
            return known to false
        }
        // 2) the exact pid is not published (yet) - wait for its connected event
        val connected = withTimeoutOrNullCompat(PROCESS_DISCOVERY_TIMEOUT_MS) {
            awaitConnected(apiServices, serial, pid)
        }
        if (connected != null) {
            return connected to false
        }
        // 3) Studio never saw this process: App Inspection only lists processes whose debug
        //    agent registered while AS tracked the device (AS shows "restart the app" for them).
        //    Do exactly that - restart via adb and capture the freshly registered process.
        if (device == null || packageName == null) {
            throw ComposeCaptureException(notVisibleMessage(pid, serial, attempted = false))
        }
        logger.d(
            "YALI Compose: pid $pid invisible to App Inspection - restarting $packageName " +
                "on $serial so its debug agent registers (same as AS's process-restart flow)"
        )
        runCatchingSuspend<Unit>("restart $packageName on $serial") {
            device.executeShellCommand("am force-stop $packageName", CollectingOutputReceiver())
            delay(500L)
            device.executeShellCommand(
                "monkey -p $packageName -c android.intent.category.LAUNCHER 1",
                CollectingOutputReceiver(),
            )
        }.getOrThrow()
        // The restart only "lands" once App Inspection processed the old process' death: a
        // freshly registered listener replays the registry, which may still hold the stale
        // descriptor until the disconnect event arrives. Wait for it to disappear first.
        withTimeoutOrNullCompat(RELAUNCH_DISCOVERY_TIMEOUT_MS) {
            while (collectProcesses(apiServices, 0L).any {
                    it.device.serial == serial && it.packageName == packageName && it.isRunning
                }
            ) {
                delay(300L)
            }
        }
        val relaunched = withTimeoutOrNullCompat(RELAUNCH_DISCOVERY_TIMEOUT_MS) {
            awaitConnectedPackage(apiServices, serial, packageName, previousPid = pid)
        } ?: throw ComposeCaptureException(
            notVisibleMessage(pid, serial, attempted = true) +
                " Restarted $packageName once, but App Inspection did not pick it up - " +
                "is the installed APK actually debuggable (android:debuggable=\"true\")?"
        )
        logger.d(
            "YALI Compose: $packageName registered after restart with pid=${relaunched.pid}"
        )
        return relaunched to true
    }

    /**
     * `attachToProcess` fails with "The agent is unattachable" while the app-side inspection
     * agent is still booting - routinely true right after YALI relaunched the process (AS's own
     * UI attaches seconds later, after a user click). Retries only for processes YALI restarted
     * itself; a pre-existing process is usually attached instantly and permanently unattachable
     * ones (system/zygote) must not burn the retry budget.
     */
    private suspend fun attachWithRetry(
        apiServices: AppInspectionApiServices,
        process: ProcessDescriptor,
        relaunched: Boolean,
    ): AppInspectionTarget {
        val limit = if (relaunched) ATTACH_RETRY_LIMIT else 1
        var attempt = 0
        while (true) {
            attempt++
            try {
                return apiServices.attachToProcess(process, "YALI compose capture")
            } catch (t: Throwable) {
                val unattachable = generateSequence<Throwable>(t) { it.cause }.any {
                    it is AppInspectionAgentUnattachableException ||
                        it.message?.contains("unattachable", ignoreCase = true) == true
                }
                if (!unattachable || attempt >= limit) {
                    throw ComposeCaptureException(
                        "Failed to attach to process ${process.pid}: ${t.message}", t
                    )
                }
                logger.d(
                    "YALI Compose: agent of pid ${process.pid} not ready " +
                        "(attempt $attempt/$limit) - retrying in ${ATTACH_RETRY_DELAY_MS}ms"
                )
                delay(ATTACH_RETRY_DELAY_MS)
            }
        }
    }

    private fun notVisibleMessage(pid: Int, serial: String, attempted: Boolean) =
        "Process $pid on device $serial is not visible to Android Studio App Inspection" +
            (if (attempted) " (even after an automatic restart)" else "") +
            ". YALI reads the Compose tree through Studio's own inspector, which only sees " +
            "debuggable processes: make sure the app build is debuggable and the process is running."

    /**
     * Same as [awaitConnected] but by package: a restart changes the pid. The listener replay
     * can synchronously push the *old*, just-force-stopped descriptor before its disconnect
     * event arrives, so only a running process with a pid different from [previousPid] counts.
     */
    private suspend fun awaitConnectedPackage(
        apiServices: AppInspectionApiServices,
        serial: String,
        packageName: String,
        previousPid: Int,
    ): ProcessDescriptor? {
        val result = AtomicReference<ProcessDescriptor?>(null)
        val listener = object : ProcessListener {
            override val filter: (ProcessDescriptor) -> Boolean =
                { it.device.serial == serial && it.packageName == packageName }

            override fun onProcessConnected(process: ProcessDescriptor) {
                if (process.isRunning && process.pid != previousPid) {
                    result.set(process)
                }
            }

            override fun onProcessDisconnected(process: ProcessDescriptor) = Unit
        }
        val discovery = apiServices.processDiscovery
        discovery.addProcessListener(DIRECT_EXECUTOR, listener)
        try {
            while (result.get() == null) {
                delay(100L)
            }
        } finally {
            discovery.removeProcessListener(listener)
        }
        return result.get()
    }

    /** Registers a listener and collects everything the discovery service has (and pushes). */
    private suspend fun collectProcesses(
        apiServices: AppInspectionApiServices,
        collectWindowMs: Long,
    ): List<ProcessDescriptor> {
        val found = CopyOnWriteArrayList<ProcessDescriptor>()
        val listener = object : ProcessListener {
            override val filter: (ProcessDescriptor) -> Boolean = { true }
            override fun onProcessConnected(process: ProcessDescriptor) {
                if (!found.contains(process)) {
                    found.add(process)
                }
            }

            override fun onProcessDisconnected(process: ProcessDescriptor) = Unit
        }
        val discovery = apiServices.processDiscovery
        discovery.addProcessListener(DIRECT_EXECUTOR, listener)
        try {
            // give the replay / further events a moment
            delay(if (collectWindowMs > 0) collectWindowMs else 250L)
        } finally {
            discovery.removeProcessListener(listener)
        }
        return found.toList()
    }

    private suspend fun awaitConnected(
        apiServices: AppInspectionApiServices,
        serial: String,
        pid: Int,
    ): ProcessDescriptor? {
        val result = AtomicReference<ProcessDescriptor?>(null)
        val listener = object : ProcessListener {
            override val filter: (ProcessDescriptor) -> Boolean =
                { it.device.serial == serial && it.pid == pid }

            override fun onProcessConnected(process: ProcessDescriptor) {
                result.set(process)
            }

            override fun onProcessDisconnected(process: ProcessDescriptor) = Unit
        }
        val discovery = apiServices.processDiscovery
        discovery.addProcessListener(DIRECT_EXECUTOR, listener)
        try {
            while (result.get() == null) {
                delay(100L)
            }
        } finally {
            discovery.removeProcessListener(listener)
        }
        return result.get()
    }

    private suspend fun detectComposeVersion(target: AppInspectionTarget): String {
        val compatibility = runCatchingSuspend("query library versions of the running process") {
            target.getLibraryVersions(
                listOf(
                    LibraryCompatibility(
                        MinimumArtifactCoordinate.COMPOSE_UI,
                        listOf(EXPECTED_CLASS_IN_COMPOSE_LIBRARY),
                    )
                )
            )
        }.getOrThrow()

        val info: LibraryCompatibilityInfo = compatibility.firstOrNull {
            it.libraryCoordinate.groupId == COMPOSE_GROUP_ID
        } ?: compatibility.firstOrNull() ?: throw ComposeCaptureException(
            "App Inspection reported no library information for the process - " +
                "the app does not seem to include Jetpack Compose (androidx.compose.ui not found)."
        )

        val version = info.version
        return when (info.status) {
            LibraryCompatibilityInfo.Status.COMPATIBLE -> version ?: throw ComposeCaptureException(
                "Jetpack Compose is present but its version could not be determined " +
                    "(the running process did not report a version for ${info.libraryCoordinate})."
            )
            LibraryCompatibilityInfo.Status.LIBRARY_MISSING,
            LibraryCompatibilityInfo.Status.VERSION_MISSING -> throw ComposeCaptureException(
                "The app does not use Jetpack Compose (androidx.compose.ui not found in the process)."
            )
            LibraryCompatibilityInfo.Status.APP_PROGUARDED -> throw ComposeCaptureException(
                "Jetpack Compose inspection is not possible in a minified/proguarded build - " +
                    "capture from a debug build instead."
            )
            else -> throw ComposeCaptureException(
                "The running Jetpack Compose version is not compatible with App Inspection" +
                    (version?.let { " (found $it)" } ?: "") +
                    (info.errorMessage?.let { ": $it" } ?: "") +
                    ". Compose 1.2.1+ (without code shrinking) is required."
            )
        }
    }

    /**
     * Mirrors ComposeLayoutInspectorClient.determineArtifactCoordinate: `ui-android` for
     * >= 1.5.0-beta01, `ui` before; falls back to the other coordinate when resolution fails
     * (odd version string, or maven mirror only carrying one of the artifacts).
     */
    private suspend fun resolveInspectorJar(
        version: String,
        project: Project,
    ): Pair<AppInspectorJar, MinimumArtifactCoordinate> {
        val service = ApplicationManager.getApplication()
            .getService(InspectorArtifactService::class.java)
            ?: throw ComposeCaptureException(
                "Android Studio InspectorArtifactService is unavailable - cannot resolve the compose inspector jar."
            )
        val primary = if (isAtLeast(version, KMP_MIGRATION_VERSION)) {
            MinimumArtifactCoordinate.COMPOSE_UI_ANDROID
        } else {
            MinimumArtifactCoordinate.COMPOSE_UI
        }
        val fallback = if (primary == MinimumArtifactCoordinate.COMPOSE_UI_ANDROID) {
            MinimumArtifactCoordinate.COMPOSE_UI
        } else {
            MinimumArtifactCoordinate.COMPOSE_UI_ANDROID
        }

        var firstError: Throwable? = null
        try {
            return resolveWith(service, primary, version, project) to primary
        } catch (t: Throwable) {
            firstError = t
        }
        try {
            return resolveWith(service, fallback, version, project) to fallback
        } catch (t: Throwable) {
            throw ComposeCaptureException(
                "Could not resolve the compose inspector jar for androidx.compose.ui:$version " +
                    "(tried ${primary.artifactId} and ${fallback.artifactId} on maven.google.com; " +
                    "first failure: ${firstError?.message}; second failure: ${t.message}). " +
                    "Check the network / corporate proxy.", t
            )
        }
    }

    /** Same AppInspectorJar layout AS uses: (fileName, parent dir, parent dir). */
    private suspend fun resolveWith(
        service: InspectorArtifactService,
        coordinate: MinimumArtifactCoordinate,
        version: String,
        project: Project,
    ): AppInspectorJar {
        val path = service.getOrResolveInspectorArtifact(
            RunningArtifactCoordinate(coordinate, version), project
        )
        return AppInspectorJar(
            path.fileName.toString(),
            path.parent.toString(),
            path.parent.toString(),
        )
    }

    /**
     * One `get_composables_command` per window root id, concatenating the non-empty responses.
     * The compose agent answers `map.get(root_view_id)` keyed by View.getUniqueDrawingId, so the
     * ids must come from [bootstrapWindowRootViewIds]; an unknown id simply yields no roots.
     * The generation rotates per command: any value different from the previous one forces the
     * agent to regenerate (proto3 semantics: an equal generation replays the last response).
     */
    private suspend fun requestComposables(
        messenger: AppInspectorMessenger,
        rootViewIds: List<Long>,
    ): List<ComposableRoot> {
        val allRoots = mutableListOf<ComposableRoot>()
        for (rootViewId in rootViewIds) {
            val command = ComposeInspectionCodec.encodeGetComposablesCommand(
                rootViewId = rootViewId,
                skipSystemComposables = true,
                generation = generationCounter.incrementAndGet(),
            )
            val responseBytes = runCatchingSuspend(
                "send get_composables_command for root view $rootViewId"
            ) {
                withTimeout(COMMAND_TIMEOUT_MS) { messenger.sendRawCommand(command) }
            }.getOrThrow()
            when (val response = ComposeInspectionCodec.decodeResponse(responseBytes)) {
                is ComposeResponse.GetComposables -> {
                    logger.d(
                        "YALI Compose: root $rootViewId -> ${response.roots.size} root(s), " +
                            "unchanged=${response.unchanged}"
                    )
                    // dedupe defensively: some agents answer every id with all roots
                    for (root in response.roots) {
                        if (allRoots.none { it.viewId == root.viewId }) allRoots += root
                    }
                }
                is ComposeResponse.UpdateSettings -> Unit // not requested; ignore
                is ComposeResponse.Unknown -> throw ComposeCaptureException(
                    "The compose inspector agent in the app did not understand YALI's request " +
                        "(unknown command). This usually means the app's androidx.compose.ui version " +
                        "is too old for this Android Studio - update Compose or use the AS Layout Inspector."
                )
                else -> throw ComposeCaptureException(
                    "Unexpected response from the compose inspector agent: $response"
                )
            }
        }
        return allRoots
    }

    /**
     * Launches the view inspector, learns the window root ids (View.getUniqueDrawingId) from its
     * event flow and stops the inspector again. AS knows these ids the same way. [collectRootViewIds]
     * does the flow-side work and is unit-testable without a transport.
     */
    private suspend fun bootstrapWindowRootViewIds(
        apiServices: AppInspectionApiServices,
        process: ProcessDescriptor,
        projectName: String,
    ): List<Long> {
        val jar = resolveViewInspectorJar()
        val ids = try {
            val messenger = apiServices.launchInspector(
                LaunchParameters(
                    process,
                    VIEW_INSPECTOR_ID,
                    jar,
                    projectName,
                    // AS passes null library compatibility for the view inspector (verified bytecode).
                    null,
                    false,
                )
            )
            collectRootViewIds(
                events = messenger.eventFlow,
                sendStartFetch = {
                    messenger.sendRawCommand(
                        ViewInspectionCodec.encodeStartFetchCommand(continuous = false)
                    )
                },
            )
        } catch (t: Throwable) {
            if (t is ComposeCaptureException) throw t
            throw ComposeCaptureException(
                "Failed to bootstrap the view inspector ($VIEW_INSPECTOR_ID) to learn the " +
                    "window root view ids: ${t.message}", t
            )
        }
        // No stopInspectors here: it disposes the whole per-process connection, disposing the
        // compose inspector launched right after. Both inspectors coexist (App Inspection keys
        // them by id); the outer capture() finally stops everything after the compose fetch.
        if (ids.isEmpty()) {
            throw ComposeCaptureException(
                "The view inspector reported no window roots for pid=${process.pid}, so the " +
                    "compose root views cannot be located. Does the app currently show a window?"
            )
        }
        logger.d("YALI Compose: window root view ids: $ids")
        return ids.toList()
    }

    /**
     * The prebuilt view inspector jar ships at
     * `<installRoot>/plugins/android/resources/app-inspection/layoutinspector-view-inspection.jar`.
     * installRoot candidates: PathManager.getHomePath() (the running AS distribution; codeSource
     * of platform/plugin classes is frequently null under the 2025.x class loaders) and, as a
     * fallback, the codeSource jar of ViewLayoutInspectorClient (`<root>/plugins/android/lib/`).
     */
    private fun resolveViewInspectorJar(): AppInspectorJar {
        val candidates = mutableListOf<File>()
        runCatching {
            val home = com.intellij.openapi.application.PathManager.getHomePath()
            if (home.isNotBlank()) {
                candidates += File(home, "plugins/android/resources/app-inspection/$VIEW_INSPECTOR_JAR_NAME")
            }
        }
        runCatching {
            val location = ViewLayoutInspectorClient::class.java.protectionDomain?.codeSource?.location
            if (location != null) {
                val pluginsDir = File(location.path).parentFile?.parentFile?.parentFile
                if (pluginsDir != null) {
                    candidates += pluginsDir.resolve(
                        "android/resources/app-inspection/$VIEW_INSPECTOR_JAR_NAME"
                    )
                }
            }
        }
        val jarFile = candidates.firstOrNull { it.isFile }
            ?: throw ComposeCaptureException(
                "layoutinspector-view-inspection.jar not found (looked in: " +
                    candidates.joinToString { it.path } + ") - the view inspector bootstrap " +
                    "requires a full Android Studio installation."
            )
        return AppInspectorJar(
            jarFile.name,
            jarFile.parentFile.absolutePath,
            jarFile.parentFile.absolutePath,
        )
    }

    private class StopCollecting : Exception()

    /**
     * Flow-side id collection, decoupled from the transport for tests:
     *  1. wait [rootsWindowMs] for the (guaranteed-before-first-layout) WindowRootsEvent;
     *  2. if it never carried ids, send StartFetchCommand{continuous=false} and keep collecting
     *     until a WindowRootsEvent or a LayoutEvent.root_view.node.id shows up.
     * Returns the empty set when both windows expire.
     */
    internal suspend fun collectRootViewIds(
        events: Flow<ByteArray>,
        sendStartFetch: suspend () -> Unit,
        rootsWindowMs: Long = ROOTS_EVENT_WINDOW_MS,
        fetchWindowMs: Long = FETCH_EVENT_WINDOW_MS,
    ): Set<Long> {
        val decoded = events.mapNotNull { ViewInspectionCodec.decodeEventBytes(it) }
        val ids = mutableSetOf<Long>()
        val rootsEvent = withTimeoutOrNullCompat(rootsWindowMs) {
            decoded.firstOrNull { event ->
                (event is ViewInspectionCodec.ViewEvent.WindowRoots && event.ids.isNotEmpty())
                    .also { matched ->
                        if (matched) ids += (event as ViewInspectionCodec.ViewEvent.WindowRoots).ids
                    }
            }
        }
        if (rootsEvent != null && ids.isNotEmpty()) return ids
        ids.clear()
        // The agent emits nothing until StartFetchCommand; sending it only *after* some event
        // arrives would deadlock, so send it unconditionally first and then collect.
        runCatching { sendStartFetch() }
            .onFailure { logger.w("YALI Compose: start_fetch failed: ${it.message}") }
        withTimeoutOrNullCompat(fetchWindowMs) {
            try {
                decoded.collect { event ->
                    when (event) {
                        is ViewInspectionCodec.ViewEvent.WindowRoots -> ids += event.ids
                        is ViewInspectionCodec.ViewEvent.Layout ->
                            event.rootViewId?.let { ids += it }
                        is ViewInspectionCodec.ViewEvent.Error ->
                            logger.w("YALI Compose: view inspector error event: ${event.message}")
                        else -> Unit
                    }
                    if (ids.isNotEmpty()) throw StopCollecting()
                }
            } catch (_: StopCollecting) {
                // normal early completion
            }
        }
        return ids
    }

    private fun resolveProject(hint: Project?): Project {
        if (hint != null) {
            return hint
        }
        val manager = ProjectManager.getInstance()
        return manager.openProjects.firstOrNull { it !== manager.defaultProject }
            ?: manager.defaultProject
    }

    /** version >= floor, comparing "major.minor.micro[-qualifier]" like com.android.ide.common.gradle.Version. */
    internal fun isAtLeast(version: String, floor: String): Boolean =
        compareVersions(version, floor) >= 0

    private fun compareVersions(a: String, b: String): Int {
        val (baseA, qualifierA) = splitVersion(a)
        val (baseB, qualifierB) = splitVersion(b)
        val parts = maxOf(baseA.size, baseB.size)
        for (i in 0 until parts) {
            val na = baseA.getOrElse(i) { 0 }
            val nb = baseB.getOrElse(i) { 0 }
            if (na != nb) {
                return compareValues(na, nb)
            }
        }
        // a release (no qualifier) is greater than any prerelease of the same base
        return when {
            qualifierA.isEmpty() && qualifierB.isEmpty() -> 0
            qualifierA.isEmpty() -> 1
            qualifierB.isEmpty() -> -1
            else -> qualifierA.compareTo(qualifierB)
        }
    }

    private fun splitVersion(version: String): Pair<List<Int>, String> {
        val cleaned = version.substringBefore('+').trim()
        val dash = cleaned.indexOf('-')
        val base = if (dash >= 0) cleaned.substring(0, dash) else cleaned
        val qualifier = if (dash >= 0) cleaned.substring(dash + 1) else ""
        val numbers = base.split('.').map { segment -> segment.filter(Char::isDigit).toIntOrNull() ?: 0 }
        return numbers to qualifier
    }

    private suspend fun <T> runCatchingSuspend(context: String, block: suspend () -> T): Result<T> =
        try {
            Result.success(block())
        } catch (t: Throwable) {
            if (t is ComposeCaptureException) {
                Result.failure(t)
            } else {
                Result.failure(ComposeCaptureException("Failed to $context: ${t.message}", t))
            }
        }

    private suspend fun <T> withTimeoutOrNullCompat(timeoutMs: Long, block: suspend () -> T?): T? =
        try {
            withTimeout(timeoutMs) { block() }
        } catch (e: TimeoutCancellationException) {
            null
        }
}
