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

rootProject.name = "lampray"

includeBuild("build-logic")

include(":lampray-web")
include(":lampray-file")
include(":lampray-file:file-api")
include(":lampray-file:file-awss3")
include(":lampray-user")
include(":lampray-user:user-api")
include(":lampray-user:staff-api")
include(":lampray-user:staff-service")
include(":lampray-user:user-details-api")
include(":lampray-user:user-details-service")
include(":lampray-user:user-service")
include(":lampray-iam")
include(":lampray-iam:authentication-api")
include(":lampray-iam:authentication-service")
include(":lampray-common")
include(":lampray-common-data")
include(":lampray-push")
include(":lampray-push:push-api")
include(":lampray-content")
include(":lampray-content:content-api")
include(":lampray-content:content-service")
include(":lampray-content:article-service")
include(":lampray-content:comment-service")
include(":lampray-content:review-service")
include(":lampray-system")
include(":lampray-system:setting-api")
include(":lampray-system:setting-service")
include(":lampray-system:message-resource-api")
include(":lampray-system:message-resource-service")

project(":lampray-push:push-api").projectDir = file("lampray-push/push-api")
project(":lampray-content:content-api").projectDir = file("lampray-content/content-api")
project(":lampray-content:content-service").projectDir = file("lampray-content/content-service")
project(":lampray-content:article-service").projectDir = file("lampray-content/article-service")
project(":lampray-content:comment-service").projectDir = file("lampray-content/comment-service")
project(":lampray-content:review-service").projectDir = file("lampray-content/review-service")
project(":lampray-user:user-api").projectDir = file("lampray-user/user-api")
project(":lampray-user:staff-api").projectDir = file("lampray-user/staff-api")
project(":lampray-user:staff-service").projectDir = file("lampray-user/staff-service")
project(":lampray-user:user-details-api").projectDir = file("lampray-user/user-details-api")
project(":lampray-user:user-details-service").projectDir = file("lampray-user/user-details-service")
project(":lampray-user:user-service").projectDir = file("lampray-user/user-service")
project(":lampray-file:file-api").projectDir = file("lampray-file/file-api")
project(":lampray-file:file-awss3").projectDir = file("lampray-file/file-awss3")
project(":lampray-iam:authentication-api").projectDir = file("lampray-iam/authentication-api")
project(":lampray-iam:authentication-service").projectDir = file("lampray-iam/authentication-service")
project(":lampray-system:setting-api").projectDir = file("lampray-system/setting-api")
project(":lampray-system:setting-service").projectDir = file("lampray-system/setting-service")
project(":lampray-system:message-resource-api").projectDir = file("lampray-system/message-resource-api")
project(":lampray-system:message-resource-service").projectDir = file("lampray-system/message-resource-service")
