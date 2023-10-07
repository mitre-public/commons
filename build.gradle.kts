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

java {
    val javaVersion = JavaVersion.VERSION_1_8
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    withSourcesJar()
//    withJavadocJar()   //TODO -- FIX JAVA DOC, DON'T DISABLE!
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