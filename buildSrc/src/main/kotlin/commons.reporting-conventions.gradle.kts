import org.sonarqube.gradle.SonarQubeExtension

plugins {
    jacoco
    id("org.sonarqube")
}

jacoco {
    toolVersion = "0.8.5"
}

val javaPlugin = extensions.getByType<JavaPluginExtension>()
val mainSourceSet = javaPlugin.sourceSets["main"]!!
val testSourceSet = javaPlugin.sourceSets["test"]!!

tasks.register<JacocoReport>("codeCoverageReport") {
    dependsOn(tasks.named("test"))
    group = "verification"

    sourceSets(mainSourceSet)

    if (testSourceSet.allJava.isNotEmpty) {
        tasks.matching { it.hasExtensionOfType<JacocoTaskExtension>() }.forEach { jacocoTask ->
            val execFileName = "$buildDir/jacoco/${jacocoTask.name}.exec"

            if (file(execFileName).exists()) {
                logger.lifecycle("Including ${project.name}: ${jacocoTask.name} in full report")
                executionData(jacocoTask)
            }
        }
    }

    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

val jacocoAggregateXmlReportFile = "$buildDir/reports/jacoco/codeCoverageReport/codeCoverageReport.xml"
configure<SonarQubeExtension> {
    properties {
        properties(
            mapOf(
                "sonar.coverage.jacoco.xmlReportPaths" to jacocoAggregateXmlReportFile,
                "sonar.projectKey" to "caasd-commons",
                "sonar.host.url" to "https://sonarqube.mitre.org",
                "sonar.login" to "3680dc9d4e87806a6bc11fb76a9fe8941c050f2a"
            )
        )
    }
}
