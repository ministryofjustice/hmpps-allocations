import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {

  id("uk.gov.justice.hmpps.gradle-spring-boot") version "7.1.2"
  kotlin("plugin.spring") version "2.1.0"
  kotlin("plugin.jpa") version "2.1.0"
  id("io.gitlab.arturbosch.detekt") version "1.23.7"
  id("org.jetbrains.kotlin.plugin.allopen") version "2.1.0"
}

configurations {
  implementation { exclude(module = "spring-boot-starter-web") }
  implementation { exclude(module = "spring-boot-starter-tomcat") }
  implementation { exclude(module = "applicationinsights-spring-boot-starter") }
  implementation { exclude(module = "applicationinsights-logging-logback") }
  testImplementation { exclude(group = "org.junit.vintage") }
}

// fix to prevent the mismatch of kotlin versions for detekt
configurations.matching { it.name == "detekt" }.all {
  resolutionStrategy.eachDependency {
    if (requested.group == "org.jetbrains.kotlin") {
      useVersion("2.0.10")
    }
  }
}

dependencyCheck {
  suppressionFiles.add("suppressions.xml")
}

allOpen {
  annotations("jakarta.persistence.Entity")
}

dependencies {
  implementation("com.googlecode.owasp-java-html-sanitizer:owasp-java-html-sanitizer:20240325.1")

  implementation("org.springframework.boot:spring-boot-starter-webflux:3.4.1")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa:3.4.1")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.2.2")

  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.8.3")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.10.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.10.1")

  runtimeOnly("com.zaxxer:HikariCP")
  runtimeOnly("org.flywaydb:flyway-core")
  implementation("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.5")

  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.2")
  testImplementation("org.mock-server:mockserver-netty:5.15.0")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testImplementation("io.mockk:mockk:1.13.16")
  testImplementation("com.ninja-squad:springmockk:4.0.2")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_21)
    }
    compileKotlin {
      compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
      }
    }
    compileTestKotlin {
      compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
      }
    }
  }
  getByName("check") {
    dependsOn(":ktlintCheck", "detekt")
  }
}

tasks.named<JavaExec>("bootRun") {
  systemProperty("spring.profiles.active", "dev,docker")
}

detekt {
  config.setFrom("src/test/resources/detekt-config.yml")
  buildUponDefaultConfig = true
}
// pin version of ktlint as HMPPS gradle orb 6.1.2 brings in dependency with older, breaking version. Remove when updated.
ktlint {
  version.set("1.4.1")
}
