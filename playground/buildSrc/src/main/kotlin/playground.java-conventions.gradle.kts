// Ref: https://github.com/mrclrchtr/gradle-kotlin-spring/blob/3.1.0/buildSrc/src/main/kotlin/java-conventions.gradle.kts
plugins {
    java
}

java {
    // Auto JDK setup
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

// Dependency Locking

dependencyLocking {
    lockAllConfigurations()
    lockMode = LockMode.STRICT
}

configurations {
    all {
        resolutionStrategy.activateDependencyLocking()
    }
}

// Tasks

tasks.compileJava {
    options.compilerArgs.addAll(
        listOf(
            "-Xlint:all,-processing", // Enables all recommended warnings except processing (See https://github.com/spring-projects/spring-boot/issues/6421)
            "-Werror" // Terminates compilation when warnings occur.
        )
    )
    options.encoding = "UTF-8"
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version
            )
        )
    }
}

repositories {
    mavenCentral()
    mavenLocal()
}
