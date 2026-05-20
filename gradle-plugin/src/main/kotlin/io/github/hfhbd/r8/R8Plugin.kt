package io.github.hfhbd.r8

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.gradle.api.artifacts.dsl.DependencyFactory
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.TaskContainer
import org.gradle.jvm.toolchain.JavaToolchainService
import javax.inject.Inject

abstract class R8Plugin : Plugin<Project> {
    @get:Inject internal abstract val dependencyFactory: DependencyFactory
    @get:Inject internal abstract val configurations: ConfigurationContainer
    @get:Inject internal abstract val components: ComponentMetadataHandler
    @get:Inject internal abstract val tasks: TaskContainer
    @get:Inject internal abstract val javaToolchains: JavaToolchainService

    override fun apply(target: Project) {
        components.withModule(R8_MODULE, R8VersionRule::class.java)

        val r8 = configurations.dependencyScope("r8") {
            it.dependencies.add(dependencyFactory.create("$R8_MODULE:$R8_VERSION"))
        }
        val r8ClasspathConfig = configurations.resolvable("r8Classpath") {
            it.extendsFrom(r8)
        }

        tasks.withType(R8JarTask::class.java).configureEach {
            it.r8Classpath.from(r8ClasspathConfig)
        }

        target.pluginManager.withPlugin("application") {
            val applicationExtension = target.extensions.getByName("application") as JavaApplication

            target.tasks.register("r8", R8JarTask::class.java) {
                val toolchain = target.extensions.getByType(JavaPluginExtension::class.java).toolchain

                it.javaHome.convention(javaToolchains.launcherFor {
                    it.languageVersion.set(toolchain.languageVersion)
                }.map { it.metadata.installationPath })

                it.mainClass.set(applicationExtension.mainClass)

                it.r8Jar.convention(target.layout.buildDirectory.file("r8/r8.jar"))
                it.programFiles.from(tasks.named("jar"), target.configurations.named("runtimeClasspath"))
            }
        }
    }
}
