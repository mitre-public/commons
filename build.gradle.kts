plugins {
    `java-library`
    `maven-publish`
    id("com.diffplug.spotless") version "6.23.3"
}

group = "org.mitre"
version = "0.0.59"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    implementation("com.google.guava:guava:32.1.2-jre")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.google.code.gson:gson:2.8.9")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("org.apache.avro:avro:1.11.0")
}

//  This idiom completely mutes the "javadoc" task from java-library.
//  Javadoc is still produced, but you won't get warnings OR build failures due to javadoc
//  I decided to turn warning off because the amount of javadoc required for builder was too much.
tasks {
    javadoc {
        options {
            (this as CoreJavadocOptions).addStringOption("Xdoclint:none", "-quiet")
        }
    }
}


tasks.named<Test>("test") {
    useJUnitPlatform()

    testLogging {
        events("PASSED", "SKIPPED", "FAILED")
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
    withSourcesJar()
    withJavadocJar()
}

// Enforces a consistent code style with the "spotless" plugin
//
// See https://github.com/diffplug/spotless
// And https://github.com/diffplug/spotless/tree/main/plugin-gradle
spotless {
    java {

        // This formatter is "Good" but not "Perfect"
        // But, using this formatter allows multiple contributors to work in a "common style"
        palantirJavaFormat()

        // import order: static, JDK, MITRE, 3rd Party
        importOrder("\\#", "java|javax", "org.mitre", "")
        removeUnusedImports()
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "commons"
            from(components["java"])
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set(project.name)
                description.set("MITRE Commons Library for Aviation")
                url.set("https://github.com/mitre-public/commons")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("jparker")
                        name.set("Jon Parker")
                        email.set("jiparker@mitre.org")
                    }
                    developer {
                        id.set("dbaker")
                        name.set("David Baker")
                        email.set("dbaker@mitre.org")
                    }
                }

                //REQUIRED! To publish to maven central (from experience the leading "scm:git" is required too)
                scm {
                    connection.set("scm:git:https://github.com/mitre-public/commons.git")
                    developerConnection.set("scm:git:ssh://git@github.com:mitre-public/commons.git")
                    url.set("https://github.com/mitre-public/commons")
                }
            }
        }
    }
}