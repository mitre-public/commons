plugins {
    `java-library`

    id("commons.testing-conventions")
    id("commons.checkstyle-conventions")
    id("commons.reporting-conventions")
    id("commons.javadoc-conventions")
    id("commons.publishing-conventions")
}

repositories {
    maven {
        name = "Maven Central"
        url = uri("https://repo.maven.apache.org/maven2")
    }
}

dependencies {
    implementation("com.google.guava:guava:32.1.2-jre")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.google.code.gson:gson:2.8.9")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("org.apache.avro:avro:1.11.0")
}

java {
    val javaVersion = JavaVersion.VERSION_1_8
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    withSourcesJar()
    withJavadocJar()
}
