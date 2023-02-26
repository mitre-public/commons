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
        name = "Maven Central (dali proxy)"
        url = uri("https://dali.mitre.org/nexus/content/repositories/central")
    }
    maven {
        name = "mitre-caasd-releases"
        url = uri("https://dali.mitre.org/nexus/content/repositories/mitre-caasd-releases/")
    }
    maven {
        name = "dali-mitre-caasd-releases"
        url = uri("https://dali.mitre.org/nexus/content/groups/mitre-caasd")
    }
}

dependencies {
    implementation("com.google.guava:guava:23.0")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("com.github.davidmoten:rtree:0.8-RC10")
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("com.esri.geometry:esri-geometry-api:2.2.2") {
        exclude("org.codehaus.jackson:jackson-core-asl")
    }
    implementation("org.mitre.swim:swim-parse:4.0.167")

    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.5.2")
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
