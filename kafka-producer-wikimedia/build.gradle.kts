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

    // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
    implementation(group= "com.squareup.okhttp3", name= "okhttp", version= "5.0.0-alpha.12")

    // https://mvnrepository.com/artifact/com.launchdarkly/okhttp-eventsource
    implementation(group= "com.launchdarkly", name= "okhttp-eventsource", version= "4.1.1")
}

tasks.test {
    useJUnitPlatform()
}