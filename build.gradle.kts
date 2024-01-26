plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(group = "org.apache.kafka", name = "kafka-clients", version = "3.6.1")
    implementation(group = "org.slf4j", name = "slf4j-api", version = "2.1.0-alpha1")
    implementation(group = "org.slf4j", name =  "slf4j-simple", version = "2.1.0-alpha1")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}