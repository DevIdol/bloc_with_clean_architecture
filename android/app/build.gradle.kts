plugins {
    id("com.android.application")
    id("kotlin-android")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

import java.util.Base64

val dartEnvironmentVariables = mutableMapOf(
    "FLAVOR" to "prod"
)

if (project.hasProperty("dart-defines")) {
    val dartDefines = project.property("dart-defines") as String
    dartDefines.split(",").forEach { entry ->
        val decoded = String(Base64.getDecoder().decode(entry), Charsets.UTF_8)
        val pair = decoded.split("=")
        if (pair.size == 2) {
            dartEnvironmentVariables[pair[0]] = pair[1]
        }
    }
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

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "com.example.clean_architecture_with_bloc"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
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
        // resValue(
        //     "string",
        //     "GOOGLE_API_KEY",
        //     dartEnvironmentVariables["GOOGLE_API_KEY"] ?: "default_value"
        // )
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
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