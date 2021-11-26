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

# Allocation Endpoints

## Upload CSV

There is an endpoint which will generate messages to be consumed and written to the postgres database backing this
application. The endpoint is found at `POST /cases/unallocated/upload`.

To Use this endpoint in a deployed environment first port forward to the deployment by executing the following command:

```shell
kubectl port-forward deployment/hmpps-allocations 8080:8080 -n <NAMESPACE>
```

replacing `<NAMESPACE>` with the desired namespace for the environment.

The curl command to call the endpoint is:

```shell
curl http://localhost:8080/cases/unallocated/upload --request POST --form 'file=@"somefile.csv"'
```

The column order for the csv is `<crn>,<tier>,<status>` with no headers