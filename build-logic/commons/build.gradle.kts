plugins {
    `groovy-gradle-plugin`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation(libs.protobuf.gradle.plugin)
}