import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.tasks.bundling.Zip
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.lsplugin.resopt)
}

val appVersionCode = 1
val appVersionName = "1.0"
val axManagerModuleId = "org.lsposed.corepatchn"
val axManagerModuleName = "Core Patch N"
val axManagerModuleAuthor = "Core Patch N"
val axManagerModuleDescription = "Core Patch N packaged for AxManager"

configure<ApplicationExtension> {
    namespace = "org.lsposed.corepatch"
    compileSdk = 37

    val releaseSigningPropertyNames = listOf(
        "releaseStoreFile",
        "releaseStorePassword",
        "releaseKeyAlias",
        "releaseKeyPassword",
    )
    val hasReleaseSigningProperties =
        releaseSigningPropertyNames.all { providers.gradleProperty(it).isPresent }

    defaultConfig {
        applicationId = "org.lsposed.corepatch"
        minSdk = 28
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigningProperties) {
                storeFile = rootProject.file(providers.gradleProperty("releaseStoreFile").get())
                storePassword = providers.gradleProperty("releaseStorePassword").get()
                keyAlias = providers.gradleProperty("releaseKeyAlias").get()
                keyPassword = providers.gradleProperty("releaseKeyPassword").get()
            }
        }
    }

    buildTypes {
        release {
            @Suppress("UnstableApiUsage")
            vcsInfo.include = false
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs["release"].takeIf { hasReleaseSigningProperties }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            merges += "META-INF/xposed/*"
            excludes += "**"
        }
        dex {
            useLegacyPackaging = true
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

val deleteAppMetadata = tasks.register("deleteAppMetadata") {
    val appMetadataFile =
        file("build/intermediates/app_metadata/release/writeReleaseAppMetadata/app-metadata.properties")
    doLast {
        appMetadataFile.writeText(
            ""
        )
    }
}

afterEvaluate {
    tasks.named("writeReleaseAppMetadata") {
        finalizedBy(deleteAppMetadata)
    }
}

dependencies {
    compileOnly(libs.libxposed.api)
    implementation(libs.libxposed.service)
}

val prepareAxManagerModule = tasks.register("prepareAxManagerModule") {
    dependsOn("assembleRelease")
    val moduleDir = layout.buildDirectory.dir("intermediates/axmanager-module")
    val releaseApk = layout.buildDirectory.file("outputs/apk/release/app-release.apk")
    inputs.file(releaseApk)
    outputs.dir(moduleDir)

    doLast {
        val moduleDirFile = moduleDir.get().asFile
        delete(moduleDirFile)

        val systemAppDir = File(moduleDirFile, "system/priv-app/CorePatchN")
        systemAppDir.mkdirs()

        val apkFile = releaseApk.get().asFile
        require(apkFile.exists()) { "Release APK not found at ${apkFile.absolutePath}" }
        apkFile.copyTo(File(systemAppDir, "CorePatchN.apk"), overwrite = true)

        File(moduleDirFile, "module.prop").writeText(
            """
            id=$axManagerModuleId
            name=$axManagerModuleName
            version=$appVersionName
            versionCode=$appVersionCode
            author=$axManagerModuleAuthor
            description=$axManagerModuleDescription
            """.trimIndent() + "\n"
        )
    }
}

tasks.register<Zip>("packageAxManagerModule") {
    dependsOn(prepareAxManagerModule)
    from(layout.buildDirectory.dir("intermediates/axmanager-module"))
    destinationDirectory.set(layout.buildDirectory.dir("outputs/axmanager"))
    archiveFileName.set("corepatchn-axmanager-v$appVersionName.zip")
}
