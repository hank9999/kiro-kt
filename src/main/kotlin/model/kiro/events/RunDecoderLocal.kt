package model.kiro.events

import model.kiro.events.model.*
import model.kiro.events.parser.KiroEventStreamParser
import java.util.Base64

/**
 * Kiro Event Stream 解析器本地测试工具
 *
 * 输入 Base64 编码的二进制数据，解析并打印事件。
 *
 * 使用方式：
 * 1. 运行程序
 * 2. 输入 Base64 编码的二进制数据（支持多行，空行结束）
 * 3. 查看解析结果
 * 4. 输入 'exit' 或 'quit' 退出
 */
object RunDecoderLocal {

    private val parser = KiroEventStreamParser()

    // ==================== ANSI 颜色支持 ====================

    private object Ansi {
        // 基础颜色
        const val RESET = "\u001B[0m"
        const val BOLD = "\u001B[1m"
        const val DIM = "\u001B[2m"

        // 前景色
        const val BLACK = "\u001B[30m"
        const val RED = "\u001B[31m"
        const val GREEN = "\u001B[32m"
        const val YELLOW = "\u001B[33m"
        const val BLUE = "\u001B[34m"
        const val MAGENTA = "\u001B[35m"
        const val CYAN = "\u001B[36m"
        const val WHITE = "\u001B[37m"

        // 亮色
        const val BRIGHT_BLACK = "\u001B[90m"
        const val BRIGHT_RED = "\u001B[91m"
        const val BRIGHT_GREEN = "\u001B[92m"
        const val BRIGHT_YELLOW = "\u001B[93m"
        const val BRIGHT_BLUE = "\u001B[94m"
        const val BRIGHT_MAGENTA = "\u001B[95m"
        const val BRIGHT_CYAN = "\u001B[96m"
        const val BRIGHT_WHITE = "\u001B[97m"

        // 背景色
        const val BG_RED = "\u001B[41m"
        const val BG_GREEN = "\u001B[42m"
        const val BG_YELLOW = "\u001B[43m"
        const val BG_BLUE = "\u001B[44m"
    }

    // 颜色开关（默认检测终端支持）
    private var colorEnabled: Boolean = detectColorSupport()

    // 十六进制显示开关
    private var hexEnabled: Boolean = false

    // 检测终端是否支持颜色
    private fun detectColorSupport(): Boolean {
        val term = System.getenv("TERM") ?: ""
        val colorTerm = System.getenv("COLORTERM") ?: ""
        val forceColor = System.getenv("FORCE_COLOR")

        // 如果明确禁用颜色
        if (System.getenv("NO_COLOR") != null) return false

        // 如果强制启用颜色
        if (forceColor != null && forceColor != "0") return true

        // 检测常见的支持颜色的终端
        return term.contains("color") ||
                term.contains("xterm") ||
                term.contains("screen") ||
                term.contains("tmux") ||
                term.contains("vt100") ||
                colorTerm.isNotEmpty() ||
                System.console() != null
    }

    // 颜色辅助函数
    private fun color(text: String, vararg codes: String): String {
        if (!colorEnabled) return text
        return codes.joinToString("") + text + Ansi.RESET
    }

    private fun bold(text: String) = color(text, Ansi.BOLD)
    private fun dim(text: String) = color(text, Ansi.DIM)
    private fun red(text: String) = color(text, Ansi.RED)
    private fun green(text: String) = color(text, Ansi.GREEN)
    private fun yellow(text: String) = color(text, Ansi.YELLOW)
    private fun blue(text: String) = color(text, Ansi.BLUE)
    private fun cyan(text: String) = color(text, Ansi.CYAN)
    private fun magenta(text: String) = color(text, Ansi.MAGENTA)
    private fun brightBlack(text: String) = color(text, Ansi.BRIGHT_BLACK)
    private fun brightCyan(text: String) = color(text, Ansi.BRIGHT_CYAN)
    private fun brightGreen(text: String) = color(text, Ansi.BRIGHT_GREEN)
    private fun brightYellow(text: String) = color(text, Ansi.BRIGHT_YELLOW)
    private fun brightRed(text: String) = color(text, Ansi.BRIGHT_RED)

    // ==================== 字符宽度计算 ====================

    // ANSI 转义序列正则表达式
    private val ANSI_REGEX = Regex("\u001B\\[[0-9;]*m")

    /**
     * 移除字符串中的 ANSI 转义序列
     */
    private fun stripAnsi(text: String): String = ANSI_REGEX.replace(text, "")

    /**
     * 计算单个字符的显示宽度
     */
    private fun charWidth(ch: Char): Int {
        return when {
            // CJK 字符范围
            ch in '\u4E00'..'\u9FFF' -> 2  // CJK 统一汉字
            ch in '\u3400'..'\u4DBF' -> 2  // CJK 扩展 A
            ch in '\uF900'..'\uFAFF' -> 2  // CJK 兼容汉字
            ch in '\u3000'..'\u303F' -> 2  // CJK 标点符号
            ch in '\uFF00'..'\uFFEF' -> 2  // 全角字符
            ch in '\u2E80'..'\u2EFF' -> 2  // CJK 部首补充
            ch in '\u2F00'..'\u2FDF' -> 2  // 康熙部首
            ch in '\u3100'..'\u312F' -> 2  // 注音符号
            ch in '\u31A0'..'\u31BF' -> 2  // 注音符号扩展
            ch in '\uAC00'..'\uD7AF' -> 2  // 韩文音节
            else -> 1
        }
    }

    /**
     * 计算字符串的显示宽度（考虑中文等宽字符占2个位置，忽略ANSI转义序列）
     */
    private fun displayWidth(text: String): Int {
        return stripAnsi(text).sumOf { charWidth(it) }
    }

    /**
     * 将字符串填充到指定显示宽度
     */
    private fun padToWidth(text: String, width: Int, padChar: Char = ' '): String {
        val currentWidth = displayWidth(text)
        return if (currentWidth < width) {
            text + padChar.toString().repeat(width - currentWidth)
        } else {
            text
        }
    }

    /**
     * 在字符串两侧填充到指定宽度（居中）
     */
    private fun centerToWidth(text: String, width: Int, padChar: Char = ' '): String {
        val currentWidth = displayWidth(text)
        if (currentWidth >= width) return text
        val totalPad = width - currentWidth
        val leftPad = totalPad / 2
        val rightPad = totalPad - leftPad
        return padChar.toString().repeat(leftPad) + text + padChar.toString().repeat(rightPad)
    }

    // ==================== 十六进制显示 ====================

    /**
     * 格式化显示十六进制数据
     */
    private fun formatHexDump(data: ByteArray, bytesPerLine: Int = 16): String {
        val sb = StringBuilder()
        var offset = 0

        while (offset < data.size) {
            // 偏移量
            sb.append(brightBlack(String.format("%08X", offset)))
            sb.append("  ")

            // 十六进制部分
            val hexPart = StringBuilder()
            val asciiPart = StringBuilder()

            for (i in 0 until bytesPerLine) {
                if (offset + i < data.size) {
                    val b = data[offset + i].toInt() and 0xFF
                    hexPart.append(String.format("%02X ", b))

                    // ASCII 部分：可打印字符显示原字符，否则显示点
                    asciiPart.append(
                        if (b in 0x20..0x7E) b.toChar() else '.'
                    )
                } else {
                    hexPart.append("   ")
                    asciiPart.append(" ")
                }

                // 每 8 字节添加额外空格
                if (i == 7) {
                    hexPart.append(" ")
                }
            }

            sb.append(cyan(hexPart.toString()))
            sb.append(" ")
            sb.append(brightBlack("|"))
            sb.append(yellow(asciiPart.toString()))
            sb.append(brightBlack("|"))
            sb.append("\n")

            offset += bytesPerLine
        }

        return sb.toString()
    }

    // ==================== UI 组件 ====================

    private const val BOX_WIDTH = 60

    private fun printHeader() {
        val title = "Kiro Event Stream 解析器"
        val subtitle = "本地测试工具"

        println()
        println(cyan("╔${"═".repeat(BOX_WIDTH)}╗"))
        println(cyan("║") + bold(centerToWidth(title, BOX_WIDTH)) + cyan("║"))
        println(cyan("║") + dim(centerToWidth(subtitle, BOX_WIDTH)) + cyan("║"))
        println(cyan("╠${"═".repeat(BOX_WIDTH)}╣"))
        println(cyan("║") + padToWidth(" 命令:", BOX_WIDTH) + cyan("║"))
        println(cyan("║") + padToWidth("   ${green("exit/quit")}  退出程序", BOX_WIDTH) + cyan("║"))
        println(cyan("║") + padToWidth("   ${green("reset")}      重置解析器", BOX_WIDTH) + cyan("║"))
        println(cyan("║") + padToWidth("   ${green("color")}      启用颜色输出", BOX_WIDTH) + cyan("║"))
        println(cyan("║") + padToWidth("   ${green("nocolor")}    禁用颜色输出", BOX_WIDTH) + cyan("║"))
        println(cyan("║") + padToWidth("   ${green("hex")}        启用十六进制显示", BOX_WIDTH) + cyan("║"))
        println(cyan("║") + padToWidth("   ${green("nohex")}      禁用十六进制显示", BOX_WIDTH) + cyan("║"))
        println(cyan("╠${"═".repeat(BOX_WIDTH)}╣"))
        println(cyan("║") + padToWidth(" 使用方式:", BOX_WIDTH) + cyan("║"))
        println(cyan("║") + padToWidth("   1. 输入 Base64 编码的二进制数据", BOX_WIDTH) + cyan("║"))
        println(cyan("║") + padToWidth("   2. 支持多行输入，输入空行开始解析", BOX_WIDTH) + cyan("║"))
        println(cyan("╚${"═".repeat(BOX_WIDTH)}╝"))
        println()

        // 显示当前设置状态
        printStatus()
    }

    private fun printStatus() {
        val colorStatus = if (colorEnabled) green("ON") else red("OFF")
        val hexStatus = if (hexEnabled) green("ON") else red("OFF")
        println(dim("当前设置: 颜色=$colorStatus${dim(", 十六进制=")}$hexStatus"))
        println()
    }

    private fun printDivider(char: Char = '─') {
        println(brightBlack(char.toString().repeat(BOX_WIDTH + 2)))
    }

    @JvmStatic
    fun main(args: Array<String>) {
        printHeader()

        val inputBuffer = StringBuilder()

        while (true) {
            print(if (inputBuffer.isEmpty()) cyan(">>> ") else cyan("... "))

            val line = readlnOrNull() ?: break

            when {
                line.equals("exit", ignoreCase = true) ||
                        line.equals("quit", ignoreCase = true) -> {
                    println(green("再见！"))
                    break
                }

                line.equals("reset", ignoreCase = true) -> {
                    parser.reset()
                    inputBuffer.clear()
                    println(green("✓") + " 解析器已重置")
                    println()
                    continue
                }

                line.equals("color", ignoreCase = true) -> {
                    colorEnabled = true
                    println(green("✓") + " 颜色输出已启用")
                    println()
                    continue
                }

                line.equals("nocolor", ignoreCase = true) -> {
                    colorEnabled = false
                    println("✓ 颜色输出已禁用")
                    println()
                    continue
                }

                line.equals("hex", ignoreCase = true) -> {
                    hexEnabled = true
                    println(green("✓") + " 十六进制显示已启用")
                    println()
                    continue
                }

                line.equals("nohex", ignoreCase = true) -> {
                    hexEnabled = false
                    println(green("✓") + " 十六进制显示已禁用")
                    println()
                    continue
                }

                line.equals("help", ignoreCase = true) || line == "?" -> {
                    printHeader()
                    continue
                }

                line.isEmpty() && inputBuffer.isNotEmpty() -> {
                    processInput(inputBuffer.toString().trim())
                    inputBuffer.clear()
                    println()
                    continue
                }

                line.isEmpty() -> {
                    continue
                }

                else -> {
                    inputBuffer.append(line)
                }
            }
        }
    }

    private fun processInput(base64Input: String) {
        println()
        printDivider('═')

        // 解码 Base64
        val binaryData = try {
            Base64.getDecoder().decode(base64Input.replace("\\s".toRegex(), ""))
        } catch (e: IllegalArgumentException) {
            println(red("✗") + " Base64 解码失败: ${e.message}")
            return
        }

        println(green("✓") + " Base64 解码成功，数据长度: ${bold(binaryData.size.toString())} 字节")

        // 显示十六进制数据
        if (hexEnabled) {
            println()
            println(bold("原始数据 (十六进制):"))
            println(formatHexDump(binaryData))
        }

        println()

        // 解析事件
        val events = try {
            parser.parse(binaryData)
        } catch (e: Exception) {
            println(red("✗") + " 解析失败: ${e.message}")
            e.printStackTrace()
            return
        }

        if (events.isEmpty()) {
            println(yellow("⚠") + " 未解析出任何事件")
            if (parser.hasPendingData()) {
                println(dim("  (缓冲区中有 ${parser.framesDecoded()} 帧待处理的数据)"))
            }
            return
        }

        println(green("✓") + " 解析出 ${bold(events.size.toString())} 个事件:")
        println()

        events.forEachIndexed { index, event ->
            printEvent(index + 1, event)
        }

        printDivider('═')
        println(
            dim("统计: ") +
                    "已解析帧数=${brightCyan(parser.framesDecoded().toString())}, " +
                    "错误数=${if (parser.errorCount() > 0) brightRed(parser.errorCount().toString()) else brightGreen(parser.errorCount().toString())}"
        )
    }

    private fun printEvent(index: Int, event: Event) {
        val (typeName, typeColor) = getEventTypeInfo(event)

        // 事件头部
        println(cyan("┌─") + bold(" 事件 #$index ") + cyan("─".repeat(BOX_WIDTH - 12)))
        println(cyan("│") + " 类型: " + typeColor(typeName))

        when (event) {
            is Event.AssistantResponse -> {
                printField("会话ID", event.data.conversationId)
                printField("消息ID", event.data.messageId)
                printField("状态", event.data.messageStatus?.name)
                printField("内容类型", event.data.contentType?.name)
                printContent("内容", event.data.content)
            }

            is Event.ToolUse -> {
                printField("工具名称", event.data.name)
                printField("工具ID", event.data.toolUseId)
                printField("完成", if (event.data.stop) green("是") else yellow("否"))
                printContent("输入", event.data.input?.toString())
            }

            is Event.Metering -> {
                println(cyan("│") + dim(" (计费事件，无额外数据)"))
            }

            is Event.ContextUsage -> {
                val percentage = event.data.formattedPercentage()
                val percentageColored = when {
                    event.data.isNearFull() -> brightRed(percentage)
                    event.data.contextUsagePercentage > 50.0 -> brightYellow(percentage)
                    else -> brightGreen(percentage)
                }
                printField("使用率", percentageColored)
                if (event.data.isNearFull()) {
                    println(cyan("│") + " " + yellow("⚠ 警告: 上下文接近满载!"))
                }
            }

            is Event.ToolCallRequest -> {
                printField("调用ID", event.data.toolCallId)
                printField("工具名称", event.data.toolName)
                printContent("输入", event.data.input?.toString())
            }

            is Event.ToolCallError -> {
                printField("调用ID", event.data.toolCallId)
                printField("错误", red(event.data.error))
            }

            is Event.SessionStart -> {
                printField("会话ID", event.data.sessionId)
            }

            is Event.SessionEnd -> {
                printField("会话ID", event.data.sessionId)
                printField("持续时间", event.data.duration?.let { "${it}ms" })
            }

            is Event.Unknown -> {
                printField("原始类型", event.rawEventType)
                printContent("原始数据", event.rawPayload?.toString())
            }

            is Event.Error -> {
                printField("错误代码", red(event.errorCode))
                printField("错误信息", red(event.errorMessage))
            }

            is Event.Exception -> {
                printField("异常类型", red(event.exceptionType))
                printField("异常信息", red(event.message))
            }
        }

        println(cyan("└${"─".repeat(BOX_WIDTH)}"))
        println()
    }

    private fun getEventTypeInfo(event: Event): Pair<String, (String) -> String> {
        return when (event) {
            is Event.AssistantResponse -> "AssistantResponse (助手响应)" to ::brightCyan
            is Event.ToolUse -> "ToolUse (工具使用)" to ::magenta
            is Event.Metering -> "Metering (计费)" to ::dim
            is Event.ContextUsage -> "ContextUsage (上下文使用率)" to ::blue
            is Event.ToolCallRequest -> "ToolCallRequest (工具调用请求)" to ::yellow
            is Event.ToolCallError -> "ToolCallError (工具调用错误)" to ::red
            is Event.SessionStart -> "SessionStart (会话开始)" to ::green
            is Event.SessionEnd -> "SessionEnd (会话结束)" to ::green
            is Event.Unknown -> "Unknown (未知事件)" to ::brightBlack
            is Event.Error -> "Error (服务端错误)" to ::brightRed
            is Event.Exception -> "Exception (服务端异常)" to ::brightRed
        }
    }

    private fun printField(label: String, value: String?) {
        val displayValue = value ?: dim("(无)")
        println(cyan("│") + " ${dim(label)}: $displayValue")
    }

    private fun printContent(label: String, content: String?, maxLength: Int = 200) {
        println(cyan("│") + " ${dim(label)}:")
        if (content.isNullOrEmpty()) {
            println(cyan("│") + "   " + dim("(无)"))
            return
        }

        if (content.length > maxLength) {
            val truncated = content.take(maxLength)
            truncated.lines().forEach { line ->
                println(cyan("│") + "   $line")
            }
            println(cyan("│") + "   " + dim("... (已截断，总长度: ${content.length})"))
        } else {
            content.lines().forEach { line ->
                println(cyan("│") + "   $line")
            }
        }
    }
}
