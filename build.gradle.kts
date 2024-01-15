plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.4.1"
  kotlin("plugin.spring") version "1.9.22"
  kotlin("plugin.jpa") version "1.9.22"
  id("io.gitlab.arturbosch.detekt") version "1.23.1"
  id("org.jetbrains.kotlin.plugin.allopen") version "1.9.22"
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
      useVersion("1.9.0")
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
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:2.0.1")

  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.3.0")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.7.1")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")

  runtimeOnly("com.zaxxer:HikariCP")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.7.1")

  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.3")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.3")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("org.mock-server:mockserver-netty:5.15.0")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testImplementation("io.mockk:mockk:1.13.9")
  testImplementation("com.ninja-squad:springmockk:4.0.2")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(19))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "19"
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
