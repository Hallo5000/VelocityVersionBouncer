plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.0"
}

group = "de.hallo5000"
version = "1.6.1-SNAPSHOT"

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
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    implementation("com.moandjiezana.toml:toml4j:0.7.1") // moved to https://mvnrepository.com/artifact/io.hotmoka/toml4j since 0.7.3 but GitHub repo shows 0.1.7 as latest
    implementation("io.netty:netty-buffer:4.2.7.Final")
    implementation("io.netty:netty-codec:4.2.7.Final")
    implementation("io.netty:netty-transport:4.2.7.Final")
    implementation("jakarta.json:jakarta.json-api:2.1.3")
    implementation("org.eclipse.parsson:jakarta.json:1.1.7")
    implementation("de.themoep.utils:lang-velocity:1.3-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
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
