plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "5.0.1"
  kotlin("plugin.spring") version "1.8.10"
  kotlin("plugin.jpa") version "1.8.10"
  id("io.gitlab.arturbosch.detekt") version "1.22.0"
  id("org.jetbrains.kotlin.plugin.allopen") version "1.8.10"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencyCheck {
  suppressionFiles.add("suppressions.xml")
}

allOpen {
  annotations("jakarta.persistence.Entity")
}

repositories {
  maven { url = uri("https://repo.spring.io/milestone") }
  mavenCentral()
}
dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:2.0.0-beta-10")

  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.2")

  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  runtimeOnly("com.zaxxer:HikariCP")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.5.3")

  testImplementation("io.jsonwebtoken:jjwt-impl:0.11.5")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("org.mock-server:mockserver-netty:5.15.0")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testImplementation("io.mockk:mockk:1.13.4")
  testImplementation("com.ninja-squad:springmockk:4.0.0")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(18))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "18"
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
  config = files("src/test/resources/detekt-config.yml")
  buildUponDefaultConfig = true
}
