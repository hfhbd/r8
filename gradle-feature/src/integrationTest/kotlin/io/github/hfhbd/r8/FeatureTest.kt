package io.github.hfhbd.r8

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import java.util.jar.Attributes
import java.util.jar.JarFile
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class FeatureTest {
    @Test
    fun generatesSimplifiedJarWithApplicationPluginAndNoToolchain() {
        val projectDir = createTempDirectory("integration-test").toFile()
        File(projectDir, "settings.gradle.kts").writeText(
            // language=kotlin
            """
               |pluginManagement {
               |    repositories {
               |        mavenCentral()
               |        gradlePluginPortal()
               |        maven {
               |            url = uri("https://raw.githubusercontent.com/Kotlin/declarative-gradle-jetbrains-ecosystem-plugin/refs/heads/maven2")
               |        }
               |    }
               |}
               |
               |
               |plugins {
               |    id("org.jetbrains.ecosystem")
               |    id("io.github.hfhbd.r8.features")
               |}
               |
               |dependencyResolutionManagement {
               |    repositories {
               |        mavenCentral()
               |        google()
               |    }
               |}
""".trimMargin()
        )

        File(projectDir, "build.gradle.dcl").writeText(
            // language=kotlin
            """
               |jvmApplication {
               |  mainClass = "com.example.Main"
               |  r8 {}
               |}
               |
           """.trimMargin()
        )

        val r8Jar = assertBuild(projectDir)
        val jarFile = JarFile(r8Jar)
        assertEquals("com.example.Main", jarFile.manifest.mainAttributes[Attributes.Name.MAIN_CLASS])
    }

    @Test
    fun applicationPluginWithoutMainClass() {
        val projectDir = createTempDirectory("integration-test").toFile()
        File(projectDir, "settings.gradle.kts").writeText(
            // language=kotlin
            """
               |pluginManagement {
               |    repositories {
               |        mavenCentral()
               |        gradlePluginPortal()
               |        maven {
               |            url = uri("https://raw.githubusercontent.com/Kotlin/declarative-gradle-jetbrains-ecosystem-plugin/refs/heads/maven2")
               |        }
               |    }
               |}
               |
               |
               |plugins {
               |    id("org.jetbrains.ecosystem")
               |    id("io.github.hfhbd.r8.features")
               |}
               |
               |dependencyResolutionManagement {
               |    repositories {
               |        mavenCentral()
               |        google()
               |    }
               |}
""".trimMargin()
        )

        File(projectDir, "build.gradle.dcl").writeText(
            // language=kotlin
            """
               |jvmApplication {
               |  r8 {
               |      additionalRules += listOf("-keep public class com.example.Main { public static void main(java.lang.String[]); }")
               |  }
               |}
               |
           """.trimMargin()
        )

        val r8Jar = assertBuild(projectDir)
        val jarFile = JarFile(r8Jar)
        assertNull(jarFile.manifest.mainAttributes[Attributes.Name.MAIN_CLASS])
    }

    @Test
    fun generatesSimplifiedJarWithApplicationPluginAndJvm8Toolchain() {
        val projectDir = createTempDirectory("integration-test").toFile()
        File(projectDir, "settings.gradle.kts").writeText(
            // language=kotlin
            """
               |pluginManagement {
               |    repositories {
               |        mavenCentral()
               |        gradlePluginPortal()
               |        maven {
               |            url = uri("https://raw.githubusercontent.com/Kotlin/declarative-gradle-jetbrains-ecosystem-plugin/refs/heads/maven2")
               |        }
               |    }
               |}
               |
               |
               |plugins {
               |    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
               |    id("org.jetbrains.ecosystem")
               |    id("io.github.hfhbd.r8.features")
               |}
               |
               |dependencyResolutionManagement {
               |    repositories {
               |        mavenCentral()
               |        google()
               |    }
               |}
""".trimMargin()
        )

        File(projectDir, "build.gradle.dcl").writeText(
            // language=kotlin
            """
               |jvmApplication {
               |  mainClass = "com.example.Main"
               |  toolchain.releaseVersion = 8
               |  r8 {}
               |}
               |
           """.trimMargin()
        )

        val r8Jar = assertBuild(projectDir)
        val jarFile = JarFile(r8Jar)
        assertEquals("com.example.Main", jarFile.manifest.mainAttributes[Attributes.Name.MAIN_CLASS])
    }

    private fun assertBuild(projectDir: File) : File {
        File(projectDir, "src/main/java/com/example/Main.java").apply {
            parentFile.mkdirs()

            writeText(
                // language=java
                """package com.example;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}"""
            )
        }

        val isDebug = System.getenv("DEBUGGER_ENABLED") == "true"

        val build = GradleRunner.create()
            .withProjectDir(projectDir)
            .withDebug(isDebug)
            .withPluginClasspath()
            .forwardOutput()
            .withArguments(":r8")
            .build()

        assertEquals(TaskOutcome.SUCCESS, build.task(":r8")?.outcome)
        val r8Jar = File(projectDir, "build/r8/r8.jar")
        assertTrue(r8Jar.exists())

        val outputStream = ByteArrayOutputStream().use { output ->
            val printStream = PrintStream(output)
            try {
                URLClassLoader(arrayOf(r8Jar.toURI().toURL())).use {
                    System.setOut(printStream)
                    val mainClass = it.loadClass("com.example.Main")
                    val mainMethod = mainClass.getMethod("main", Array<String>::class.java)
                    mainMethod.invoke(null, arrayOf<String>())
                    output.flush()
                    output
                }
            } catch (e: ClassNotFoundException) {
                fail("Could not load r8 jar at ${r8Jar.toURI()}", e)
            }
        }
        assertEquals("Hello World\n", outputStream.toString())
        return r8Jar
    }
}
