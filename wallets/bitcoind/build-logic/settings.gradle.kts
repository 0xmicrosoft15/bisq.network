dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../../../gradle/libs.versions.toml"))
        }
    }
}

include("gradle-tasks")

rootProject.name = "bitcoind-build-logic"