plugins {
    id("setup")
}

dependencies {
    implementation(projects.gradleTasks)
    compileOnly(libs.kotlin.ecosystem.plugin)
}

val version = tasks.register("writeVersion",StoreVersion::class) {
    version.put("R8_MODULE", libs.r8.map { it.module.toString() })
    version.put("R8_VERSION", libs.r8.map { it.version.toString() })
    version.put("ANNOTATION_MODULE", libs.annotations.map { it.module.toString() })
    version.put("ANNOTATION_VERSION", libs.annotations.map { it.version.toString() })
}

sourceSets.main {
    kotlin.srcDir(version)
}

gradlePlugin.plugins.register("io.github.hfhbd.r8.features") {
    implementationClass = "io.github.hfhbd.r8.R8FeaturesPlugin"
    displayName = "hfhbd r8 Features Plugin"
    description = "hfhbd r8 Features Plugin"
}

configurations.configureEach {
    if (isCanBeConsumed) {
        attributes {
            attribute(GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, named(GradleVersion.current().version))
        }
    }
}

testing.suites.register("integrationTest", JvmTestSuite::class) {
    gradlePlugin.testSourceSet(sources)
    dependencies {
        implementation(gradleTestKit())
    }
}

val d = configurations.dependencyScope("d") {
    dependencies.add(libs.kotlin.ecosystem.plugin.get())
}
val r = configurations.resolvable("r") {
    extendsFrom(d)
}

tasks.pluginUnderTestMetadata {
    pluginClasspath.from(r)
}
