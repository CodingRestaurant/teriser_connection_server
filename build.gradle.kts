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

    implementation ("com.google.code.gson:gson:2.8.8")

    implementation (platform("io.projectreactor:reactor-bom:2020.0.12"))
    implementation ("io.projectreactor.netty:reactor-netty-core")
    implementation ("io.projectreactor.netty:reactor-netty-http")

    testImplementation ("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testRuntimeOnly ("org.junit.jupiter:junit-jupiter-engine:5.7.0")

    implementation ("com.sparkjava:spark-core:2.9.3")

    implementation(kotlin("stdlib-jdk8"))

    testCompileOnly("org.slf4j:slf4j-simple:1.7.29")
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

tasks.withType<Jar>() {

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    manifest {
        attributes["Main-Class"] = "com.codrest.teriser.connectionserver.MainKt"
    }

    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
}