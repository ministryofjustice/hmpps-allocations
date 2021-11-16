# hmpps-allocations

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-allocations/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-allocations)

This is the HMPPS Allocations service. This is used by Manage a workforce UI to serve unallocated cases.

## What it does

Listens to events from Delius, stores unallocated cases and writes an allocation into Delius

Integration points:
- Delius via SQS
- Community-api read and write
- Assessment-api read
- Postgres for database

## Continuous Integration
https://app.circleci.com/pipelines/github/ministryofjustice/hmpps-allocations



### Prerequisites
* Java JDK 16+
* An editor/IDE
* Gradle
* Docker
* OAuth token

### How to run locally

Execute the following commands:

```shell
docker-compose up -d
./gradlew bootRun
```

### How to run the tests locally

Execute the following commands:

```shell
docker compose up -d localstack
./gradlew check
```
