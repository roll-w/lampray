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
    api(project(":lampray-common"))
    api("org.liquibase:liquibase-core")
    api("com.fasterxml.jackson.core:jackson-databind")
    api("com.fasterxml.jackson.module:jackson-module-kotlin")
    api("com.github.luben:zstd-jni:1.5.7-6")
    api("org.msgpack:jackson-dataformat-msgpack:0.9.10")
}

description = "lampray-common-data"
