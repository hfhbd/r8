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

        val createR8RuleTask = project.tasks.findByName("createR8Rule")
        assertTrue(createR8RuleTask is CreateR8RuleTask)
        assertEquals(
            listOf(
                "-keep public class com.example.Main { public static void main(java.lang.String[]); }",
            ),
            createR8RuleTask.rules.get(),
        )
        assertEquals(createR8RuleTask.moduleName.get(), project.name)
        val expectedOutputFile = project.file("build/generated/r8/rules")
        assertEquals(
            expectedOutputFile,
            createR8RuleTask.outputDirectory.get().asFile
        )

        createR8RuleTask.createR8Rule()
        assertEquals(
            """-keep public class com.example.Main { public static void main(java.lang.String[]); }
                |
            """.trimMargin(),
            File(expectedOutputFile, "META-INF/com.android.tools/r8/test.pro").readText())

        val r8Task = project.tasks.findByName("r8")
        assertTrue(r8Task is R8JarTask)
    }

    @Test
    fun applyingWithoutApplicationPlugin() {
        val project = ProjectBuilder.builder().build()

        project.pluginManager.apply("io.github.hfhbd.r8")

        val noR8Task = project.tasks.findByName("r8")
        assertNull(noR8Task)

        val noCreateR8RuleTask = project.tasks.findByName("createR8Rule")
        assertNull(noCreateR8RuleTask)
    }
}
