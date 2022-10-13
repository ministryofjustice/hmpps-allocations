# hmpps-allocations

[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-allocations/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-allocations)
[![API docs](https://img.shields.io/badge/API_docs-view-85EA2D.svg?logo=swagger)](https://hmpps-allocations-dev.hmpps.service.justice.gov.uk/swagger-ui.html)]
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

* Java JDK 18+
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
docker compose up -d localstack postgres
./gradlew check
```

## Code Style

[ktlint](https://github.com/pinterest/ktlint) is the authority on style and is enforced on build.

Run `./gradlew ktlintFormat` to fix formatting errors in your code before commit.

To apply the formatting to Intellij and add a pre-commit hook run the following commands:

```shell
./gradlew ktlintApplyToIdea addKtlintFormatGitPreCommitHook
```
