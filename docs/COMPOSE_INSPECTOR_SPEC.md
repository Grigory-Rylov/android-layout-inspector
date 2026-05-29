# YALI Compose Inspector — техническое задание

Документ описывает архитектуру и план реализации расширения YALI, которое умеет
извлекать иерархию Jetpack Compose из запущенного на устройстве приложения и
показывать сводную информацию о ней.

## 1. Контекст

Текущий YALI вытаскивает View hierarchy через два независимых канала:

1. **Legacy `ViewDebug` протокол** (`ClientWindow.loadWindowData` →
   `ViewNodeParser`) — даёт классический `View` tree и скриншот.
2. **UI Automator dump** (`adb shell uiautomator dump` → SAX-парсер
   `HierarchyDumpParser`) — даёт XML accessibility-дерева, в т.ч. для
   Compose-нод (Compose рисует семантику через accessibility-бридж).

Оба способа не дают полноценную Compose-иерархию: ViewDebug видит
`AndroidComposeView` как один лист, UI Automator показывает только листы с
merged semantics — без названий `@Composable`, без файла/строки исходника, без
параметров.

## 2. Эталонная реализация (Android Studio)

Android Studio с релиза 2021.1 использует **App Inspection framework**:

- На устройстве работает **transport daemon** (`/data/local/tmp/perfd`), который
  IDE деплоит при первом подключении.
- IDE через transport деплоит в процесс приложения dex-агент
  **`compose-ui-inspection.jar`** (берётся либо из артефакта
  `androidx.compose.ui:ui-inspection`, либо из bundled `plugins/android/resources`
  Android Studio).
- Агент инжектится в JVM целевого процесса через JVMTI и регистрируется
  под идентификатором **`layoutinspector.compose.inspection`**.
- Связь IDE ↔ агент — protobuf-сообщения через transport (RSocket поверх UDS).

### 2.1. Протокол

Файл протокола: `compose/ui/ui-inspection/src/main/proto/layout_inspector_compose.proto`
в AOSP (`platform/frameworks/support`). Java-пакет —
`layoutinspector.compose.inspection.LayoutInspectorComposeProtocol`.

Ключевые сообщения:

- **`Command { GetComposablesCommand | GetParametersCommand | … }`** —
  запрос от IDE.
- **`Response { GetComposablesResponse | GetParametersResponse | … }`** —
  ответ агента.
- **`GetComposablesResponse`** содержит `repeated StringEntry strings` (таблица
  строк, в неё ссылаются `int32`-поля типа `name`, `filename`) и
  `repeated ComposableRoot roots`.
- **`ComposableNode`** — узел Compose-дерева:
  - `sint64 id` — уникальный id.
  - `repeated ComposableNode children`.
  - `int32 package_hash`, `int32 filename`, `int32 line_number`, `int32 offset`,
    `int32 name` — источник (ссылки в string table).
  - `Bounds bounds` (Rect `layout` + Quad `render` с трансформациями).
  - `int32 flags` — битовая маска (`SYSTEM_CREATED`,
    `HAS_MERGED_SEMANTICS`, `HAS_UNMERGED_SEMANTICS`, `INLINED`,
    `NESTED_SINGLE_CHILDREN`, `HAS_DRAW_MODIFIER`, `HAS_CHILD_DRAW_MODIFIER`).
  - `int64 view_id` — id хост-View.
  - `int32 recompose_count`, `int32 recompose_skips`, `sint32 anchor_hash`.

Полный proto-файл хранится в репозитории — `proto/compose_layout_inspection.proto`.

### 2.2. Поток в Studio

1. Studio запрашивает список процессов через transport, фильтрует debuggable.
2. Пользователь выбирает процесс → Studio проверяет версию
   `androidx.compose.ui:ui` (минимум `1.2.1`).
3. Studio вызывает `apiServices.launchInspector(LaunchParameters)` —
   transport деплоит `compose-ui-inspection.jar` в процесс приложения.
4. Studio отправляет `Command{GetComposables}` для каждого root View. Агент
   ходит по `AndroidComposeView`, через рефлексию получает `SlotTree` +
   `SemanticsOwner`, собирает дерево, сериализует в protobuf.
5. Дерево соединяется с View tree (по `view_id` у root) и рисуется.

## 3. Что доступно YALI

YALI — это плагин для Android Studio (`bundledPlugin("org.jetbrains.android")`).
В принципе у плагина есть доступ ко всем bundled-API Studio, включая
`com.android.tools.idea.appinspection.api.AppInspectionApiServices`.

Но привязка к этим внутренним API:

- **хрупкая** — пакеты `com.android.tools.idea.appinspection.*` меняются от
  версии к версии (новые поля в `LaunchParameters`, переименования
  `InspectorClientLaunchMonitor`).
- **слабо документирована** — нет публичного компилируемого SDK.
- **требует точного матчинга версий** транспорта и `compose-ui-inspection.jar`.

Альтернативный путь — собственный JDWP-канал. Но рабочий JDWP-канал к
App Inspection-агенту не существует: агент устанавливается через JVMTI и
слушает Unix domain socket, не JDWP DDMS chunks (попытка такого подхода видна
в существующей ветке `compose` и не работает).

## 4. Стратегия реализации

Целевая архитектура — **многоуровневая**, чтобы дать пользу сразу и постепенно
докручивать качество.

### 4.1. Layer 0 — UI Automator + Compose-aware анализ (MVP, эта итерация)

**Что:** новое action `Inspect Compose Hierarchy` в меню `Tools → YALI`. По
выбору устройства YALI запускает существующий `HierarchyDump`
(`adb shell uiautomator dump`), парсит, анализирует и показывает в диалоге:

- общую статистику дерева (всего нод, видимых нод, активити, окон);
- сводку **Compose-нод**: эвристика — нода с
  `class="android.view.View"` (Compose semantic) **или** содержащая
  предка `androidx.compose.ui.platform.AndroidComposeView`;
- топ-список текстов, content-description'ов, кликабельных нод;
- распределение классов в дереве;
- сами Compose-ноды в JTree (с координатами, текстом, content-desc).

**Зачем:**

- работает на любом debuggable-устройстве API ≥ 21 без агентов;
- ничего не ломает в существующем flow YALI;
- даёт пользователю real-life диагностический инструмент уже сейчас;
- независимо от версии Compose и от внутренних API Android Studio;
- является «единым каналом данных», который сложно сделать сильно лучше без
  агента.

**Ограничения:**

- видим только нод с **merged semantics**; чисто декоративные Compose-узлы
  без `Modifier.semantics{}` не попадают.
- нет имён `@Composable` функций, нет файла/строки.

### 4.2. Layer 1 — встроенный App Inspection (следующая итерация)

Через bundled-API Studio:

```kotlin
val apiServices = AppInspectionDiscoveryService.getInstance(project).apiServices
val messenger = apiServices.launchInspector(
    LaunchParameters(
        processDescriptor,
        inspectorId = "layoutinspector.compose.inspection",
        jar = AppInspectorJar(
            name = "compose-ui-inspection.jar",
            developmentDirectory = null,
            releaseDirectory = "plugins/android/resources/app-inspection/",
        ),
        projectName = project.name,
        minLibraryVersion = "1.2.1",
        force = true,
    )
)
val response = messenger.sendRawCommand(
    Command.newBuilder().apply {
        getComposablesBuilder.apply {
            rootViewId = viewId
            skipSystemComposables = true
            generation = 0
            extractAllParameters = false
        }
    }.build().toByteArray()
)
val parsed = Response.parseFrom(response)
```

На этом уровне получаем полноценный `ComposableNode` tree с именами,
координатами, флагами и recomposition counts.

**Что нужно сделать в этой итерации:**

1. Подключить `protobuf` gradle-plugin, сгенерировать java-классы из
   `proto/compose_layout_inspection.proto`.
2. Реализовать `AppInspectionLauncher` на тонком reflection wrapper над
   `com.android.tools.idea.appinspection.*` (чтобы плагин компилировался
   против разных версий Studio).
3. Реализовать `ComposeTreeFetcher` — sendCommand → parse → mapping
   в внутренний `ComposeViewNode`.
4. Прикрутить `ComposeViewNode` к существующему `TreeMerger`, чтобы
   Compose-ноды соединялись с View-нодами по `view_id`.

### 4.3. Layer 2 — UI рендеринг и фичи (на будущее)

- Отрисовка Compose-нод в `LayoutPanel` поверх скриншота (Quad-трансформации
  из `Bounds.render`).
- Boostrapping параметров (`GetParametersCommand`) с лениво подгружаемыми
  значениями.
- Highlight recompositions (`UpdateSettingsCommand` →
  `highlight_recompositions = true`).
- Прыжок в исходник по клику (использовать `package_hash` + `filename`
  + `line_number` → `PsiManager.findFile`).

## 5. Архитектура MVP (Layer 0)

```
com.github.grishberg.android.li.compose
├── InspectComposeHierarchyAction.kt   // меню Tools → YALI → Inspect Compose
├── ComposeInspectDialog.kt            // Swing-окно со сводкой + tree
├── ComposeHierarchyAnalyzer.kt        // эвристика + статистика
└── model
    ├── ComposeHierarchySummary.kt     // total/visible/clickable/textCount...
    └── ComposeCandidateNode.kt        // подсвеченные Compose-кандидаты
```

`InspectComposeHierarchyAction`:

1. Получает `IDevice` через существующий `ConnectedDeviceInfoProvider`.
2. Если устройств несколько — показывает `ChooseDeviceDialog` (минимальный
   `JList<IDevice>`).
3. Запускает `HierarchyDump.getHierarchyDump()` (переиспользует существующий
   класс — он уже умеет `uiautomator dump` + `pullFile`).
4. Парсит через `HierarchyDumpParser` (тоже существующий).
5. Передаёт корень в `ComposeHierarchyAnalyzer`.
6. Открывает `ComposeInspectDialog` с результатом.

`ComposeHierarchyAnalyzer`:

- Обход дерева `DumpViewNode`.
- Накопление:
  - `totalNodes` — всего;
  - `visibleNodes` — у которых width>0 и height>0;
  - `clickableNodes`, `focusableNodes` — UI Automator при `--compressed` режиме
    их не отдаёт, поэтому в MVP считаем по другим эвристикам (текст / нон-нулл
    content-desc);
  - `composeCandidates` — ноды, где
    `name == "android.view.View"` И `id == null` И
    (есть `text` ИЛИ есть `content-desc`)
    либо предок которых — `AndroidComposeView`/`ComposeView`.
  - `classDistribution` — гистограмма по класс-имени;
  - `topTexts` / `topContentDescs` — топ-30 (для быстрой ориентации в экране).

`ComposeInspectDialog`:

- Верхняя панель — сводка строк (`Total: 142, Compose candidates: 37, …`).
- Левая половина — `JTree` Compose-кандидатов с координатами и текстом.
- Правая половина — `JTable` распределения классов.
- Снизу — кнопка `Copy summary to clipboard` (JSON) и `Refresh`.

## 6. Совместимость и риски

- YALI на master уже Kotlin 2.2, Java 21, IntelliJ Platform Gradle Plugin 2.10
  и Android Studio API 2025.3.1.1 (sinceBuild `253.28294.334`). Все новые файлы
  следуют этим версиям. Никаких новых зависимостей в MVP не нужно — задача
  решается уже подключённым `kotlinx.coroutines` + bundled IntelliJ Swing.
- UI Automator dump может быть пустым на некоторых устройствах (особенно с
  кастомными прошивками или в момент анимации). Action отдельно сообщает
  пользователю через `NotificationHelper.error`.
- На устройствах без debuggable-процессов action всё равно работает — UI
  Automator слушает Accessibility, а не debug-канал.

## 7. План работ

- [x] Исследование протокола App Inspection / Compose UI Inspection.
- [x] Документ ТЗ (этот файл).
- [x] Layer 0 implementation: action + analyzer + dialog.
- [x] Регистрация в `plugin.xml`.
- [x] Локальный билд `./gradlew buildPlugin`.
- [ ] (next iteration) Layer 1: protobuf, AppInspection launcher.
- [ ] (next iteration) Layer 2: визуализация поверх скриншота.

## 8. Источники

- AOSP `androidx.compose.ui:ui-inspection` — реализация on-device агента.
- Android Studio plugin `layout-inspector` (JetBrains/android,
  `layout-inspector/src/com/android/tools/idea/layoutinspector/pipeline/appinspection/compose/`).
- Существующая ветка `origin/compose` этого репозитория (отсюда взят proto-файл).
