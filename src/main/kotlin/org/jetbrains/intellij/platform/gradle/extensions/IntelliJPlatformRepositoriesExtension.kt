// Copyright 2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.extensions

import org.gradle.api.Action
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.maven
import org.jetbrains.intellij.platform.gradle.BuildFeature
import org.jetbrains.intellij.platform.gradle.IntelliJPluginConstants.Extensions
import javax.inject.Inject

internal typealias RepositoryAction = (MavenArtifactRepository.() -> Unit)

@Suppress("unused")
@IntelliJPlatform
abstract class IntelliJPlatformRepositoriesExtension @Inject constructor(
    private val repositories: RepositoryHandler,
    private val providers: ProviderFactory,
) {

    fun releases(action: RepositoryAction = {}) = createRepository(
        name = "IntelliJ Repository",
        url = "https://www.jetbrains.com/intellij-repository/releases",
        urlWithCacheRedirector = "https://cache-redirector.jetbrains.com/www.jetbrains.com/intellij-repository/releases",
        action = action,
    )

    fun snapshots(action: RepositoryAction = {}) = createRepository(
        name = "IntelliJ Repository (Snapshots)",
        url = "https://www.jetbrains.com/intellij-repository/snapshots",
        urlWithCacheRedirector = "https://cache-redirector.jetbrains.com/www.jetbrains.com/intellij-repository/snapshots",
        action = action,
    )

    fun nightly(action: RepositoryAction = {}) = createRepository(
        name = "IntelliJ Repository (Nightly)",
        url = "https://www.jetbrains.com/intellij-repository/nightly",
        urlWithCacheRedirector = "https://cache-redirector.jetbrains.com/www.jetbrains.com/intellij-repository/nightly",
        action = action,
    )

    fun marketplace(action: RepositoryAction = {}) = createRepository(
        name = "JetBrains Marketplace Repository",
        url = "https://plugins.jetbrains.com/maven",
        urlWithCacheRedirector = "https://cache-redirector.jetbrains.com/plugins.jetbrains.com/maven",
        action = action,
    )

    fun pluginVerifier(action: RepositoryAction = {}) = createRepository(
        name = "IntelliJ Plugin Verifier Repository",
        url = "https://packages.jetbrains.team/maven/p/intellij-plugin-verifier/intellij-plugin-verifier",
        urlWithCacheRedirector = "https://cache-redirector.jetbrains.com/packages.jetbrains.team/maven/p/intellij-plugin-verifier/intellij-plugin-verifier",
        action = action,
    )

    private fun createRepository(
        name: String,
        url: String,
        urlWithCacheRedirector: String = url,
        action: RepositoryAction = {},
    ) = BuildFeature.USE_CACHE_REDIRECTOR.getValue(providers).get().let { useCacheRedirector ->
        repositories.maven(
            when (useCacheRedirector) {
                true -> urlWithCacheRedirector
                false -> url
            }
        ) {
            this.name = name
            action()
        }
    }
}

fun RepositoryHandler.intellijPlatform(configure: Action<IntelliJPlatformRepositoriesExtension>) =
    (this as ExtensionAware).extensions.configure(Extensions.INTELLIJ_PLATFORM, configure)
