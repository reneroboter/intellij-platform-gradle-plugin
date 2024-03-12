// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

package org.jetbrains.intellij.platform.gradle.tasks

import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.intellij.platform.gradle.Constants.Tasks
import org.jetbrains.intellij.platform.gradle.IntelliJPluginTestBase
import kotlin.io.path.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SignPluginTaskTest : IntelliJPluginTestBase() {

    private val tripleQuote = "\"\"\""

    @Test
    fun `run Marketplace ZIP Signer in the latest version`() {
        buildFile.kotlin(
            """
            dependencies {
                intellijPlatform {
                    zipSigner()
                }            
            }
            
            intellijPlatform {
                signing {
                    certificateChainFile = file("${resource("certificates/cert.crt")}")
                    privateKeyFile = file("${resource("certificates/cert.key")}")
                }
            }
            """.trimIndent()
        )

        build(Tasks.SIGN_PLUGIN, "--debug") {
            val message = "'Marketplace ZIP Signer specified with dependencies' resolved as: "
            val line = output.lines().find { it.contains(message) }
            assertNotNull(line)

            val path = Path(line.substringAfter(message))
            assertTrue(path.exists())
            assertTrue(path.name.startsWith("marketplace-zip-signer-"))
            assertEquals("jar", path.extension)
        }
    }

    @Test
    fun `run Marketplace ZIP Signer in specified version using certificateChainFile and privateKeyFile`() {
        buildFile.kotlin(
            """
            dependencies {
                intellijPlatform {
                    zipSigner("0.1.21")
                }            
            }
            
            intellijPlatform {
                signing {
                    certificateChainFile = file("${resource("certificates/cert.crt")}")
                    privateKeyFile = file("${resource("certificates/cert.key")}")
                }
            }
            """.trimIndent()
        )

        build(Tasks.SIGN_PLUGIN, "--debug") {
            assertContains("marketplace-zip-signer-0.1.21-cli.jar", output)
        }
    }

    @Test
    fun `fail on signing without password provided`() {
        buildFile.kotlin(
            """
            dependencies {
                intellijPlatform {
                    zipSigner()
                }            
            }
            
            intellijPlatform {
                signing {
                    certificateChainFile = file("${resource("certificates-password/chain.crt")}")
                    privateKeyFile = file("${resource("certificates-password/private_encrypted.pem")}")
                }
            }
            """.trimIndent()
        )

        buildAndFail(Tasks.SIGN_PLUGIN) {
            assertContains("Can't read private key. Password is missing", output)
        }
    }

    @Test
    fun `fail on signing with incorrect password provided`() {
        buildFile.kotlin(
            """
            dependencies {
                intellijPlatform {
                    zipSigner()
                }            
            }
            
            intellijPlatform {
                signing {
                    certificateChainFile = file("${resource("certificates-password/chain.crt")}")
                    privateKeyFile = file("${resource("certificates-password/private_encrypted.pem")}")
                    password = "incorrect"
                }
            }
            """.trimIndent()
        )

        buildAndFail(Tasks.SIGN_PLUGIN) {
            assertContains("unable to read encrypted data", output)
        }
    }

    @Test
    fun `sign plugin with password provided`() {
        buildFile.kotlin(
            """
            dependencies {
                intellijPlatform {
                    zipSigner()
                }            
            }
            
            intellijPlatform {
                signing {
                    certificateChainFile = file("${resource("certificates-password/chain.crt")}")
                    privateKeyFile = file("${resource("certificates-password/private_encrypted.pem")}")
                    password = "foobar"
                }
            }
            """.trimIndent()
        )

        build(Tasks.SIGN_PLUGIN)
    }

    @Test
    fun `run Marketplace ZIP Signer in specified version using certificateChain and privateKey`() {
        buildFile.kotlin(
            """
            dependencies {
                intellijPlatform {
                    zipSigner("0.1.21")
                }            
            }
            
            intellijPlatform {
                signing {
                    certificateChain = $tripleQuote${resourceContent("certificates/cert.crt")}$tripleQuote
                    privateKey = $tripleQuote${resourceContent("certificates/cert.key")}$tripleQuote
                    password = "incorrect"
                }
            }
            """.trimIndent()
        )

        build(Tasks.SIGN_PLUGIN, "--info") {
            assertContains("marketplace-zip-signer-0.1.21-cli.jar", output)
        }
    }

    @Test
    fun `skip Marketplace ZIP Signer task if no key and certificateChain were provided`() {
        buildFile.kotlin(
            """
            dependencies {
                intellijPlatform {
                    zipSigner()
                }            
            }
            """.trimIndent()
        )

        build(Tasks.SIGN_PLUGIN) {
            assertEquals(TaskOutcome.SKIPPED, task(":${Tasks.SIGN_PLUGIN}")?.outcome)
        }
    }

    @Test
    fun `run Marketplace ZIP Signer and fail on invalid version`() {
        buildFile.kotlin(
            """
            dependencies {
                intellijPlatform {
                    zipSigner("0.0.1")
                }            
            }
            """.trimIndent()
        )

        build(
            gradleVersion = gradleVersion,
            fail = true,
            assertValidConfigurationCache = false,
            Tasks.SIGN_PLUGIN,
        ) {
            assertContains("Could not find org.jetbrains:marketplace-zip-signer:0.0.1.", output)
        }
    }

    @Test
    fun `output file contains version when specified in build file`() {
        buildFile.kotlin(
            """
            dependencies {
                intellijPlatform {
                    zipSigner()
                }            
            }
            
            intellijPlatform {
                signing {
                    certificateChainFile = file("${resource("certificates/cert.crt")}")
                    privateKeyFile = file("${resource("certificates/cert.key")}")
                }
            }
            """.trimIndent()
        )

        build(Tasks.SIGN_PLUGIN)

        val distributionFolder = buildDirectory.resolve("distributions")
        assertTrue(distributionFolder.listDirectoryEntries().any {
            it.name == "projectName-1.0.0-signed.zip"
        })
    }
}
