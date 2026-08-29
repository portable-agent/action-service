plugins {
    java
    jacoco
    id("org.jooq.jooq-codegen-gradle") version "3.21.7"
    id("com.diffplug.spotless") version "8.10.0"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.portableagent"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.temporal:temporal-sdk:1.37.0")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    jooqCodegen("org.jooq:jooq-meta-extensions:3.21.7")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

jooq {
    configuration {
        generator {
            database {
                name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                includes = "action_proposals|action_dispatch_outbox"
                properties {
                    property {
                        key = "scripts"
                        value = "src/main/resources/db/migration/*.sql"
                    }
                    property {
                        key = "sort"
                        value = "flyway"
                    }
                    property {
                        key = "defaultNameCase"
                        value = "lower"
                    }
                }
            }
            generate {
                deprecated = false
                records = true
            }
            target {
                packageName = "dev.portableagent.action.db"
                directory = "build/generated-src/jooq/main"
            }
        }
    }
}

sourceSets.main {
    java.srcDir("build/generated-src/jooq/main")
}

spotless {
    java {
        target("src/**/*.java")
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
    format("docs") {
        target("*.md", "docs/**/*.md", "*.yml", "*.yaml")
        trimTrailingWhitespace()
        endWithNewline()
    }
}

tasks.compileJava {
    dependsOn(tasks.jooqCodegen)
}

tasks.withType<Test> {
    useJUnitPlatform()
    finalizedBy(tasks.jacocoTestReport)
}

tasks.check {
    dependsOn(tasks.spotlessCheck)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required = true
        html.required = true
    }
}
