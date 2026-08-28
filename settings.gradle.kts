pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") { name = "Fabric" }
        maven("https://maven.legacyfabric.net/") { name = "legacy-fabric" }
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "mc-client-1.8.9"

include("launcher")
include("mods:example-mod")
include("mods:mcclient-mods")
