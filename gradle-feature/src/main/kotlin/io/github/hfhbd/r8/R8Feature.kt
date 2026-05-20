package io.github.hfhbd.r8

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.ComponentMetadataHandler
import org.gradle.api.artifacts.dsl.DependencyFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.features.annotations.BindsProjectFeature
import org.gradle.features.binding.BuildModel
import org.gradle.features.binding.Definition
import org.gradle.features.binding.ProjectFeatureApplicationContext
import org.gradle.features.binding.ProjectFeatureApplyAction
import org.gradle.features.binding.ProjectFeatureBinding
import org.gradle.features.binding.ProjectFeatureBindingBuilder
import org.gradle.features.dsl.bindProjectFeature
import org.gradle.features.file.ProjectFeatureLayout
import org.gradle.features.registration.ConfigurationRegistrar
import org.gradle.features.registration.TaskRegistrar
import org.jetbrains.kotlin.gradle.declarative.projecttypes.jvmapplication.JvmApplicationProjectType
import javax.inject.Inject

interface R8Definition : Definition<R8BuildModel> {
    val r8Version: Property<String>
    val additionalR8Rules: ListProperty<String>
}
interface R8BuildModel : BuildModel {
    val r8Version: Property<String>
    val additionalR8Rules: ListProperty<String>
}

@BindsProjectFeature(R8Feature::class)
abstract class R8Feature : Plugin<Project>, ProjectFeatureBinding {
    override fun apply(target: Project) {}
    override fun bind(builder: ProjectFeatureBindingBuilder) {
        builder.bindProjectFeature("r8", ApplyAction::class)
            .withUnsafeApplyAction()
    }

    abstract class ApplyAction : ProjectFeatureApplyAction<R8Definition, R8BuildModel, JvmApplicationProjectType> {
        @get:Inject
        internal abstract val dependencyFactory: DependencyFactory

        @get:Inject
        internal abstract val configurations: ConfigurationRegistrar

        @get:Inject
        internal abstract val components: ComponentMetadataHandler

        @get:Inject
        internal abstract val tasks: TaskRegistrar

        @get:Inject
        internal abstract val layout: ProjectFeatureLayout

        override fun apply(
            context: ProjectFeatureApplicationContext,
            definition: R8Definition,
            buildModel: R8BuildModel,
            parentDefinition: JvmApplicationProjectType,
        ) {
            val parentBuildModel = context.getBuildModel(parentDefinition)

            buildModel.r8Version.set(definition.r8Version.orElse(R8_VERSION))
            buildModel.additionalR8Rules.set(definition.additionalR8Rules)

            components.withModule(R8_MODULE, R8VersionRule::class.java)

            val r8 = configurations.dependencyScope("r8") {
                it.dependencies.addLater(buildModel.r8Version.map { r8Version ->
                    dependencyFactory.create("$R8_MODULE:$r8Version")
                })
            }
            val r8ClasspathConfig = configurations.resolvable("r8Classpath") {
                it.extendsFrom(r8)
            }

            parentBuildModel.applications.configureEach { application ->
                tasks.register("r8" + application.name, R8JarTask::class.java) {
                    it.rules.add(application.mainClassName.map {
                        """-keep public class $it { public static void main(java.lang.String[]); }"""
                    })
                    it.rules.addAll(buildModel.additionalR8Rules)
                    it.javaHome.set(application.jdkLauncher.map { it.metadata.installationPath })

                    it.r8Jar.convention(layout.contextBuildDirectory.map { it.file("r8/r8-${application.name}.jar") })
                    it.programFiles.from(application.runtimeClasspath)

                    it.r8Classpath.from(r8ClasspathConfig)
                }
            }
        }
    }
}
