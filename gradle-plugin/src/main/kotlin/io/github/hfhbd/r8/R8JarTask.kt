package io.github.hfhbd.r8

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.process.CommandLineArgumentProvider
import org.gradle.process.ExecOperations
import javax.inject.Inject

@CacheableTask
abstract class R8JarTask : DefaultTask() {
    init {
        group = "r8"
    }

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:Classpath
    abstract val inputJars: ConfigurableFileCollection

    @get:Internal
    abstract val libJars: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val rules: ConfigurableFileCollection

    @get:Input
    abstract val targetJvmVersion: Property<JavaLanguageVersion>

    @get:OutputFile
    abstract val r8Jar: RegularFileProperty

    @get:Classpath
    internal abstract val r8Classpath: ConfigurableFileCollection

    @get:Inject
    internal abstract val javaExec: ExecOperations

    @get:Inject
    internal abstract val javaToolchains: JavaToolchainService

    @TaskAction
    internal fun createJar() {
        javaExec.javaexec {
            val launcher = javaToolchains.launcherFor {
                languageVersion.set(targetJvmVersion)
            }

            classpath(r8Classpath)
            mainClass.set("com.android.tools.r8.R8")
            argumentProviders.add(
                CommandLineArgumentProvider {
                    buildList {
                        add("--release")
                        add("--classfile")

                        add("--output")
                        add(r8Jar.get().asFile.absolutePath)

                        for (rule in rules) {
                            add("--pg-conf")
                            add(rule.absolutePath)
                        }

                        add("--lib")
                        add(launcher.get().metadata.installationPath.asFile.absolutePath)

                        for (libJars in libJars) {
                            add("--lib")
                            add(libJars.absolutePath)
                        }

                        for (inputJar in inputJars) {
                            add(inputJar.absolutePath)
                        }
                    }
                },
            )
        }
    }
}
