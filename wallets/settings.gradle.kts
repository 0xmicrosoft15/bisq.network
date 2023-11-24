pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("../build-logic")
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

includeBuild("..")
include("core")

include("bitcoind")
include("electrum")
include("elementsd")

include("json-rpc")
include("process")
include("regtest")

rootProject.name = "wallets"