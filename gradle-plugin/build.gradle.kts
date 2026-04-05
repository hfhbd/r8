plugins {
    id("java-gradle-plugin")
    id("setup")
}

kotlin {
    jvmToolchain(17)
    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation.enabled = true
}

gradlePlugin.plugins.register("io.github.hfhbd.r8") {
    id = name
    implementationClass = "io.github.hfhbd.r8.R8Plugin"
    displayName = "hfhbd githubReleasesWorker Gradle Plugin"
    description = "hfhbd githubReleasesWorker Gradle Plugin"
}

tasks.validatePlugins {
    enableStricterValidation.set(true)
}

val version by tasks.registering(StoreVersion::class) {
    version.put("R8_MODULE", libs.r8.map { it.module.toString() })
    version.put("R8_VERSION", libs.r8.map { it.version.toString() })
}

sourceSets.main {
    kotlin.srcDir(version)
}

testing.suites.register("integrationTest", JvmTestSuite::class) {
    gradlePlugin.testSourceSet(sources)
    dependencies {
        implementation(gradleTestKit())
    }
}
