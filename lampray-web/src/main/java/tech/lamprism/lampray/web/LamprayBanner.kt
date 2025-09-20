/*
 * Copyright (C) 2023 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package tech.lamprism.lampray.web

import org.springframework.boot.Banner
import org.springframework.boot.ansi.AnsiColor
import org.springframework.boot.ansi.AnsiOutput
import org.springframework.boot.ansi.AnsiStyle
import org.springframework.core.env.Environment
import tech.lamprism.lampray.Version
import java.io.PrintStream

/**
 * @author RollW
 */
class LamprayBanner : Banner {
    companion object {
        val BANNER: String = """
                __                                           
               / /   ____ _____ ___  ____  _________ ___  __ 
              / /   / __ `/ __ `__ \/ __ \/ ___/ __ `/ / / / 
             / /___/ /_/ / / / / / / /_/ / /  / /_/ / /_/ /  
            /_____/\__,_/_/ /_/ /_/ .___/_/   \__,_/\__, /   
                                 /_/               /____/    

			""".trimIndent()

        const val LAMPRAY = "Lampray"

        private const val LICENSE = "Release under the Apache License, Version 2.0 (Apache-2.0)"

        private const val STRAP_LINE_SIZE = LICENSE.length
    }

    override fun printBanner(
        environment: Environment,
        sourceClass: Class<*>,
        printStream: PrintStream
    ) {
        printStream.run {
            println()
            printCenteredBanner(STRAP_LINE_SIZE)
            val version = " (v${Version.VERSION})"
            val padding = StringBuilder()
            while (padding.length < STRAP_LINE_SIZE - (version.length + LAMPRAY.length)) {
                padding.append(" ")
            }
            println(
                AnsiOutput.toString(
                    AnsiColor.YELLOW, LAMPRAY, AnsiColor.DEFAULT, padding.toString(),
                    AnsiStyle.FAINT, AnsiColor.GREEN, version, AnsiColor.DEFAULT
                )
            )
            println(LICENSE)
            println()
            print(Version.formatVersion())
        }
    }

    private fun PrintStream.printCenteredBanner(length: Int) {
        val maxLine = BANNER.lines().maxOf { it.length }
        if (maxLine > length) {
            println(BANNER)
            return
        }
        val padding = (length - maxLine) / 2
        BANNER.lines().forEach { line ->
            repeat(padding) {
                print(" ")
            }
            println(line)
        }
    }
}
