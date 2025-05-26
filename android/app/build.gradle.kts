plugins {
    id("com.android.application")
    id("kotlin-android")
    id("dev.flutter.flutter-gradle-plugin")
}

import java.util.Properties
import java.io.FileInputStream
import java.util.Base64

val dartEnvironmentVariables: Map<String, String> = run {
    val dartDefines = project.properties["dart-defines"]?.toString()?.split(",")
    val map = mutableMapOf<String, String>()
    dartDefines?.forEach { define ->
        val keyValue = String(Base64.getDecoder().decode(define)).split("=")
        if (keyValue.size == 2) {
            map[keyValue[0]] = keyValue[1]
        }
    }
    map.toMap()
}

android {
    namespace = "com.example.clean_architecture_with_bloc"
    compileSdk = flutter.compileSdkVersion
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    flavorDimensions.add("default")
    productFlavors {
        create("prod") {
            dimension = "default"
            applicationIdSuffix = ".prod"
        }
    }

    defaultConfig {
        applicationId = "com.example.clean_architecture_with_bloc"
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName

        if (dartEnvironmentVariables["FLAVOR"] != "prod") {
            applicationIdSuffix = ".${dartEnvironmentVariables["FLAVOR"]}"
        }
        resValue(
            "string",
            "app_name",
            "My App" + if (dartEnvironmentVariables["FLAVOR"] == "prod") "" else ".${dartEnvironmentVariables["FLAVOR"]}"
        )
    }

    signingConfigs {
        create("release") {
            keyAlias = System.getenv("KEY_ALIAS") ?: throw GradleException("KEY_ALIAS is not set")
            keyPassword = System.getenv("KEY_PASSWORD") ?: throw GradleException("KEY_PASSWORD is not set")
            storeFile = file("upload-keystore.jks")
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: throw GradleException("KEYSTORE_PASSWORD is not set")
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

flutter {
    source = "../.."
}

// Custom task for copying flavor-specific resources
val copySources by tasks.registering(Copy::class) {
    from("src/${dartEnvironmentVariables["FLAVOR"]}/res")
    into("src/main/res")
}

tasks.whenTaskAdded {
    dependsOn(copySources)
    if (name == "generateDebugResources" || name == "generateReleaseResources") {
        dependsOn(copySources)
    }
}

// Copy flavor-specific google-services.json
val selectGoogleServicesJson by tasks.registering(Copy::class) {
    from("src/${dartEnvironmentVariables["FLAVOR"]}/google-services.json")
    into("./")
}