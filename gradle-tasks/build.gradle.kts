plugins {
    id("setup")
}

dependencies {
    compileOnly(libs.r8)
}

val version = tasks.register("writeVersion",StoreVersion::class) {
    version.put("R8_MODULE", libs.r8.map { it.module.toString() })
    version.put("R8_VERSION", libs.r8.map { it.version.toString() })
}

sourceSets.main {
    kotlin.srcDir(version)
}
