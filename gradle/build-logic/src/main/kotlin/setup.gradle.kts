plugins {
    id("java-library")
    id("maven-publish")
    id("signing")
    id("io.github.hfhbd.mavencentral")
    id("dev.sigstore.sign")
}

testing.suites.withType(JvmTestSuite::class).configureEach {
    useKotlinTest()
}

java {
    withJavadocJar()
    withSourcesJar()
}

configurations.configureEach {
    if (isCanBeConsumed) {
        attributes {
            attribute(GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE, named(GradleVersion.current().version))
        }
    }
}

// Workaround for clash between `signature` and `archives`; remove when bumping to Gradle 10:
configurations.archives {
    attributes {
        attribute(Attribute.of("deprecated", String::class.java), "true")
    }
}

publishing {
    repositories {
        maven(url = "https://maven.pkg.github.com/hfhbd/r8") {
            name = "GitHubPackages"
            credentials(PasswordCredentials::class)
        }
    }
    publications.withType<MavenPublication>().configureEach {
        pom {
            name.set("hfhbd r8")
            description.set("hfhbd r8")
            url.set("https://github.com/hfhbd/r8")
            licenses {
                license {
                    name.set("Apache-2.0")
                    url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                }
            }
            developers {
                developer {
                    id.set("hfhbd")
                    name.set("Philip Wedemann")
                    email.set("mybztg+mavencentral@icloud.com")
                }
            }
            scm {
                connection.set("scm:git://github.com/hfhbd/r8.git")
                developerConnection.set("scm:git://github.com/hfhbd/r8.git")
                url.set("https://github.com/hfhbd/r8")
            }
            distributionManagement {
                repository {
                    id = "github"
                    name = "GitHub hfhbd Apache Maven Packages"
                    url = "https://maven.pkg.github.com/hfhbd/r8"
                }
            }
        }
    }
}

signing {
    useInMemoryPgpKeys(
        providers.gradleProperty("signingKey").orNull,
        providers.gradleProperty("signingPassword").orNull,
    )
    isRequired = providers.gradleProperty("signingKey").isPresent
    sign(publishing.publications)
}
