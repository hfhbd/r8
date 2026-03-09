# r8

Wrapper for [r8](https://r8.googlesource.com/r8) to use it with the `application` plugin.

## Install

This package/Gradle plugin is uploaded to MavenCentral and GitHub packages.

```kotlin
// settings.gradle (.kts)
pluginManagement {
  repositories {
    mavenCentral()
  }
}
```

## Usage

Apply the plugin in each project.

```kotlin
// build.gradle (.kts)
plugins {
  id("io.github.hfhbd.r8") version "LATEST"
  id("application")
}

application {
    mainClass.set("HelloWorld")
}
```
This setup will create a minified jar with all dependencies and the `Main-Class` attribute under `build/r8/r8.jar`.

Without the `application` plugin, you need to setup a R8 task by yourself.
