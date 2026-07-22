plugins {
    id("java")
    id("org.springframework.boot") version "3.5.0" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}

subprojects {

    apply(plugin = "java")
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    group = "dev.project"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    dependencies {
        // Spring Boot Web (REST API)
        implementation("org.springframework.boot:spring-boot-starter-web")

        // Lombok (чтобы не писать геттеры/сеттеры)
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")

        implementation("org.mapstruct:mapstruct:1.6.0")
        annotationProcessor("org.mapstruct:mapstruct-processor:1.6.0")

        // JSON (pick one)
        implementation("com.fasterxml.jackson.core:jackson-databind:2.17.2")
        implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:2.17.2")

        // Тестирование
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation(platform("org.junit:junit-bom:5.10.0"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}