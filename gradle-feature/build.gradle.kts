plugins {
    id("setup")
}

gradlePlugin.plugins.register("io.github.hfhbd.r8.features") {
    implementationClass = "io.github.hfhbd.r8.EcosystemPlugin"
    displayName = "hfhbd r8 Ecosystem Plugin"
    description = "hfhbd r8 Ecosystem Plugin"
}

val version = tasks.register("writeVersion",StoreVersion::class) {
    version.put("R8_MODULE", libs.r8.map { it.module.toString() })
    version.put("R8_VERSION", libs.r8.map { it.version.toString() })
}

sourceSets.main {
    kotlin.srcDir(version)
}

configurations.configureEach {
    if (isCanBeConsumed) {
        attributes {
            attribute(GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, named(GradleVersion.current().version))
        }
    }
}

dependencies {
    implementation(projects.gradleTasks)
    implementation(libs.kotlin.ecosystem.plugin)
}

testing.suites.register("integrationTest", JvmTestSuite::class) {
    gradlePlugin.testSourceSet(sources)
    dependencies {
        implementation(gradleTestKit())
    }
}
