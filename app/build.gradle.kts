import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "com.ksound.atmos"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.ksound.atmos"
    minSdk = 28
    targetSdk = 36
    versionCode = 5
    versionName = "1.0.4"
  }

  buildTypes {
    release {
      signingConfig = signingConfigs.getByName("debug")
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
  }

  lint {
    checkReleaseBuilds = false
  }
}

base {
  archivesName.set("KSound-v${android.defaultConfig.versionName}")
}

kotlin {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
  }
}

composeCompiler {
  includeSourceInformation = false
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2026.02.01"))
  implementation("androidx.activity:activity-compose:1.12.4")
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.material3:material3:1.5.0-alpha15")
  implementation("androidx.compose.material:material-icons-extended:1.7.8")
  implementation("androidx.core:core-ktx:1.15.0")
}
