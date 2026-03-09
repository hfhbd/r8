package io.github.hfhbd.r8

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class CreateR8RuleTask : DefaultTask() {
    init {
        group = "r8"
    }

    @get:Input
    abstract val rules: ListProperty<String>

    @get:Input
    abstract val moduleName: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    protected fun createR8Rule() {
        File(outputDirectory.get().asFile, "META-INF/proguard/${moduleName.get()}.pro").apply {
            parentFile.mkdirs()
        }.writeText(rules.get().joinToString("\n", postfix = "\n"))
    }
}
