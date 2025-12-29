package com.github.hank9999.kirokt.config

/**
 * 命令行参数解析结果
 */
data class CliArgs(
    val configPath: String = "./config.json",
    val credentialsPath: String = "./credentials.json"
) {
    companion object {
        /**
         * 解析命令行参数
         *
         * 支持的参数:
         * - `--config <path>` 或 `-c <path>`: 指定配置文件路径
         * - `--credentials <path>` 或 `-C <path>`: 指定凭据文件路径
         */
        fun parse(args: Array<String>): CliArgs {
            var configPath = "./config.json"
            var credentialsPath = "./credentials.json"

            var i = 0
            while (i < args.size) {
                when (args[i]) {
                    "--config", "-c" -> {
                        if (i + 1 < args.size) {
                            configPath = args[++i]
                        } else {
                            throw IllegalArgumentException("缺少 --config 参数的值")
                        }
                    }
                    "--credentials", "-C" -> {
                        if (i + 1 < args.size) {
                            credentialsPath = args[++i]
                        } else {
                            throw IllegalArgumentException("缺少 --credentials 参数的值")
                        }
                    }
                    "--help", "-h" -> {
                        printHelp()
                        kotlin.system.exitProcess(0)
                    }
                    else -> {
                        if (args[i].startsWith("-")) {
                            throw IllegalArgumentException("未知参数: ${args[i]}")
                        }
                    }
                }
                i++
            }

            return CliArgs(configPath, credentialsPath)
        }

        private fun printHelp() {
            println("""
                |KIRO KT - Your next generation AI Client Tools
                |
                |用法: kiro-kt [选项]
                |
                |选项:
                |  -c, --config <path>       配置文件路径 (默认: ./config.json)
                |  -C, --credentials <path>  凭据文件路径 (默认: ./credentials.json)
                |  -h, --help                显示帮助信息
            """.trimMargin())
        }
    }
}
