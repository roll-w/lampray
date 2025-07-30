/*
 * Copyright (C) 2023-2025 RollW
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
    `kotlin-dsl`
    `java-gradle-plugin`
}

val kotlinVersion = findProperty("kotlin.version").toString()
val springBootVersion = findProperty("spring.boot.version").toString()

dependencies {
    implementation("io.spring.gradle:dependency-management-plugin:1.1.6")
    implementation("org.springframework.boot:spring-boot-gradle-plugin:${springBootVersion}")
    implementation(kotlin("gradle-plugin", kotlinVersion))
    // plugin.spring
    implementation(kotlin("allopen", kotlinVersion))
    // plugin.jpa
    implementation(kotlin("noarg", kotlinVersion))
}

repositories {
    mavenLocal()
    gradlePluginPortal()
    mavenCentral()
}

gradlePlugin {
    plugins {
        create("containerPlugin") {
            id = "space.lingu.gradle.container"
            implementationClass = "space.lingu.gradle.container.ContainerPlugin"
        }
    }
}
