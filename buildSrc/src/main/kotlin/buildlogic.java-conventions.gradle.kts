import org.jetbrains.kotlin.gradle.plugin.kotlinToolingVersion

/*
 * Copyright (C) 2023 RollW
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    java
    `java-library`
    kotlin("jvm")
    kotlin("plugin.spring")
    id("io.spring.dependency-management")
    id("org.springframework.boot")
}

val kotlinVersion = "2.0.20"

dependencies {
    api("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    api("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    testImplementation("org.jetbrains.kotlin:kotlin-test:${kotlinVersion}")

    implementation("space.lingu.fiesta:fiesta-annotations:0.2.1")
    annotationProcessor("space.lingu.fiesta:fiesta-checker:0.2.1")
}

group = "space.lingu.lamp-blog"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xjvm-default=all-compatibility"
        )
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc> {
    options.encoding = "UTF-8"
}