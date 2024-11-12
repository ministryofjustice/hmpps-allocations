plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "6.0.8"
  kotlin("plugin.spring") version "2.0.21"
  kotlin("plugin.jpa") version "2.0.21"
  id("io.gitlab.arturbosch.detekt") version "1.23.7"
  id("org.jetbrains.kotlin.plugin.allopen") version "2.0.21"
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

  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("uk.gov.justice.service.hmpps:hmpps-sqs-spring-boot-starter:5.1.1")

  implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.6.0")

  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:1.9.0")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.9.0")

  runtimeOnly("com.zaxxer:HikariCP")
  runtimeOnly("org.flywaydb:flyway-core")
  implementation("org.flywaydb:flyway-database-postgresql")
  runtimeOnly("org.postgresql:postgresql:42.7.4")

  testImplementation("io.jsonwebtoken:jjwt-impl:0.12.6")
  testImplementation("io.jsonwebtoken:jjwt-jackson:0.12.6")
  testImplementation("org.awaitility:awaitility-kotlin:4.2.2")
  testImplementation("org.mock-server:mockserver-netty:5.15.0")
  testImplementation("org.junit.jupiter:junit-jupiter-params")
  testImplementation("io.mockk:mockk:1.13.13")
  testImplementation("com.ninja-squad:springmockk:4.0.2")
  testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "21"
    }
    compileKotlin {
      kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
      }
    }
    compileTestKotlin {
      kotlinOptions {
        jvmTarget = JavaVersion.VERSION_21.toString()
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
