// Ref: https://github.com/mrclrchtr/gradle-kotlin-spring/blob/3.1.0/buildSrc/src/main/kotlin/spring-conventions.gradle.kts
plugins {
    id("org.springframework.boot")
}

logger.lifecycle("Enabling Spring Boot plugin in module ${project.path}")
apply(plugin = "org.springframework.boot")

logger.lifecycle("Enabling Spring Boot Dependency Management in module ${project.path}")
apply(plugin = "io.spring.dependency-management")

springBoot {
    // Creates META-INF/build-info.properties for Spring Boot Actuator
    buildInfo()
}
