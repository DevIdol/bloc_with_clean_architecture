plugins {
    id("com.android.application")
    id("kotlin-android")
    id("dev.flutter.flutter-gradle-plugin")
}

import java.util.Properties
import java.io.FileInputStream
import java.util.Base64

val dartEnvironmentVariables: Map<String, String> = run {
    val map = mutableMapOf<String, String>()
    // Default flavor to "prod" if not specified
    map["FLAVOR"] = "prod"
    val dartDefines = project.properties["dart-defines"]?.toString()?.split(",")
    dartDefines?.forEach { define ->
        try {
            val keyValue = String(Base64.getDecoder().decode(define)).split("=")
            if (keyValue.size == 2) {
                map[keyValue[0]] = keyValue[1]
            }
        } catch (e: Exception) {
            println("Invalid dart-define: $define")
        }
    }
    map.toMap()
}

val keystoreProperties: Properties().apply {
    val keystorePropertiesFile = rootProject.file("key.properties")
    if (keystorePropertiesFile.exists()) {
        load(FileInputStream(keystorePropertiesFile))
    } else {
        println("Warning: key.properties not found, signing may fail")
    }
}

android {
    namespace = "com.mtm.mtmknowsync"
    compileSdk = flutter.compileSdkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    flavorDimensions.add("default")
    productFlavors {
        create("prod") {
            dimension = "default"
            applicationIdSuffix = dartEnvironmentVariables["APP_SUFFIX"] ?: ".prod"
        }
    }

    defaultConfig {
        applicationId = "com.example.clean_architecture_with_bloc"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName

        // Apply applicationIdSuffix from dartEnvironmentVariables
        val appSuffix = dartEnvironmentVariables["APP_SUFFIX"] ?: ""
        if (appSuffix.isNotEmpty()) {
            applicationIdSuffix = appSuffix
        }
        resValue(
            "string",
            "app_name",
            dartEnvironmentVariables["APP_NAME"] ?: "MTMKnowSync"
        )
    }

    signingConfigs {
        create("release") {
            keyAlias = keystoreProperties["keyAlias"] as String? ?: throw GradleException("keyAlias not set in key.properties")
            keyPassword = keystoreProperties["keyPassword"] as String? ?: throw GradleException("keyPassword not set in key.properties")
            storeFile = keystoreProperties["storeFile"]?.let { file(it) } ?: throw GradleException("storeFile not set in key.properties")
            storePassword = keystoreProperties["storePassword"] as String? ?: throw GradleException("storePassword not set in key.properties")
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            // Disable minification to avoid R8 issues
            // isMinifyEnabled = false
            // shrinkResources = false
            // proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

flutter {
    source = "../.."
}

// Task to copy APK to Flutter-expected path
val copyProdReleaseApk by tasks.registering(Copy::class) {
    from("build/app/outputs/apk/prod/release/app-prod-release.apk")
    into("build/app/outputs/flutter-apk")
    rename { "app-prod-release.apk" }
    onlyIf { file("build/app/outputs/apk/prod/release/app-prod-release.apk").exists() }
}

tasks.named("assembleProdRelease") {
    finalizedBy(copyProdReleaseApk)
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
}


// Custom task for copying flavor-specific resources
// val copySources by tasks.registering(Copy::class) {
//     from("src/${dartEnvironmentVariables["FLAVOR"]}/res")
//     into("src/main/res")
// }

// tasks.whenTaskAdded {
//     dependsOn(copySources)
//     if (name == "generateDebugResources" || name == "generateReleaseResources") {
//         dependsOn(copySources)
//     }
// }

// Copy flavor-specific google-services.json
// val selectGoogleServicesJson by tasks.registering(Copy::class) {
//     from("src/${dartEnvironmentVariables["FLAVOR"]}/google-services.json")
//     into("./")
// }