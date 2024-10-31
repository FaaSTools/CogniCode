import org.gradle.BuildAdapter
import org.gradle.BuildResult
import java.util.concurrent.TimeUnit

plugins {
    id("java")
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

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


