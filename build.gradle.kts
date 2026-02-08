import io.papermc.hangarpublishplugin.model.Platforms
import java.io.ByteArrayOutputStream

plugins {
    id("java")
    id("io.papermc.hangar-publish-plugin") version "0.1.4"
    id("com.gradleup.shadow") version "8.3.0"
}

group = "de.hallo5000"
version = "1.4.0-release"

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
    implementation("com.moandjiezana.toml:toml4j:0.7.1")
    implementation("io.netty:netty-buffer:4.2.7.Final")
    implementation("io.netty:netty-codec:4.2.7.Final")
    implementation("io.netty:netty-transport:4.2.7.Final")
    implementation("jakarta.json:jakarta.json-api:2.1.3")
    implementation("org.eclipse.parsson:jakarta.json:1.1.7")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
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

// Helper methods
fun executeGitCommand(vararg command: String): String {
    val byteOut = ByteArrayOutputStream()
    exec {
        commandLine = listOf("git", *command)
        standardOutput = byteOut
    }
    return byteOut.toString(Charsets.UTF_8.name()).trim()
}

fun latestCommitMessage(): String {
    return executeGitCommand("log", "-1", "--pretty=%B")
}

val versionString: String = version as String
val isRelease: Boolean = !versionString.contains("SNAPSHOT")

val suffixedVersion: String = if (isRelease) {
    versionString
} else {
    // Give the version a unique name by using the GitHub Actions run number
    versionString + "+" + System.getenv("GITHUB_RUN_NUMBER")
}

val shadowJarFile = layout.buildDirectory.file(
    "libs/${project.name}-${project.version}.jar"
)

afterEvaluate {
    tasks.findByName("publishPluginPublicationToHangar")?.apply {
        inputs.file(shadowJarFile)

        doFirst {
            val file = shadowJarFile.get().asFile
            if (!file.exists()) {
                throw GradleException(
                    "Publish aborted: Artifact not found.\n" +
                            "Run './gradlew build' before publishing."
                )
            }
        }
    }
}

val readme: String = project.file("README.md").readText(Charsets.UTF_8)

hangarPublish {
    publications.register("plugin") {
        version.set(versionString) //originally suffixedVersion but I include 'release' or 'SNAPSHOT' in the version
        channel.set(if (isRelease) "Release" else "Snapshot")
        changelog.set(System.getenv("CHANGELOG"))
        id.set("VelocityVersionBouncer")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        platforms {
            register(Platforms.VELOCITY) {
                // Set the JAR file to upload
                //jar.set(tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar").flatMap { it.archiveFile })
                jar.set(shadowJarFile)

                // Set platform versions from gradle.properties file
                val versions: List<String> = (property("velocityVersion") as String)
                    .split(",")
                    .map { it.trim() }
                platformVersions.set(versions)
            }
        }
        pages.resourcePage(readme)
    }
}