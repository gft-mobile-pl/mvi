import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

kotlin {
    jvm {
        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(Java.jvmTarget)
        }
    }

    android {
        namespace = "com.gft.mvi"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.fromTarget(Java.jvmTarget)
        }
    }

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(libs.jetbrains.lifecycle.viewmodel)
            api(libs.jetbrains.lifecycle.viewmodel.savedstate)
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinx.serialization.core)
            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.gft.coroutines)
            implementation(libs.gft.concurrency)
        }
        iosMain.dependencies {
            implementation(libs.jetbrains.compose.ui)
        }
    }
}

mavenPublishing {
    coordinates(project.property("libraryGroupId") as String, "mvi-core", project.property("libraryVersion") as String)

    pom {
        name.set(project.property("libraryNamePrefix") as String + " Core")
        description.set(project.property("libraryDescription") as String)
        inceptionYear.set(project.property("libraryInceptionYear") as String)
        url.set("https://${project.property("libraryRepositoryUrl") as String}")
        licenses {
            license {
                name.set(project.property("libraryLicenseName") as String)
                url.set(project.property("libraryLicenseUrl") as String)
                distribution.set(project.property("libraryLicenseDistribution") as String)
            }
        }
        developers {
            developer {
                name.set(project.property("libraryDeveloperName") as String)
            }
        }
        scm {
            url.set("https://${project.property("libraryRepositoryUrl") as String}")
            connection.set("scm:git:git://${project.property("libraryRepositoryUrl") as String}")
            developerConnection.set("scm:git:ssh://git@${project.property("libraryRepositoryUrl") as String}.git")
        }
    }
    publishToMavenCentral()
    signAllPublications()
}
