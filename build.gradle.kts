plugins {
  id("uk.gov.justice.hmpps.gradle-spring-boot") version "3.3.9"
  kotlin("plugin.spring") version "1.5.31"
}

configurations {
  testImplementation { exclude(group = "org.junit.vintage") }
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-webflux")
  implementation("org.springframework.boot:spring-boot-starter-jdbc")

  runtimeOnly("com.h2database:h2:1.4.200")
  runtimeOnly("com.zaxxer:HikariCP:3.4.5")
  runtimeOnly("org.flywaydb:flyway-core:6.5.6")
  runtimeOnly("org.postgresql:postgresql")
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(16))
}

tasks {
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
      jvmTarget = "16"
    }
  }
}
