import org.gradle.api.JavaVersion

/**
 * JVM version used across the whole project.
 *
 * JVM 17 is required: com.gft.mobile:concurrency (inline withLock) is compiled for JVM 17.
 */
object Java {
    val sourceCompatibility = JavaVersion.VERSION_17
    val targetCompatibility = JavaVersion.VERSION_17
    val jvmTarget = JavaVersion.VERSION_17.toString()
}
