plugins {
    application
    java
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
}

application {
    mainClass.set("dev.mcclient.launcher.Main")
}

/** Headless probe for Minecraft API allowlist approval -- see auth/ApprovalCheck.java. */
tasks.register<JavaExec>("checkApproval") {
    group = "verification"
    description = "Checks whether the Azure app has cleared Minecraft API review."
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("dev.mcclient.launcher.auth.ApprovalCheck")
    // PENDING is a legitimate answer, not a build failure -- callers read the status line.
    isIgnoreExitValue = true
}
