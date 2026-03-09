package io.github.hfhbd.r8

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.attributes.java.TargetJvmVersion
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

abstract class R8Plugin : Plugin<Project> {
    override fun apply(target: Project) {
        val r8 = target.configurations.dependencyScope("r8")
        val r8ClasspathConfig = target.configurations.resolvable("r8Classpath") {
            extendsFrom(r8.get())
        }

        target.dependencies {
            r8.invoke(R8) {
                attributes {
                    // https://issuetracker.google.com/issues/377126124
                    attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 11)
                }
            }
        }

        target.tasks.withType<R8JarTask>().configureEach {
            this.r8Classpath.from(r8ClasspathConfig)
        }

        target.pluginManager.withPlugin("application") {
            val applicationExtension = target.extensions.getByName<JavaApplication>("application")

            target.tasks.named("jar", Jar::class) {
                manifest.attributes["Main-Class"] = applicationExtension.mainClass
            }

            val createR8Rule = target.tasks.register("createR8Rule", CreateR8RuleTask::class) {
                this.rules.add(applicationExtension.mainClass.map {
                    """-keep public class $it { public static void main(java.lang.String[]); }"""
                })
                this.moduleName.convention(project.name)
                this.outputDirectory.convention(project.layout.buildDirectory.dir("generated/r8-rules"))
            }
            val sourceSets = target.extensions.getByName<SourceSetContainer>("sourceSets")
            sourceSets.named("main") {
                resources.srcDir(createR8Rule)
            }

            target.tasks.register("r8", R8JarTask::class) {
                val toolchain = project.extensions.getByType<JavaPluginExtension>().toolchain

                targetJvmVersion.convention(toolchain.languageVersion)
                r8Jar.convention(target.layout.buildDirectory.file("r8/r8.jar"))
                inputJars.from(target.tasks.named("jar"), target.configurations.named("runtimeClasspath"))
            }
        }
    }
}
