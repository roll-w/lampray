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

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "lamp-blog"
include(":lamp-blog-web")
include(":lamp-blog-email")
include(":lamp-blog-file")
include(":lamp-blog-file-awss3")
include(":lamp-blog-user")
include(":lamp-blog-user:user-api")
include(":lamp-blog-user:user-service")
include(":lamp-blog-iam")
include(":lamp-blog-iam:authentication-api")
include(":lamp-blog-iam:authentication-service")
include(":lamp-blog-common")
include(":lamp-blog-file-api")
include(":lamp-blog-content")
include(":lamp-blog-content:content-api")
include(":lamp-blog-content:content-service")

project(":lamp-blog-content:content-api").projectDir = file("lamp-blog-content/content-api")
project(":lamp-blog-content:content-service").projectDir = file("lamp-blog-content/content-service")
project(":lamp-blog-user:user-api").projectDir = file("lamp-blog-user/user-api")
project(":lamp-blog-user:user-service").projectDir = file("lamp-blog-user/user-service")
project(":lamp-blog-file-api").projectDir = file("lamp-blog-file/lamp-blog-file-api")
project(":lamp-blog-file-awss3").projectDir = file("lamp-blog-file/lamp-blog-file-awss3")
project(":lamp-blog-iam:authentication-api").projectDir = file("lamp-blog-iam/authentication-api")
project(":lamp-blog-iam:authentication-service").projectDir = file("lamp-blog-iam/authentication-service")

