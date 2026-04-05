package io.github.hfhbd.r8

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.gradle.api.artifacts.dsl.DependencyFactory
import org.gradle.api.file.ProjectLayout
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Jar
import javax.inject.Inject

abstract class R8Plugin internal constructor(): Plugin<Project> {
    @get:Inject internal abstract val dependencyFactory: DependencyFactory
    @get:Inject internal abstract val configurations: ConfigurationContainer
    @get:Inject internal abstract val components: ComponentMetadataHandler
    @get:Inject internal abstract val tasks: TaskContainer
    @get:Inject internal abstract val layout: ProjectLayout
    @get:Inject internal abstract val sourceSets: SourceSetContainer

    override fun apply(target: Project) {
        components.withModule(R8_MODULE, R8VersionRule::class.java) {
            it.params(8)
        }

        val r8 = configurations.dependencyScope("r8") {
            it.dependencies.add(dependencyFactory.create("$R8_MODULE:$R8_VERSION"))
        }
        val r8ClasspathConfig = configurations.resolvable("r8Classpath") {
            it.extendsFrom(r8.get())
        }

        tasks.withType(R8JarTask::class.java).configureEach {
            it.r8Classpath.from(r8ClasspathConfig)
        }

        target.pluginManager.withPlugin("application") {
            val applicationExtension = target.extensions.getByName("application") as JavaApplication

            tasks.named("jar", Jar::class.java).configure {
                it.manifest.attributes["Main-Class"] = applicationExtension.mainClass
            }

            val createR8Rule = tasks.register("createR8Rule", CreateR8RuleTask::class.java) {
                it.rules.convention(applicationExtension.mainClass.map {
                    listOf("""-keep public class $it { public static void main(java.lang.String[]); }""")
                })
                it.moduleName.convention(target.name)
                it.outputDirectory.convention(layout.buildDirectory.dir("generated/r8/rules"))
            }

            sourceSets.named("main") {
                it.resources.srcDir(createR8Rule)
            }

            target.tasks.register("r8", R8JarTask::class.java) {
                val toolchain = target.extensions.getByType(JavaPluginExtension::class.java).toolchain

                it.targetJvmVersion.convention(toolchain.languageVersion)
                it.r8Jar.convention(target.layout.buildDirectory.file("r8/r8.jar"))
                it.inputJars.from(target.tasks.named("jar"), target.configurations.named("runtimeClasspath"))
            }
        }
    }
}
