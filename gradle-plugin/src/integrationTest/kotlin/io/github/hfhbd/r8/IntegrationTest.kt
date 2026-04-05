package io.github.hfhbd.r8

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.net.URLClassLoader
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

class IntegrationTest {
    @Test
    fun generatesSimplifiedJarWithApplicationPluginAndNoToolchain() {
        val projectDir = createTempDirectory("integration-test").toFile()
        File(projectDir, "build.gradle.kts").writeText(
            // language=kotlin
            """
               |plugins {
               |  id("io.github.hfhbd.r8")
               |  id("application")
               |}
               |
               |repositories.google()
               |
               |application.mainClass = "com.example.Main"
               |
           """.trimMargin()
        )

        assertBuild(projectDir)
    }


    @Test
    fun generatesSimplifiedJarWithApplicationPluginAndJvm8Toolchain() {
        val projectDir = createTempDirectory("integration-test").toFile()
        File(projectDir, "settings.gradle.kts").writeText(
            // language=kotlin
            """
               |plugins {
               |  id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
               |}
               |
           """.trimMargin()
        )

        File(projectDir, "build.gradle.kts").writeText(
            // language=kotlin
            """
               |plugins {
               |  id("io.github.hfhbd.r8")
               |  id("application")
               |}
               |
               |repositories.google()
               |
               |java {
               |    toolchain {
               |        languageVersion = JavaLanguageVersion.of(8)
               |    }
               |}
               |
               |application.mainClass = "com.example.Main"
               |
           """.trimMargin()
        )

        assertBuild(projectDir)
    }

    private fun assertBuild(projectDir: File) {
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

        val build = GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
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
    }
}
