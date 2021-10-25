import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.5.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.6.0-RC"
    kotlin("plugin.spring") version "1.6.0-RC"
}

group "com.codrest"
version "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {

    implementation (group= "org.springframework.boot", name= "spring-boot-starter", version= "2.5.3")
    implementation (group= "io.netty", name="netty-all", version= "4.1.24.Final")

    testImplementation ("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.7.0")

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
springBoot {
    mainClass.set("com.codrest.teriser.connectionserver.TeriserConnectionServerApplicationKt")
}