package io.github.hfhbd.r8

import org.gradle.api.artifacts.CacheableRule
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.attributes.Usage
import org.gradle.api.attributes.java.TargetJvmVersion
import javax.inject.Inject

@CacheableRule
internal abstract class R8VersionRule @Inject constructor(
    val jvmVersion: Int,
) : ComponentMetadataRule {
    override fun execute(context: ComponentMetadataContext) {
        context.details.withVariant("compile") {
            it.attributes {
                it.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, jvmVersion)
                it.attribute(Usage.USAGE_ATTRIBUTE, it.named(Usage::class.java, Usage.JAVA_API))
            }
        }
    }
}
