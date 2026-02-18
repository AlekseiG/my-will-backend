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
            distribution {
                outputDirectory.set(file("$projectDir/build/dist/js/productionExecutable"))
            }
        }
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-core:3.1.0")
                implementation("io.ktor:ktor-client-content-negotiation:3.1.0")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.1.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
                
                @OptIn(org.jetbrains.compose.ExperimentalComposeLibrary::class)
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("io.ktor:ktor-client-okhttp:3.1.0")
            }
        }
        val jsMain by getting {
            resources.srcDir("src/jsMain/resources")
            dependencies {
                implementation("io.ktor:ktor-client-js:3.1.0")
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
