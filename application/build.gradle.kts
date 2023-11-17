plugins {
    id("bisq.java-library")
}

dependencies {
    implementation(project(":i18n"))
    implementation(project(":persistence"))
    implementation(project(":security"))
    implementation(project(":settings"))
    implementation(project(":user"))
    implementation(project(":chat"))
    implementation(project(":support"))
    implementation(project(":bonded_roles"))
    implementation(project(":offer"))
    implementation(project(":trade"))

    implementation("network:network")

    implementation(libs.google.guava)
    implementation(libs.typesafe.config)
    implementation(libs.bouncycastle.pg)
}
