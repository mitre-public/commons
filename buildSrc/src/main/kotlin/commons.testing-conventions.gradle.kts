import com.adarshr.gradle.testlogger.theme.ThemeType

plugins {
    id("com.adarshr.test-logger")
}

testlogger {
    theme = ThemeType.STANDARD_PARALLEL
    showFullStackTraces = true
}

val integrationMaxHeap = "4096m"
val integrationJvmArgs = "-Xss256k -Dfile.encoding=UTF-8".split(" ")

tasks.named<Test>("test") {
    group = "verification"
    description = "Runs ALL tests regardless of tagged category within the project"

    failFast = true

    testlogger {
        slowThreshold = 1000 // log in red if exceeds 1 sec
        showSummary = false
    }

    useJUnitPlatform {
        includeEngines("junit-jupiter")
    }
}