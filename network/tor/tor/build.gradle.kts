plugins {
    id("bisq.java-library")
    id("bisq.java-integration-tests")
    id("bisq.gradle.tor_binary.BisqTorBinaryPlugin")
}

tor {
    version.set("13.5.2")
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.file("generated/src/main/resources"))
        }
    }
}

dependencies {
    implementation(project(":tor-common"))

    implementation("bisq:security")

    implementation("network:network-common")
    implementation("network:network-identity")
    implementation("network:socks5-socket-channel")

    implementation(libs.bouncycastle)
    implementation(libs.failsafe)
    implementation(libs.tukaani)
    implementation(libs.typesafe.config)

    implementation(libs.chimp.jsocks)
    implementation(libs.chimp.jtorctl)
}