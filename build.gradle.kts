plugins {
    `java-library`
    `maven-publish`
}

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

java {
    val javaVersion = JavaVersion.VERSION_1_8
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            //groupId does not need to be set here
            artifactId = project.name
            //version does not need to be set here

            from(components["java"])
        }
    }
}