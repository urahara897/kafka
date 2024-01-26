plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(group= "org.apache.kafka", name= "kafka-clients", version= "3.6.1")

    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation(group= "org.slf4j", name= "slf4j-api", version= "2.1.0-alpha1")

    // https://mvnrepository.com/artifact/org.slf4j/slf4j-simple
    implementation(group= "org.slf4j", name= "slf4j-simple", version= "2.1.0-alpha1")

    //https://central.sonatype.com/artifact/org.opensearch.client/opensearch-rest-high-level-client
    implementation("org.opensearch.client:opensearch-rest-high-level-client:2.11.1")

    // https://mvnrepository.com/artifact/com.google.code.gson/gson
    implementation("com.google.code.gson:gson:2.9.0")

    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}