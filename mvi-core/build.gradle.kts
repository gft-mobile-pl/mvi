import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = Java.jvmTarget
            }
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(libs.coroutines.core)
            implementation(libs.viewmodel.compose)
            implementation(libs.viewmodel.savedstate.compose)
            implementation(libs.concurrency)
            implementation(libs.coroutines.gftx)
        }
    }
}

compose {
    kotlinCompilerPlugin.set(Compose.kotlinCompilerPlugin)
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
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}
