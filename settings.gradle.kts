pluginManagement {
    includeBuild("rn/node_modules/@react-native/gradle-plugin")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("com.facebook.react.settings")
}

// Detect npx path for nvm/fnm users where Gradle daemon can't find it on PATH
val npxPath: String = providers.exec {
    commandLine("bash", "-lc", "which npx")
}.standardOutput.asText.get().trim()

extensions.configure<com.facebook.react.ReactSettingsExtension> {
    autolinkLibrariesFromCommand(
        command = listOf(npxPath, "@react-native-community/cli", "config"),
        workingDirectory = file("rn"),
        lockFiles = files("rn/package-lock.json")
    )
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "RNDemo"
include(":app")
include(":rnlib")
