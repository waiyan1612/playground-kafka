plugins {
    // From buildSrc
    id("playground.java-conventions")
    id("playground.spring-conventions")
    id("playground.testing-conventions")
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    implementation("org.springframework.boot:spring-boot-starter-log4j2")
    modules {
        module("org.springframework.boot:spring-boot-starter-logging") {
            replacedBy("org.springframework.boot:spring-boot-starter-log4j2", "Use Log4j2 instead of Logback")
        }
    }

    // Libraries
    implementation(libs.kafka)

    // For Docs
    implementation(libs.spring.doc)


    developmentOnly("org.springframework.boot:spring-boot-devtools")
}
