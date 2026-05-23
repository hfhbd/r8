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
import org.gradle.workers.WorkAction
import org.gradle.workers.WorkParameters
import org.gradle.workers.WorkerExecutor
import java.io.File
import java.nio.file.Path
import java.util.jar.Attributes
import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.jar.Manifest
import javax.inject.Inject

@CacheableTask
abstract class R8JarTask internal constructor() : DefaultTask() {
    init {
        group = "r8"
    }

    @get:Input
    @get:Optional
    abstract val mainClass: Property<String>

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:Classpath
    abstract val programFiles: ConfigurableFileCollection

    @get:Internal
    abstract val libJars: ConfigurableFileCollection

    @get:Input
    abstract val additionalRules: ListProperty<String>

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
            it.mainClass.set(mainClass)
            it.tempDir.set(temporaryDir)
            it.javaHome.set(javaHome)
            it.r8Jar.set(r8Jar)
            it.additionalRules.set(additionalRules)
            it.libJars.from(libJars)
            it.programFiles.from(programFiles.asFileTree)
        }
    }
}

interface R8WorkerParameters : WorkParameters {
    val mainClass: Property<String>
    val javaHome: DirectoryProperty
    val tempDir: DirectoryProperty
    val r8Jar: RegularFileProperty
    val additionalRules: ListProperty<String>
    val libJars: ConfigurableFileCollection
    val programFiles: ConfigurableFileCollection
}

abstract class R8Worker : WorkAction<R8WorkerParameters> {
    override fun execute() {
        if (parameters.mainClass.isPresent) {
            val tempR8Jar = File(parameters.tempDir.get().asFile, "r8.jar")
            val mainClass = parameters.mainClass.get()

            executeR8(
                outputJar = tempR8Jar.toPath(),
                rules = parameters.additionalRules.get() + """-keep public class $mainClass { public static void main(java.lang.String[]); }""",
                javaHome = parameters.javaHome.get().asFile.toPath(),
                libJars = parameters.libJars.map { it.toPath() },
                programFiles = parameters.programFiles.map { it.toPath() },
            )

            modifyJarMainClass(tempR8Jar, parameters.r8Jar.get().asFile, mainClass)
        } else {
            executeR8(
                outputJar = parameters.r8Jar.get().asFile.toPath(),
                rules = parameters.additionalRules.get(),
                javaHome = parameters.javaHome.get().asFile.toPath(),
                libJars = parameters.libJars.map { it.toPath() },
                programFiles = parameters.programFiles.map { it.toPath() },
            )
        }
    }
}


fun modifyJarMainClass(inputJarPath: File, outputJarPath: File, newMainClass: String) {
    val manifest = JarFile(inputJarPath).use { jarFile ->
        jarFile.manifest ?: Manifest().apply {
            mainAttributes[Attributes.Name.MANIFEST_VERSION] = "1.0"
        }
    }

    manifest.mainAttributes[Attributes.Name.MAIN_CLASS] = newMainClass

    JarFile(inputJarPath).use { inputJar ->
        outputJarPath.outputStream().use {
            JarOutputStream(it, manifest).use { outputJar ->
                inputJar.entries().asSequence().forEach { entry ->
                    if (entry.name != JarFile.MANIFEST_NAME) {
                        outputJar.putNextEntry(entry)
                        inputJar.getInputStream(entry).use { input ->
                            input.copyTo(outputJar)
                        }
                    }
                }
            }
        }
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
            .setEnableExperimentalKeepAnnotations(true)
            .build()
    )
}
