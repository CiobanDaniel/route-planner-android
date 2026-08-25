import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

fun quotedBuildConfig(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val osrmBaseUrl = (localProperties.getProperty("osrm.base.url")
    ?: "https://router.project-osrm.org").trim().trimEnd('/')
val nominatimBaseUrl = (localProperties.getProperty("nominatim.base.url")
    ?: "https://nominatim.openstreetmap.org").trim().trimEnd('/')
val osrmFallbackUrl = (localProperties.getProperty("osrm.fallback.url") ?: "").trim().trimEnd('/')
val nominatimFallbackUrl = (localProperties.getProperty("nominatim.fallback.url") ?: "").trim().trimEnd('/')
val privacyPolicyUrl = (localProperties.getProperty("privacy.policy.url")
    ?: "https://github.com/CiobanDaniel/route-planner-android/blob/main/docs/PRIVACY_DRAFT.md").trim()
val supportEmail = (localProperties.getProperty("support.email") ?: "").trim()

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

val googleServicesFile = file("google-services.json")
if (googleServicesFile.exists()) {
    pluginManager.apply("com.google.gms.google-services")
    pluginManager.apply("com.google.firebase.crashlytics")
}

android {
    namespace = "com.danielcioban.routeplanner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.danielcioban.routeplanner"
        minSdk = 24
        targetSdk = 36
        versionCode = 6
        versionName = "0.5.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "OSRM_BASE_URL", quotedBuildConfig(osrmBaseUrl))
        buildConfigField("String", "NOMINATIM_BASE_URL", quotedBuildConfig(nominatimBaseUrl))
        buildConfigField("String", "OSRM_FALLBACK_URL", quotedBuildConfig(osrmFallbackUrl))
        buildConfigField("String", "NOMINATIM_FALLBACK_URL", quotedBuildConfig(nominatimFallbackUrl))
        buildConfigField("String", "PRIVACY_POLICY_URL", quotedBuildConfig(privacyPolicyUrl))
        buildConfigField("String", "SUPPORT_EMAIL", quotedBuildConfig(supportEmail))
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }

    lint {
        abortOnError = false
        warningsAsErrors = false
        checkReleaseBuilds = true
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

pluginManager.withPlugin("com.google.firebase.crashlytics") {
    android.buildTypes.named("release").configure {
        val ext = extensions.findByName("firebaseCrashlytics") ?: return@configure
        val setter = ext.javaClass.methods.firstOrNull {
            it.name == "setMappingFileUploadEnabled" && it.parameterCount == 1
        }
        if (setter != null) {
            setter.invoke(ext, true)
        } else {
            @Suppress("UNCHECKED_CAST")
            val prop = ext.javaClass.methods
                .firstOrNull { it.name == "getMappingFileUploadEnabled" && it.parameterCount == 0 }
                ?.invoke(ext) as? org.gradle.api.provider.Property<*>
            @Suppress("UNCHECKED_CAST")
            (prop as? org.gradle.api.provider.Property<Boolean>)?.set(true)
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.accompanist.permissions)
    implementation(libs.play.services.location)
    implementation(libs.play.services.code.scanner)
    implementation(libs.okhttp)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.appcompat)

    if (googleServicesFile.exists()) {
        implementation(platform(libs.firebase.bom))
        implementation(libs.firebase.crashlytics)
    }

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

configurations.configureEach {
    exclude(group = "com.google.firebase", module = "firebase-analytics")
    exclude(group = "com.google.firebase", module = "firebase-analytics-ktx")
    exclude(group = "com.google.firebase", module = "firebase-auth")
    exclude(group = "com.google.firebase", module = "firebase-auth-ktx")
}
