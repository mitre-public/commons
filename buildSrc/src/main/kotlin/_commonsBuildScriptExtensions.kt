import org.gradle.api.Task
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.kotlin.dsl.findByType

internal inline fun <reified T : Any> Task.hasExtensionOfType(): Boolean = extensions.findByType<T>() !== null

internal inline val FileCollection.isNotEmpty: Boolean get() = !isEmpty

internal fun CheckstyleExtension.doNotIgnoreFailures() {
    isIgnoreFailures = false
}

internal fun CheckstyleExtension.doNotShowViolations() {
    isShowViolations = false
}
