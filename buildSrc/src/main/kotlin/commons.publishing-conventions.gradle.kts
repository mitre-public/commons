import net.researchgate.release.GitAdapter.GitConfig
import net.researchgate.release.ReleaseExtension

plugins {
    `maven-publish`
    id("net.researchgate.release")
}

val mavenUser: String? by project
val mavenPassword: String? by project

extensions.configure<ReleaseExtension> {
    preTagCommitMessage = "[Gradle] Bump to stable version "
    newVersionCommitMessage = "[Gradle] Bump to version "

    val git = getProperty("git") as GitConfig
    git.requireBranch = "master"
}

extensions.configure<PublishingExtension> {
    publications {
        create<MavenPublication>(project.name) {
            artifactId = project.name

            apply(plugin = "java-library")// https://docs.gradle.org/current/userguide/java_library_plugin.html
            from(components["java"])

            pom {
                name.set(project.name)
                organization {
                    name.set("The MITRE Corporation")
                    url.set("https://www.mitre.org")
                }
                scm {
                    connection.set("scm:git@mustache.mitre.org:7999/common/commons.git")
                    developerConnection.set("scm:git@mustache.mitre.org:7999/common/commons.git")
                    url.set("https://mustache.mitre.org/projects/COMMON/repos/commons/browse")
                    tag.set("HEAD")
                }
            }
        }
    }
    repositories {
        maven {
            //logger.lifecycle("using version [$version] to determine publish repo")
            if (version.toString().endsWith("SNAPSHOT")) {
                name = "mitre-caasd-snapshots"
                url = uri("https://dali.mitre.org/nexus/content/repositories/mitre-caasd-snapshots/")
            } else {
                name = "mitre-caasd-releases"
                url = uri("https://dali.mitre.org/nexus/content/repositories/mitre-caasd-releases/")
            }
            credentials {
                username = mavenUser
                password = mavenPassword
            }
        }
    }
}
