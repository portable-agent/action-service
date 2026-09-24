import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    java
    jacoco
    id("org.jooq.jooq-codegen-gradle") version "3.21.8"
    id("org.openapi.generator") version "7.24.0"
    id("com.diffplug.spotless") version "8.10.1"
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "dev.portableagent"
version = "0.1.0-SNAPSHOT"

extra["tomcat.version"] = "11.0.25"

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
    implementation("io.temporal:temporal-sdk:1.39.0")
    implementation("org.springframework.boot:spring-boot-starter-flyway")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    jooqCodegen("org.jooq:jooq-meta-extensions:3.21.8")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("io.temporal:temporal-testing:1.39.0")
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
    java.srcDir(layout.buildDirectory.dir("generated-src/openapi/src/main/java"))
    java.srcDir(layout.buildDirectory.dir("generated-src/mcp-openapi/src/main/java"))
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$projectDir/src/main/openapi/action-api.yaml")
    outputDir.set(
        layout.buildDirectory
            .dir("generated-src/openapi")
            .get()
            .asFile.absolutePath,
    )
    apiPackage.set("dev.portableagent.action.api")
    modelPackage.set("dev.portableagent.action.api.model")
    configOptions.set(
        mapOf(
            "annotationLibrary" to "none",
            "documentationProvider" to "none",
            "hideGenerationTimestamp" to "true",
            "interfaceOnly" to "true",
            "openApiNullable" to "false",
            "performBeanValidation" to "true",
            "skipDefaultInterface" to "true",
            "useResponseEntity" to "true",
            "useSpringBoot4" to "true",
            "useSpringBuiltInValidation" to "true",
            "useSwaggerUI" to "false",
            "useTags" to "true",
        ),
    )
}

val mcpApiGenerate =
    tasks.register<GenerateTask>("mcpApiGenerate") {
        generatorName.set("spring")
        inputSpec.set("$projectDir/src/main/openapi/mcp-gateway-api.yaml")
        outputDir.set(
            layout.buildDirectory
                .dir("generated-src/mcp-openapi")
                .get()
                .asFile.absolutePath,
        )
        modelPackage.set("dev.portableagent.action.mcp.api.model")
        globalProperties.set(
            mapOf(
                "models" to "",
                "modelDocs" to "false",
                "modelTests" to "false",
            ),
        )
        configOptions.set(
            mapOf(
                "annotationLibrary" to "none",
                "documentationProvider" to "none",
                "hideGenerationTimestamp" to "true",
                "openApiNullable" to "false",
                "performBeanValidation" to "true",
                "useSpringBoot4" to "true",
                "useSpringBuiltInValidation" to "true",
            ),
        )
    }

spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat()
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
    dependsOn(tasks.jooqCodegen, tasks.openApiGenerate, mcpApiGenerate)
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
