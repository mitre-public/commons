import java.time.LocalDate
import java.time.format.DateTimeFormatter

tasks.named<Javadoc>("javadoc") {
    (options as StandardJavadocDocletOptions).addBooleanOption("Xdoclint:none", true)
}

val javadocDir = "$buildDir/docs/java-doc"
val javadocZipDir = "$buildDir/dist"
val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

val javadocZipName: String
    get() = "${dateTimeFormatter.format(LocalDate.now())}-ditto-$version.zip"

val javadocBuild by tasks.registering(Javadoc::class) {
    description = "Generate uber Javadocs containing sources from all modules"
    title = "${project.name} $version API"
    group = "javadoc"
    setDestinationDir(file(javadocDir))

    val customOptions = options as StandardJavadocDocletOptions
    customOptions.addBooleanOption("Xdoclint:none", true)
    customOptions.addBooleanOption("quiet", true)
    customOptions.addBooleanOption("linksource", true)
    customOptions.addStringOption("sourcepath", "")//hacky workaround for bug: https://github.com/gradle/gradle/issues/5630

    val javadocTask = tasks.getByName<Javadoc>("javadoc")
    source += javadocTask.source
    classpath += javadocTask.classpath
}

val javadocPackage by tasks.registering(Zip::class) {
    dependsOn(javadocBuild)
    description = "Zip the javadocs into a single file"
    group = "javadoc"

    from(javadocDir)
    destinationDirectory.set(file(javadocZipDir))
    archiveFileName.set(javadocZipName)
}
