plugins {
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/")
}

dependencies {
    compileOnly("net.fabricmc:fabric-loader:0.19.3")
}

tasks.jar {
    archiveBaseName.set("example-mod")
    archiveVersion.set("0.1.0")
}
