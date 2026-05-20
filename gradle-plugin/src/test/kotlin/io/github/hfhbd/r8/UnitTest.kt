package io.github.hfhbd.r8

import org.gradle.api.plugins.JavaApplication
import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnitTest {
    @Test
    fun applyingWithApplicationPlugin() {
        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply("io.github.hfhbd.r8")
        project.pluginManager.apply("application")

        project.repositories.google()

        val javaApplication = project.extensions.getByName("application") as JavaApplication
        javaApplication.mainClass.set("com.example.Main")

        val r8Task = project.tasks.findByName("r8")
        assertTrue(r8Task is R8JarTask)

        assertEquals(
            listOf(
                "-keep public class com.example.Main { public static void main(java.lang.String[]); }",
            ),
            r8Task.rules.get(),
        )
        assertEquals("com.example.Main", r8Task.mainClass.get())

        val expectedOutputFile = project.file("build/r8/r8.jar")
        assertEquals(
            expectedOutputFile,
            r8Task.r8Jar.get().asFile
        )
    }

    @Test
    fun applyingWithoutApplicationPlugin() {
        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply("io.github.hfhbd.r8")

        val noR8Task = project.tasks.findByName("r8")
        assertNull(noR8Task)
    }
}
