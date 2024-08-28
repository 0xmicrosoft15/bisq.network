pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    includeBuild("build-logic")
}

plugins {
    id("bisq.gradle.toolchain_resolver.ToolchainResolverPlugin")
}

toolchainManagement {
    jvm {
        javaRepositories {
            repository("bisq_zulu") {
                resolverClass.set(bisq.gradle.toolchain_resolver.BisqToolchainResolver::class.java)
            }
        }
    }
}

rootProject.name = "bisq"

include("account")
include("common")
include("application")
include("bisq-easy")
include("bonded-roles")
include("chat")
include("contract")
include("daemon")
include("identity")
include("i18n")
include("offer")
include("persistence")
include("platform")
include("presentation")
include("trade")
include("security")
include("settings")
include("support")
include("user")

includeBuild("apps")
includeBuild("network")
includeBuild("wallets")
