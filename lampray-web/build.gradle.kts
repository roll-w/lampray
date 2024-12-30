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
    id("buildlogic.spring-boot-conventions")
    id("buildlogic.jpa-conventions")
}

dependencies {
    implementation(project(":lampray-common"))
    implementation(project(":lampray-system:setting-service"))
    implementation(project(":lampray-system:message-resource-service"))
    implementation(project(":lampray-content:content-service"))
    implementation(project(":lampray-content:article-service"))
    implementation(project(":lampray-content:comment-service"))
    implementation(project(":lampray-content:review-service"))
    implementation(project(":lampray-iam:authentication-service"))
    implementation(project(":lampray-user:user-service"))
    implementation(project(":lampray-user:staff-service"))
    implementation(project(":lampray-user:user-details-service"))
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation(libs.rollw.web.common.spring.boot.starter)
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.apache.commons:commons-lang3")
    implementation(libs.com.google.guava)
    implementation(libs.commons.text)
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation(libs.spring.shell.starter)
    implementation(libs.jline.terminal.jna)
    implementation(libs.sshd.core)
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-quartz")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation(libs.caffeine)
    implementation("com.mysql:mysql-connector-j")
    kapt("org.springframework.boot:spring-boot-configuration-processor")
    implementation("com.zaxxer:HikariCP")
    implementation(libs.io.jsonwebtoken.jjwt.api)
    implementation(libs.io.jsonwebtoken.jjwt.impl)
    implementation(libs.io.jsonwebtoken.jjwt.jackson)
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

description = "lampray-web"

