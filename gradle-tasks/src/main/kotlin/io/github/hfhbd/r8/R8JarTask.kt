package io.github.hfhbd.r8

import com.android.tools.r8.CompilationMode
import com.android.tools.r8.JdkClassFileProvider
import com.android.tools.r8.OutputMode
import com.android.tools.r8.R8
import com.android.tools.r8.R8Command
import com.android.tools.r8.origin.Origin
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.nio.file.Path
import javax.inject.Inject

@CacheableTask
abstract class R8JarTask internal constructor() : DefaultTask() {
    init {
        group = "r8"
    }

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:Classpath
    abstract val programFiles: ConfigurableFileCollection

    @get:Internal
    abstract val libJars: ConfigurableFileCollection

    @get:Input
    abstract val rules: ListProperty<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val javaHome: DirectoryProperty

    @get:OutputFile
    abstract val r8Jar: RegularFileProperty

    @get:Classpath
    abstract val r8Classpath: ConfigurableFileCollection

    @get:Inject
    internal abstract val workerExecutor: WorkerExecutor

    @TaskAction
    internal fun createJar() {
        workerExecutor.classLoaderIsolation {
            it.classpath.from(r8Classpath)
        }.submit(R8Worker::class.java) {
            it.javaHome.set(javaHome)
            it.r8Jar.set(r8Jar)
            it.rules.set(rules)
            it.libJars.from(libJars)
            it.programFiles.from(programFiles)
        }
    }
}

interface R8WorkerParameters : WorkParameters {
    val javaHome: DirectoryProperty
    val r8Jar: RegularFileProperty
    val rules: ListProperty<String>
    val libJars: ConfigurableFileCollection
    val programFiles: ConfigurableFileCollection
}

abstract class R8Worker : WorkAction<R8WorkerParameters> {
    override fun execute() {
        executeR8(
            outputJar = parameters.r8Jar.get().asFile.toPath(),
            rules = parameters.rules.get(),
            javaHome = parameters.javaHome.get().asFile.toPath(),
            libJars = parameters.libJars.map { it.toPath() },
            programFiles = parameters.programFiles.map { it.toPath() },
        )
    }
}

private fun executeR8(
    outputJar: Path,
    rules: List<String>,
    javaHome: Path,
    libJars: List<Path>,
    programFiles: List<Path>,
) {
    R8.run(
        R8Command.builder()
            .setMode(CompilationMode.RELEASE)
            .setOutput(outputJar, OutputMode.ClassFile)
            .addProguardConfiguration(rules, Origin.unknown())
            .addLibraryResourceProvider(JdkClassFileProvider.fromJdkHome(javaHome))
            .addLibraryFiles(javaHome)
            .addLibraryFiles(libJars)
            .addProgramFiles(programFiles)
            .build()
    )
}
