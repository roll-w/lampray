# Lampray

[![License][liBadge]][liLink]

Lampray is a blog system built with Spring Boot 3 and Vue3.

## Requirements

- Java 17+
- MySQL 8.0+

## Build

> This part includes the steps to get the backend of the project running.
> For the frontend, please refer to the [frontend](lampray-frontend/README.md).

### Prerequisites

This project requires the following libraries that need you
to install to your local Maven repository:

- [web-common](https://github.com/Roll-W/web-common-starter)

### Build Jar

After cloning the repository and installing the required libraries,
you can build the project using the following command:

```shell
./gradlew build
```

or if you want to skip the tests:

```shell
./gradlew assemble 
```

After building the project, you should be able to find the jar file in
`lampray-web/build/libs` directory.

### Build Distribution Pack

In the previous step, we primarily covered how to build the entire project,
which is mainly intended for local execution. However, when running in other
environments, for standardization purposes, we need a distribution package.
In the following section, we will introduce how to create a distribution
package.

To generate the distribution package, run the following command:

```shell
./gradlew package
```

This command will generate a compressed file, similar to `lampray-{version}-dist.tar.gz`,
under the `build/dist` directory. This file includes the base JAR file, startup
scripts, and other resources.

### Build Image

This section provides guidance on how to build a Docker image.
Before continue, ensure that Docker is installed in your build environment.

To build the Docker image, run the following command:

```shell
./gradlew buildImage
```

After the build process is complete, you can find the image with the name
`lampray:{version}` in the local Docker image list.

## Configuration

To start up the application, you need to provide a configuration file.

The configuration file uses the `properties` format, like the following:

```properties
# Database Configuration
database.url=jdbc:mysql://localhost:3306/lampray
database.username=root
database.password=root

# Server Configuration
server.port=5100
```

## Running

### Before Running

Before running the application, you need to make sure that the MySQL
database is running and the database is created.

If you haven't created the database, you can create it using the following
SQL command:

```sql
CREATE DATABASE lampray;
```

After the application starts, it will automatically create the tables
and indexes required by the application.

### Running the Application

After building the project, then you can run the application
using the following command:

```shell
java -jar lampray.jar  # Replace lampray.jar with the actual jar file name
```

Or if you are using the distribution pack:

```shell
bin/lampray # Replace with the actual path to `lampray`
```

> Current support command line arguments:
> - `--config`, `-c`: Specify the configuration file to use.
    > Default will try find `lampray.conf` in the current directory and the
    > `conf` directory under the working directory.

By default, the application will start on port `5100`. And database
related configurations must be provided in the configuration file,
or the application will fail to start.

You can use the environment variable `JAVA_OPTS` to specify the
JVM options, like the following:

```shell
JAVA_OPTS="-Xmx1024m -Xms64m" bin/lampray
```

### Running with Docker

If you have built the Docker image, you can run the application using the
following command:

> [!NOTE]
> Before running the command, you need to prepare a configuration file named
> `lampray.conf` in your local directory for mount to the container and replace
> the `/path/to/lampray.conf` with the actual path to the configuration file.

```shell
docker run \
  -d \
  -it \
  -p 5100:5100 \
  --network host \
  -v /path/to/lampray.conf:/app/lampray/conf/lampray.conf \
  --name lampray lampray:{version}
```

> Options:
> - `--network host`: Use the host network, or replace with your own network.
> - `-v /path/to/lampray.conf:/app/lampray/conf/lampray.conf`: Mount the configuration file to the container.

Also, you can use the environment variable `JAVA_OPTS` to specify the
JVM options, like the following:

```shell
docker run \
  #...other options omitted
  -e JAVA_OPTS="-Xmx1024m -Xms64m" \  # replace with your own JVM options
  --name lampray lampray:{version}
```

## Features

- User Management
- Article Management

## Contributing

Expected workflow is: Fork -> Patch -> Push -> Pull Request

## Licence

The Lampray project is licensed under the Apache License, Version 2.0.

```text
Copyright (C) 2023-2025 RollW

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

[liBadge]: https://img.shields.io/github/license/roll-w/lampray?color=569cd6&style=flat-square

[liLink]: https://github.com/roll-w/lampray/blob/master/LICENSE
