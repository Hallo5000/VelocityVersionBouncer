plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.0"
}

group = "de.hallo5000"
version = "2.0.0-release"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "minebench-repo"
        url = uri("https://repo.minebench.de/")
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:4.2.1-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:4.2.1-SNAPSHOT")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("jakarta.json:jakarta.json-api:2.1.3")
    implementation("org.eclipse.parsson:jakarta.json:1.1.9")
    implementation("de.themoep.utils:lang-velocity:1.3-SNAPSHOT")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2") //maybe switch to another nullability annotation library in the future
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.jar {
    enabled = false
}

tasks.build {
    dependsOn(tasks.named("shadowJar"))
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveClassifier.set("")// remove the "-all" suffix
}
