import org.gradle.BuildAdapter
import org.gradle.BuildResult
import java.util.concurrent.TimeUnit

plugins {
    id("java")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.example"
version = "1.0"

repositories {
    mavenCentral()
}

application {
    mainClass.set("org.example.Main")
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    implementation("com.theokanning.openai-gpt3-java:service:0.18.2")
    implementation("software.amazon.awssdk:bedrock:2.29.10")
    implementation("software.amazon.awssdk:bedrockruntime:2.29.10")
    implementation("software.amazon.awssdk:sdk-core:2.29.10")
    implementation("software.amazon.awssdk:apache-client:2.29.10")
    implementation("software.amazon.awssdk:s3:2.29.10")
    implementation("io.github.cdimascio:java-dotenv:5.2.2")
    implementation("com.google.cloud:google-cloud-storage:2.53.1")
    implementation("org.apache.jena:apache-jena-libs:5.4.0")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

var startTime = System.currentTimeMillis()

gradle.addBuildListener(object : BuildAdapter() {
    override fun buildFinished(result: BuildResult) {
        val duration = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - startTime)
        println("\n${duration}s")
    }
})

tasks {
    // configure the ShadowJar task
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        // name the output jar exactly “ServerlessIntent-1.0.0.jar”
        archiveBaseName.set("ServerlessIntent")
        archiveClassifier.set("")       // drop the “-all” suffix
        archiveVersion.set(version.toString())

        // 1) merge all META-INF/services/** so Jena’s JSON-LD reader gets registered
        mergeServiceFiles()

        // 2) strip out all signature files so the JVM won’t reject the jar
        exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

        // 3) ensure the manifest points at your main class
        manifest {
            attributes["Main-Class"] = application.mainClass.get()
        }
    }
}