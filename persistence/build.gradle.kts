plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
}

dependencies {
    implementation(libs.google.guava)
    testImplementation(libs.mockito)
}
