plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    implementation(project(":i18n"))
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":identity"))

    implementation("network:network")
}
