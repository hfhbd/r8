package io.github.hfhbd.r8

import org.gradle.api.Plugin
import org.gradle.api.initialization.Settings
import org.gradle.features.annotations.RegistersProjectFeatures

@RegistersProjectFeatures(R8Feature::class)
abstract class EcosystemPlugin : Plugin<Settings> {
    override fun apply(target: Settings) {}
}
