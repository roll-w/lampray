# Lampray

[![License](https://img.shields.io/github/license/roll-w/lampray?color=569cd6&style=flat-square)](https://github.com/roll-w/lampray/blob/master/LICENSE)
[![Build Image](https://github.com/roll-w/lampray/actions/workflows/build_image.yaml/badge.svg?branch=master)](https://github.com/roll-w/lampray/actions/workflows/build_image.yaml)
[![Ask DeepWiki](https://deepwiki.com/badge.svg)](https://deepwiki.com/roll-w/lampray)

Lampray is a blog system built with Spring Boot 3 and Vue3.

## Requirements

- Java 17+
- Supported databases: sqlite (default, in-memory or file), h2 (in-memory or file), mysql, postgresql, mariadb, oracle,
  sqlserver

Recommended minimum database versions (suggested):

- MySQL: 8.0+
- PostgreSQL: 12+
- MariaDB: 10.5+
- Oracle: 19c+
- SQL Server: 2017+

Notes:

- The application defaults to using SQLite (in-memory) if no database configuration is provided.
- For network databases (MySQL/PostgreSQL/MariaDB/Oracle/SQL Server) you must have the database server running and
  create or configure the target database (default name: `lampray`).

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

The configuration file uses the `toml` format.
The application supports multiple database types and flexible target formats (see the examples below):

Supported target formats:

- Network address: `host:port` (e.g. `localhost:3306`)
- File-based: `file:./data/app.db` or `file:/absolute/path/to/db`
- In-memory: `memory`

Examples:

Default (SQLite in-memory):

> Not recommended for production use.

```toml
[database]
type = "sqlite"
target = "memory"          # use "file:./data/lampray.db" for a file-based SQLite DB
name = "lampray"           # ignored for in-memory/file SQLite but kept for consistency
username = ""
password = ""
```

MySQL example (network database):

```toml
[database]
type = "mysql"
target = "localhost:3306"
name = "lampray"
username = "root"
password = "password"
options = [] # optional extra JDBC parameters, e.g. ["useSSL=false", "serverTimezone=UTC"]
```

Additional notes:

- `type` accepts: `sqlite`, `mysql`, `postgresql`, `h2`, `oracle`, `sqlserver`, `mariadb`.
- `target` is parsed to determine the JDBC URL. Use `file:` prefix for file-based DBs and `memory` for in-memory DBs.
- `options` can provide extra JDBC parameters (as an array of `key=value` strings).

## Running

### Before Running

Before running the application, make sure your chosen database is available:

- For the default SQLite in-memory mode (`type = "sqlite"` and `target = "memory"`), no database setup is required.
- For a file-based SQLite database (e.g. `target = "file:./data/lampray.db"`), ensure the application has write
  permission to the directory.
- For network databases (MySQL/PostgreSQL/MariaDB/Oracle/SQL Server), ensure the server is running and the target
  database exists.
  By default the application expects a database named `lampray`;
  create it if necessary, for example (MySQL/PostgreSQL):
  ```sql
  CREATE DATABASE lampray;
  ```

After the application starts, it will create the tables and indexes required by the application
(for supported engines where the user account has sufficient privileges).

### Running the Application

After building the project, then you can run the application
using the following command:

```shell
java -jar lampray.jar start # Replace lampray.jar with the actual jar file name
```

Or if you are using the distribution pack:

```shell
bin/lampray start # Replace with the actual path to `lampray`
```

> Current support command line arguments:
> - `--config`, `-c`: Specify the configuration file to use. Default will try find
    `lampray.toml` in the current directory and the `conf` directory under the working directory.

You can use the command `bin/lampray help` to see the available commands
and options.

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
> `lampray.toml` in your local directory for mount to the container and replace
> the `/path/to/conf/` with the actual path to the conf directory.

> [!NOTE]
> You need to create a Docker network named `lampray` first, or replace it with
> your own network.
>
> You can create a Docker network using the following command:
>
> ```shell
> docker network create lampray
> ```

```shell
docker run \
  -d \
  -it \
  -p 5100:5100 \
  -p 5101:5101 \
  --network lampray \
  -v /path/to/conf:/app/lampray/conf \
  --name lampray lampray:{version}
```

> Options:
> - `--network lampray`: Use the `lampray` network, or replace with your own network.
> - `-v /path/to/conf:/app/lampray/conf/`: Mount the configuration directory to the container. Needs a configuration
    file named `lampray.toml` in the local directory.
> - `-p 5100:5100 -p 5101:5101`: Port `5100` is the default HTTP port, and `5101` is the default SSH port for the
    application. You can change the port mapping according to your configuration.

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
