plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
}
kotlin { jvmToolchain(21) }
sourceSets {
    main {
        kotlin.srcDir("../app/src/main/java")
        kotlin.include("com/zerostar/game/game/**", "com/zerostar/game/data/**", "com/zerostar/game/ads/AdsGate.kt")
    }
    test { kotlin.srcDir("../app/src/test/java"); kotlin.exclude("**/GameRepositoryTest.kt", "**/LifecycleSaveTest.kt") }
}
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("androidx.datastore:datastore-core:1.1.7")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
tasks.test { testLogging { events("passed", "failed", "skipped"); showStandardStreams = true } }


