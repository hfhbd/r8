package io.github.hfhbd.r8

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.features.annotations.RegistersProjectFeatures

@RegistersProjectFeatures(R8Feature::class)
abstract class R8FeaturesPlugin : Plugin<Settings> {
    override fun apply(target: Settings) {
        target.dependencyResolutionManagement.components.withModule(R8_MODULE, R8VersionRule::class.java)
        target.dependencyResolutionManagement.versionCatalogs.register("r8Libs") {
            val versionAlias = it.version("r8", R8_VERSION)
            it.library("r8", "com.android.tools", "r8")
                .versionRef(versionAlias)

            val annotationAlias = it.version("annotation", ANNOTATION_VERSION)
            it.library("annotation", "androidx.annotation", "annotation")
                .versionRef(annotationAlias)
        }
    }
}
