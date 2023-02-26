plugins {
    `kotlin-dsl`
}

repositories {
    maven {
        name = "Gradle Plugin Portal (dali proxy)"
        url = uri("https://dali.mitre.org/nexus/content/repositories/proxy-gradle-plugin-portal/")
    }
}

dependencies {
    val sonarQubePluginVersion = "3.0" // Matches sonarqube.mitre.org version
    val testLoggerPluginVersion = "3.1.0"
    val releasePluginVersion = "2.8.1"

    implementation("org.sonarsource.scanner.gradle:sonarqube-gradle-plugin:$sonarQubePluginVersion")
    implementation("com.adarshr:gradle-test-logger-plugin:$testLoggerPluginVersion")
    implementation("net.researchgate:gradle-release:$releasePluginVersion")
}
