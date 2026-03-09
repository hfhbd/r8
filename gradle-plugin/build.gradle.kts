plugins {
    `kotlin-dsl`
    id("setup")
}

kotlin.jvmToolchain(17)

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
    version.put("R8", libs.r8.map { it.toString() })
}

sourceSets.main {
    kotlin.srcDir(version)
}
