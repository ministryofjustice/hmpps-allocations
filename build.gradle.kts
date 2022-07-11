

plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "4.3.2"
  kotlin("plugin.spring") version "1.7.0"
  kotlin("plugin.jpa") version "1.7.0"
  id("io.gitlab.arturbosch.detekt") version "1.20.0"
  id("org.jetbrains.kotlin.plugin.allopen")version "1.7.0"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

allOpen {
  annotations("javax.persistence.Entity")
}

val springDocVersion by extra("1.6.7")

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:1.1.6")

  implementation("org.springdoc:springdoc-openapi-ui:$springDocVersion")
  implementation("org.springdoc:springdoc-openapi-kotlin:$springDocVersion")
  implementation("org.springdoc:springdoc-openapi-data-rest:$springDocVersion")

  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("com.opencsv:opencsv:5.6")
  runtimeOnly("com.zaxxer:HikariCP")
  runtimeOnly("org.flywaydb:flyway-core")
  runtimeOnly("org.postgresql:postgresql:42.4.0")

  testImplementation("io.jsonwebtoken:jjwt:0.9.1")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.0")
  testImplementation("org.mock-server:mockserver-netty:5.13.2")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "17"
    }
  }
  getByName("check") {
    dependsOn(":ktlintCheck", "detekt")
  }
}

tasks.named<JavaExec>("bootRun") {
  systemProperty("spring.profiles.active", "dev,docker")
}
repositories {
  mavenCentral()
}

detekt {
  config = files("src/test/resources/detekt-config.yml")
  buildUponDefaultConfig = true
  ignoreFailures = true
}
