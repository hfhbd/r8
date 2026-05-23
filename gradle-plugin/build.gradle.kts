plugins {
    id("setup")
}

dependencies {
    api(projects.gradleTasks)
}

val version = tasks.register("writeVersion",StoreVersion::class) {
    version.put("R8_MODULE", libs.r8.map { it.module.toString() })
    version.put("R8_VERSION", libs.r8.map { it.version.toString() })
    version.put("ANNOTATIONS", libs.annotations.map { it.toString() })
}

sourceSets.main {
    kotlin.srcDir(version)
}

gradlePlugin.plugins.register("io.github.hfhbd.r8") {
    implementationClass = "io.github.hfhbd.r8.R8Plugin"
    displayName = "hfhbd r8 Plugin"
    description = "hfhbd r8 Plugin"
}

configurations.configureEach {
    if (isCanBeConsumed) {
        attributes {
            attribute(GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, named("9.4.0"))
        }
    }
}

testing.suites.register("integrationTest", JvmTestSuite::class) {
    gradlePlugin.testSourceSet(sources)
    dependencies {
        implementation(gradleTestKit())
    }
}
