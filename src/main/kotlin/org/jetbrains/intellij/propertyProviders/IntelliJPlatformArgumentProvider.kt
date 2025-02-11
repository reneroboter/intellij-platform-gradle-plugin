// Copyright 2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.propertyProviders

import com.jetbrains.plugin.structure.base.utils.exists
import com.jetbrains.plugin.structure.base.utils.readLines
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity.RELATIVE
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.process.JavaForkOptions
import org.jetbrains.intellij.Version
import org.jetbrains.intellij.ideBuildNumber
import org.jetbrains.intellij.ideProductInfo
import org.jetbrains.intellij.resolveIdeHomeVariable
import org.jetbrains.intellij.utils.OpenedPackages
import java.nio.file.Path

class IntelliJPlatformArgumentProvider(
    @InputDirectory @PathSensitive(RELATIVE) val ideDirectory: Path,
    @InputFile @PathSensitive(RELATIVE) val coroutinesJavaAgentPath: Path,
    private val options: JavaForkOptions,
) : CommandLineArgumentProvider {

    private val buildNumber by lazy {
        ideDirectory
            .let(::ideBuildNumber)
            .split('-')
            .last()
            .let(Version::parse)
    }

    private val productInfo
        get() = ideProductInfo(ideDirectory)

    private val bootclasspath
        get() = ideDirectory
            .resolve("lib/boot.jar")
            .takeIf { it.exists() }
            ?.let { listOf("-Xbootclasspath/a:$it") }
            .orEmpty()

    private val vmOptions
        get() = productInfo
            ?.currentLaunch
            ?.vmOptionsFilePath
            ?.removePrefix("../")
            ?.let { ideDirectory.resolve(it).readLines() }
            .orEmpty()
            .filter { !it.contains("kotlinx.coroutines.debug=off") }

    private val kotlinxCoroutinesJavaAgent
        get() = "-javaagent:$coroutinesJavaAgentPath".takeIf {
            buildNumber >= Version(221)
        }

    private val additionalJvmArguments
        get() = productInfo
            ?.currentLaunch
            ?.additionalJvmArguments
            ?.filterNot { it.startsWith("-D") }
            ?.takeIf { it.isNotEmpty() }
            ?.map { it.resolveIdeHomeVariable(ideDirectory) }
            ?: OpenedPackages

    private val defaultHeapSpace = listOf("-Xmx512m", "-Xms256m")
    private val heapSpace
        get() = listOfNotNull(
            options.maxHeapSize?.let { "-Xmx${it}" },
            options.minHeapSize?.let { "-Xms${it}" },
        )

    override fun asArguments() =
        (defaultHeapSpace + bootclasspath + vmOptions + kotlinxCoroutinesJavaAgent + additionalJvmArguments + heapSpace)
            .filterNot { it.isNullOrBlank() }
}
