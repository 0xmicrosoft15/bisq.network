plugins {
    id("bisq.java-library")
    id("bisq.protobuf")
    id("bisq.gradle.electrum.BisqElectrumPlugin")
    id("bisq.java-integration-tests")
}

electrum {
    version.set("4.2.2")
}

sourceSets {
    main {
        resources {
            srcDir(layout.buildDirectory.file("generated/src/main/resources"))
        }
    }
}

dependencies {
    api(project(":core"))

    implementation("bisq:persistence")
    implementation(project(":json-rpc"))
    implementation(project(":process"))
    
    implementation(libs.typesafe.config)
    implementation(libs.bundles.glassfish.jersey)

    integrationTestImplementation(project(":bitcoind"))
    integrationTestImplementation(project(":regtest"))
}