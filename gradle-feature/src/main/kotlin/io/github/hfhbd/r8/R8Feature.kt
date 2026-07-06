package io.github.hfhbd.r8

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyCollector
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
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import javax.inject.Inject

interface R8Definition : Definition<R8BuildModel> {
    val r8Version: Property<String>
    val additionalRules: ListProperty<String>
}
interface R8BuildModel : BuildModel {
    val r8Version: Property<String>
    val additionalRules: ListProperty<String>
}

abstract class DefaultR8BuildModel : R8BuildModel {
    abstract val r8DependencyCollector: DependencyCollector
}

@BindsProjectFeature(R8Feature::class)
abstract class R8Feature : Plugin<Project>, ProjectFeatureBinding {
    override fun apply(target: Project) {}
    override fun bind(builder: ProjectFeatureBindingBuilder) {
        builder.bindProjectFeature("r8", ApplyAction::class)
            .withBuildModelImplementationType(DefaultR8BuildModel::class.java)
    }

    abstract class ApplyAction : ProjectFeatureApplyAction<R8Definition, R8BuildModel, JvmApplicationProjectType> {
        @get:Inject
        internal abstract val dependencyFactory: DependencyFactory

        @get:Inject
        internal abstract val configurations: ConfigurationRegistrar

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
            buildModel.additionalRules.set(definition.additionalRules)

            buildModel as DefaultR8BuildModel
            buildModel.r8DependencyCollector.add(
                buildModel.r8Version.map { r8Version ->
                    dependencyFactory.create("$R8_MODULE:$r8Version")
                }
            )

            val r8ClasspathConfig = configurations.resolvable("r8Classpath") {
                it.fromDependencyCollector(buildModel.r8DependencyCollector)
            }

            parentBuildModel.applications.all { application ->
                val name = application.name.takeUnless { it == KotlinCompilation.MAIN_COMPILATION_NAME }

                tasks.register("r8" + (name ?: "") , R8JarTask::class.java) {
                    it.mainClass.set(application.mainClassName)
                    it.additionalRules.addAll(buildModel.additionalRules)
                    it.javaHome.set(application.jdkLauncher.map { it.metadata.installationPath })

                    it.r8Jar.convention(layout.contextBuildDirectory.map {
                        val appName = name?.let { "-$it"} ?: ""
                        it.file("r8/r8$appName.jar")
                    })
                    it.programFiles.from(application.jvmCompilationUnit.outputs, application.runtimeClasspath)

                    it.r8Classpath.from(r8ClasspathConfig)
                }
            }
        }
    }
}
