plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("com.diffplug.spotless") version "8.10.2"
}

group = "com.virginonline"
version = "0.0.1-SNAPSHOT"

val okHttpVersion: String by project

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")

    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-quartz-test")
    implementation("com.squareup.okhttp3:okhttp:$okHttpVersion")

    testImplementation("com.squareup.okhttp3:mockwebserver3:$okHttpVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
