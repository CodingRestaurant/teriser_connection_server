import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.0-RC"
}

group "com.codrest"
version "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation (group= "io.netty", name="netty-all", version= "4.1.24.Final")

    testImplementation ("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    implementation("org.slf4j:slf4j-api:1.7.32")

    implementation ("com.sparkjava:spark-kotlin:1.0.0-alpha")

    compileOnly ("org.projectlombok:lombok:1.18.12")
    annotationProcessor ("org.projectlombok:lombok:1.18.12")
    testCompileOnly ("org.projectlombok:lombok:1.18.12")
    testAnnotationProcessor ("org.projectlombok:lombok:1.18.12")
    implementation(kotlin("stdlib-jdk8"))

}

tasks.withType<Test> {
    useJUnitPlatform()
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}