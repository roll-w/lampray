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
    id("buildlogic.java-conventions")
    id("buildlogic.jpa-conventions")
}

dependencies {
    api(project(":lamp-blog-iam:authentication-api"))
    api(project(":lamp-blog-user:user-service"))
    api(project(":lamp-blog-common-data"))
    api(libs.lingu.light.core)
    api(project(":lamp-blog-email"))
    implementation(libs.io.jsonwebtoken.jjwt.api)
    implementation(libs.io.jsonwebtoken.jjwt.impl)
    implementation(libs.io.jsonwebtoken.jjwt.jackson)
}

description = "lamp-blog-authentication-service"
