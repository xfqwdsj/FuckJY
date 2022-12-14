import WindowSize.*
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import composables.DEFAULT_PAGE
import composables.Page
import kotlinx.coroutines.launch
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import kotlin.experimental.xor
import kotlin.system.exitProcess

val helper: String = File(
    System.getProperty("compose.application.resources.dir"),
    "RegistryHelper.exe"
).absolutePath

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    windowSize: WindowSize
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var currentPage by remember { mutableStateOf(DEFAULT_PAGE) }

    val page: @Composable () -> Unit = {
        val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("FuckJY")
                    },
                    navigationIcon = {
                        AnimatedVisibility(
                            visible = windowSize == Compact,
                            enter = fadeIn() + expandHorizontally(),
                            exit = fadeOut() + shrinkHorizontally()
                        ) {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open(windowSize) } }) {
                                Icon(Icons.Default.Menu, contentDescription = "??????")
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            }
        ) { padding ->
            Row(Modifier.padding(padding)) {
                AnimatedVisibility(
                    visible = windowSize == Medium
                ) {
                    NavigationRail {
                        Page.values().forEach { page ->
                            page.RailItem(currentPage) { currentPage = it }
                        }
                    }
                }
                Crossfade(currentPage) { page ->
                    when (page) {
                        Page.PasswordOperation -> PasswordOperation.Content(scrollBehavior)
                        Page.ProcessOperation -> ProcessOperation.Content(scrollBehavior)
                    }
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }
    ) {
        PermanentNavigationDrawer(
            drawerContent = {
                AnimatedVisibility(
                    visible = windowSize == Expanded,
                    enter = fadeIn() + expandHorizontally(),
                    exit = fadeOut() + shrinkHorizontally()
                ) {
                    PermanentDrawerSheet {
                        Spacer(Modifier.height(12.dp))
                        Page.values().forEach { page ->
                            page.DrawerItem(currentPage) { currentPage = it }
                        }
                    }
                }
            }
        ) {
            ModalNavigationDrawer(
                drawerContent = {
                    ModalDrawerSheet {
                        Spacer(modifier = Modifier.height(12.dp))
                        Page.values().forEach { page ->
                            page.ModalDrawerItem(drawerState, currentPage) { currentPage = it }
                        }
                    }
                },
                drawerState = drawerState,
                gesturesEnabled = windowSize == Compact,
                content = page
            )
        }
    }

    LaunchedEffect(windowSize) {
        when (windowSize) {
            Compact -> {}

            Medium -> {
                drawerState.close()
            }

            Expanded -> {
                drawerState.close()
            }
        }
    }
}

object PasswordOperation {
    private var input by mutableStateOf("")
    private var showResult by mutableStateOf(false)
    private var result by mutableStateOf("")
    private var isError by mutableStateOf(false)
    private val scrollState = ScrollState(0)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(scrollBehavior: TopAppBarScrollBehavior) {
        fun decrypt() {
            if (input.length % 8 != 0) {
                result = "????????????????????? 8 ?????????"
                isError = true
            } else {
                try {
                    result = decrypt(input)
                    isError = false
                } catch (e: NumberFormatException) {
                    result = "???????????????????????? 16 ????????????\n${e.message}".trim()
                    isError = true
                } catch (e: Throwable) {
                    result = "??????????????????\n${e.message}\n${e.stackTrace}".trim()
                    isError = true
                }
            }
            showResult = true
        }

        fun encrypt() {
            println(System.getProperties())
            result = encrypt(input)
            isError = false
            showResult = true
        }

        fun get() {
            input = ProcessBuilder(helper).start().apply { waitFor() }.inputStream.bufferedReader().readText().trim()
            decrypt()
        }

        fun set() {
            val process = ProcessBuilder(helper, encrypt(input))
                .start().apply { waitFor() }
            if (process.exitValue() != 0) {
                result = process.errorStream.bufferedReader().readText().trim()
                isError = true
            } else {
                result = "????????????"
                isError = false
            }
            showResult = true
        }

        fun replace() {
            if (isError) return
            input = result
            showResult = false
        }

        Box(Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                TextField(value = input, onValueChange = { input = it })
                Spacer(Modifier.height(16.dp))
                Row {
                    Button(onClick = ::decrypt) {
                        Text("??????")
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = ::encrypt) {
                        Text("??????")
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = ::get) {
                        Text("??????????????????")
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = ::set) {
                        Text("??????????????????")
                    }
                }
                Spacer(Modifier.height(16.dp))
                AnimatedVisibility(
                    visible = showResult
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        SelectionContainer {
                            Text(
                                result,
                                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        AnimatedVisibility(
                            visible = !isError
                        ) {
                            Button(onClick = ::replace) {
                                Text("??????????????????")
                            }
                        }
                    }
                }
            }
            VerticalScrollbar(
                modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(),
                adapter = rememberScrollbarAdapter(scrollState)
            )
        }
    }
}

fun decrypt(input: String): String {
    val array = input.toHex()   // ???????????? 16 ????????????????????? 1234abcd???????????? ByteArray
    val zero = array[0] xor 0x15   // ????????? 1 ??? Byte ????????????????????????
    var result = ""

    for (i in zero..array.lastIndex) {  // ????????? for (int i = zero; i < array.length; i ++)???Kotlin ??????????????????
        // ?????????????????????????????????????????????????????????
        array[i] = if (i % 4 == 0 || i % 4 == 3) array[i] xor 0x15 else array[i] xor 0x0f
        if ((i - zero) % 2 == 0) continue   // ????????? UTF-16LE????????????????????? Byte ????????????
        if (array[i - 1].toInt() == 0 && array[i].toInt() == 0) break   // ??????????????????????????????
        // ????????? Byte ????????????????????? String ?????????????????? 1 ?????????
        result += String(byteArrayOf(array[i - 1], array[i]), Charsets.UTF_16LE)
    }

    return result
}

@OptIn(ExperimentalUnsignedTypes::class)
fun encrypt(input: String): String {
    val plain = "$input\u0000"  // ?????????????????????????????????
    // ???????????????????????? 1 ??????????????????????????? 4 ???????????????????????????????????? 1u ????????? uint 1
    val data = ubyteArrayOf(
        1u, *plain.toByteArray(Charsets.UTF_16LE).asUByteArray(), *UByteArray((plain.length - 1) % 2 * 2) { 0u }, 1u
    )
    var result = ""

    for (i in data.indices) {   // ????????? for (int i = 0; i < data.length; i ++)
        result += if (i % 4 == 0 || i % 4 == 3) {
            data[i] xor 0x15u
        } else {
            data[i] xor 0x0fu
        }.toString(16).padStart(2, '0')  // ??? Byte ?????? 16 ????????? String ??????????????? 0
    }

    return result
}

object ProcessOperation {
    private val scrollState = ScrollState(0)

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun Content(scrollBehavior: TopAppBarScrollBehavior) {
        fun kill() {
            ProcessBuilder("taskkill", "/f", "/im", "StudentMain.exe").start()
        }

        Box(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState)
                .nestedScroll(scrollBehavior.nestedScrollConnection).padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Button(
                onClick = ::kill, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("????????????")
            }
        }
    }

}

fun String.toHex(): ByteArray {
    check(length % 2 == 0) { "Invalid hex string length: $length" }
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

@Composable
fun ComposeWindow.rememberWindowSize(): WindowSize {
    with(LocalDensity.current) {
        var state by rememberSaveable { mutableStateOf(width.toDp().toWindowSize()) }

        val listener = object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                state = e.component.width.toDp().toWindowSize()
            }
        }
        addComponentListener(listener)

        DisposableEffect(Unit) {
            onDispose {
                removeComponentListener(listener)
            }
        }

        return state
    }
}

fun Dp.toWindowSize() = when {
    this < 600.dp -> Compact
    this < 840.dp -> Medium
    else -> Expanded
}

enum class WindowSize {
    Compact, Medium, Expanded
}

@OptIn(ExperimentalMaterial3Api::class)
suspend inline fun DrawerState.open(windowSize: WindowSize) {
    if (windowSize == Compact) {
        open()
    }
}

fun main(vararg args: String) {
    if (!args.contains("admin")) {
        try {
            ProcessBuilder(helper).start().waitFor()
        } catch (e: Throwable) {
            ProcessBuilder(
                File(
                    System.getProperty("compose.application.resources.dir"), "Elevator.exe"
                ).absolutePath
            ).start()
            exitProcess(0)
        }
    }

    application {
        Window(onCloseRequest = ::exitApplication, title = "FuckJY") {
            App(
                windowSize = window.rememberWindowSize()
            )
        }
    }
}
