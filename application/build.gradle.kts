plugins {
    id("bisq.java-library")
    id("bisq.gradle.maven_publisher.LocalMavenPublishPlugin")
}

dependencies {
    implementation(project(":i18n"))
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":settings"))
    implementation(project(":user"))
    implementation(project(":chat"))
    implementation(project(":support"))
    implementation(project(":bonded-roles"))
    implementation(project(":offer"))
    implementation(project(":trade"))

    implementation("network:network")

    implementation(libs.typesafe.config)
}
