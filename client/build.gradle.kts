plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

kotlin {
    jvmToolchain(25)
    jvm {
        mainRun {
            mainClass.set("org.mywill.client.MainKt")
        }
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
    }
    @OptIn(org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl::class)
    js(IR) {
        compilerOptions.moduleName.set("client")
        browser {
            commonWebpackConfig {
                cssSupport {
                    enabled = true
                }
            }
            runTask {
                devServerProperty.map {
                    it.port = 8081
                }
            }
            distribution {
                outputDirectory.set(file("$projectDir/build/dist/js/productionExecutable"))
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation("io.ktor:ktor-client-core:3.4.0")
                implementation("io.ktor:ktor-client-content-negotiation:3.4.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

                implementation("org.jetbrains.compose.runtime:runtime:1.10.1")
                implementation("org.jetbrains.compose.foundation:foundation:1.10.1")
                implementation("org.jetbrains.compose.ui:ui:1.10.1")
                implementation("org.jetbrains.compose.material3:material3:1.9.0")
                implementation("org.jetbrains.compose.components:components-resources:1.10.1")
                implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.10.1")
            }
        }
        commonTest {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        jvmMain {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:3.4.0")
            }
        }
        jsMain {
            resources.srcDir("src/jsMain/resources")
            dependencies {
                implementation("io.ktor:ktor-client-js:3.4.0")
            }
        }
    }

    tasks.named<ProcessResources>("jsProcessResources") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }

    tasks.named("jsBrowserProductionWebpack") {
        doLast {
            copy {
                from("src/jsMain/resources/index.html")
                into("$projectDir/build/dist/js/productionExecutable")
            }
        }
    }
}
