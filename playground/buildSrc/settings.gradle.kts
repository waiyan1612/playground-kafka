rootProject.name = "playground"

// This is required because buildSrc is executed before the default libs catalog is generated.
dependencyResolutionManagement {
    versionCatalogs {
        create("buildSrcLibs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
