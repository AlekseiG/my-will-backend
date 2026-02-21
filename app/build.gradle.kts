plugins {
    kotlin("plugin.spring")
    kotlin("plugin.jpa")
    kotlin("jvm")
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    application
    jacoco
}

extra["springCloudVersion"] = "2025.0.0"

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.liquibase:liquibase-core:5.0.1")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.5")
    implementation("io.github.oshai:kotlin-logging-jvm:8.0.01")
    implementation("net.javacrumbs.shedlock:shedlock-spring:7.6.0")
    implementation("net.javacrumbs.shedlock:shedlock-provider-jdbc-template:7.6.0")
    implementation("org.bouncycastle:bcprov-jdk18on:1.83")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

configure<JacocoPluginExtension> {
    toolVersion = "0.8.14"
}

val jacocoExclusions = listOf(
    "**/dto/**",
    "**/entity/**",
    "**/config/**",
    "**/*Application*",
    "**/AppKt*",
    "**/*Exception*",
    "**/repository/**",
    "**/converter/**",
    "**/config/**",
    "**/security/**"
)

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy("jacocoTestReport")
}

val jacocoTestReport by tasks.getting(JacocoReport::class) {
    dependsOn(tasks.test)
    classDirectories.setFrom(
        files("build/classes/kotlin/main").asFileTree.matching {
            include("org/mywill/**")
            exclude(jacocoExclusions)
        }
    )
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    doLast {
        println("\nJacoco HTML report: ${reports.html.outputLocation.get()}/index.html")
    }
}

tasks.jacocoTestCoverageVerification {
    dependsOn(jacocoTestReport)
    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = 0.0.toBigDecimal() // Порог 0% на старте — можно поднять позже
            }
        }
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

application {
    mainClass = "org.mywill.server.AppKt"
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25)
        freeCompilerArgs.addAll("-Xjsr305=strict", "-Xannotation-default-target=param-property")
    }
}