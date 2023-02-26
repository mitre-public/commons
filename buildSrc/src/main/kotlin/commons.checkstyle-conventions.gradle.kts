plugins {
    checkstyle
}

tasks.withType<Checkstyle> {
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

checkstyle {
    configFile = file("${rootProject.rootDir}/docs/code-style/checkstyle.xml")
    doNotIgnoreFailures()
    maxErrors = 650
    maxWarnings = 650

    doNotShowViolations()
    toolVersion = "8.42"
}

tasks.getByName("checkstyleTest").enabled = false