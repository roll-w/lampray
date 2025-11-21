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

import org.jetbrains.kotlin.gradle.tasks.KaptGenerateStubs
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import space.lingu.GenerateBuildConfigTask

plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api(libs.rollw.web.common.core)
    api("org.springframework:spring-web")
    api("org.springframework:spring-expression")
    api("org.slf4j:slf4j-api")
    api("org.apache.commons:commons-lang3")
    api(libs.com.google.guava)
    api(libs.commons.text)
    api("com.fasterxml.jackson.core:jackson-annotations")
}

description = "lampray-common"


tasks.register<GenerateBuildConfigTask>("generateBuildConfig") {
    packageName = "tech.lamprism.lampray"
    outputDirectory = file("build/")
    this.version = project.version.toString()
}

tasks.named<ProcessResources>("processResources") {
    dependsOn("generateBuildConfig")
}

tasks.named<KotlinCompile>("compileKotlin") {
    dependsOn("generateBuildConfig")
}

tasks.withType<KaptGenerateStubs> {
    dependsOn("generateBuildConfig")
}

sourceSets {
    main {
        java.srcDir("build/generated/sources/buildconfig/java/main/")
    }
}
