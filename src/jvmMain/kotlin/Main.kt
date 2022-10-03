// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.launch
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import kotlin.experimental.xor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App(
    windowSize: WindowSize
) {
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    var currentPage by remember { mutableStateOf(0) }

    @OptIn(ExperimentalMaterial3Api::class) val drawerContent: @Composable ColumnScope.() -> Unit = {
        Spacer(modifier = Modifier.height(12.dp))
        NavigationDrawerItem(label = {
            Text("密码处理")
        }, selected = currentPage == 0, onClick = {
            currentPage = 0
            coroutineScope.launch {
                drawerState.close()
            }
        }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        NavigationDrawerItem(label = {
            Text("进程操作")
        }, selected = currentPage == 1, onClick = {
            currentPage = 1
            coroutineScope.launch {
                drawerState.close()
            }
        }, modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
    }

    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) {
            darkColorScheme()
        } else {
            lightColorScheme()
        }
    ) {
        ModalNavigationDrawer(
            drawerContent = drawerContent,
            drawerState = drawerState,
            //gesturesEnabled = windowSize == WindowSize.Compact
        ) {

            Scaffold(topBar = {
                SmallTopAppBar(title = {
                    Text("FuckJY")
                }, navigationIcon = {
                    IconButton(onClick = { coroutineScope.launch { drawerState.open(windowSize) } }) {
                        Icon(Icons.Default.Menu, contentDescription = "菜单")
                    }
                })
            }) { padding ->
                Box(Modifier.padding(padding)) {
                    Crossfade(currentPage) { page ->
                        when (page) {
                            0 -> PasswordOperation()
                            1 -> ProcessOperation()
                        }
                    }
                }
            }
        }
    }

    /*
    LaunchedEffect(windowSize) {
        when (windowSize) {
            WindowSize.Compact -> {

            }

            WindowSize.Medium -> {
                drawerState.close()
            }

            WindowSize.Expanded -> {
                drawerState.close()
            }
        }
    }
     */
}

@Composable
fun PasswordOperation() {
    var input by remember { mutableStateOf("") }
    var showResult by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    fun decrypt() {
        if (input.length % 4 != 0) {
            result = "输入长度必须为4的倍数"
            isError = true
        } else {
            try {
                result = decrypt(input)
                isError = false
            } catch (e: NumberFormatException) {
                result = "输入必须为有效的16进制数字\n${e.message}"
                isError = true
            } catch (e: Throwable) {
                result = "出现未知错误\n${e.message}\n${e.stackTrace}"
                isError = true
            }
        }
        showResult = true
    }

    fun encrypt() {
        result = encrypt(input)
        isError = false
        showResult = true
    }

    fun replace() {
        if (isError) return
        input = result
        showResult = false
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(value = input, onValueChange = { input = it })
        Spacer(Modifier.height(16.dp))
        Row {
            Button(onClick = ::decrypt) {
                Text("解密")
            }
            Spacer(Modifier.width(16.dp))
            Button(onClick = ::encrypt) {
                Text("加密")
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
                        Text("替换到输入框")
                    }
                }
            }
        }
    }
}

fun decrypt(input: String): String {
    val array = input.toHex()   // 把输入的 16 进制字符串（如 1234abcd）转换成 ByteArray
    val zero = array[0] xor 0x50 xor 0x45   // 解密第 1 个 Byte 获得密码起始位置
    var result = ""

    for (i in zero..array.lastIndex) {  // 相当于 for (int i = zero; i < array.length; i ++)，Kotlin 中没有该写法
        // 根据余数判断处理方法，异或运算没有顺序
        array[i] = if (i % 4 == 0 || i % 4 == 3) array[i] xor 0x50 xor 0x45 else array[i] xor 0x4c xor 0x43
        if ((i - zero) % 2 == 0) continue   // 由于是 UTF-16LE，所以需要两个 Byte 一起读取
        if (array[i - 1].toInt() == 0 && array[i].toInt() == 0) break   // 读取到空字符直接跳出
        // 将两个 Byte 合并起来，这个 String 理论上只会有 1 个字符
        result += String(byteArrayOf(array[i - 1], array[i]), Charsets.UTF_16LE)
    }
    return result
}

@OptIn(ExperimentalUnsignedTypes::class)
fun encrypt(input: String): String {
    val plain = "$input\u0000"  // 明文密码，添加了空字符
    // 编码并声明从索引 1 开始，此处 1u 相当于 uint 1
    val data = ubyteArrayOf(1u, *plain.toByteArray(Charsets.UTF_16LE).asUByteArray(), 1u)
    var result = ""

    for (i in data.indices) {   // 相当于 for (int i = 0; i < data.length; i ++)
        result += if (i % 4 == 0 || i % 4 == 3) {
            data[i] xor 0x50u xor 0x45u
        } else {
            data[i] xor 0x4cu xor 0x43u
        }.toString(16).padStart(2, '0')  // 将 Byte 转成 16 进制的 String 并在前面补 0
    }

    return result
}

@Composable
fun ProcessOperation() {
    val killProcess = {

    }

    Box(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Button(
            onClick = killProcess, colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.error
            )
        ) {
            Text("杀死极域（WIP）")
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "FuckJY") {
        App(
            windowSize = window.rememberWindowSize()
        )
    }
}

fun String.toHex(): ByteArray {
    check(length % 2 == 0) { "Invalid hex string length: $length" }
    return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}

@Composable
fun ComposeWindow.rememberWindowSize(): WindowSize {
    with(LocalDensity.current) {
        var state by remember { mutableStateOf(width.toDp().toWindowSize()) }

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
    this < 600.dp -> WindowSize.Compact
    this < 840.dp -> WindowSize.Medium
    else -> WindowSize.Expanded
}

enum class WindowSize {
    Compact, Medium, Expanded
}

@OptIn(ExperimentalMaterial3Api::class)
suspend inline fun DrawerState.open(windowSize: WindowSize) {
    //if (windowSize == WindowSize.Compact) {
    open()
    //}
}
