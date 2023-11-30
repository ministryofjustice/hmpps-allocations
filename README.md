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

### How to run locally against dev environment

#### Running hmpps-allocations service locally
* Local development is normally done by working TDD and using unit/integration tests to drive development (and validate everything works) - see the `How to run the tests locally` section below for how to do this
* There are occasions where you might want to run locally against the dev environment so that you can check everything is working with real(ish) data on the DEV environment
* The following steps will allow you to do this and integrate with:
    * DEV Allocations DB (AWS RDS Database)
    * DEV hmpps-tier service
    * DEV assessment service
    * DEV assess-risks-needs service
    * DEV workforce-allocations-to-delius service
    * DEV community service
* The host urls for all the services listed above are configured in `application-local.yml`
* All secrets for authentication with these service and connection to the RDS database are passed as VM options (and not kept in code for security reasons) - see later section

#### Prep for running service - Connect to DEV RDS DB
* To connect to the DEV database we will need to port forward it
* Here is the wiki on how to do this [Access the DEV RDS Database](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/other-topics/rds-external-access.html#accessing-your-rds-database)

#### Run localstack docker service locally
* we can run `localstack` locally as a docker container so that we do not need to integrate with the messaging infrastructure in AWS (the application is reliant on this for processing events)
* to run `localstack` run this from this repo's root directory: 
```
docker-compose up -d localstack
```
* with the above command, you will have noticed that we are specifically running the `localstack` container only. If we were to run the usual `docker-compose up -d` command then we would run the `postgres` container also which would port clash with the port forward we are running for the DEV DB

#### Run service in Intellij
* Right-click and Run `uk.gov.justice.digital.hmpps.hmppsallocations.HmppsAllocations`
* This will fail initially but will have created a `Run Configuration` called `HmppsAllocations` in the configuration dropdown next to the `Run` and `Debug` buttons
* Click on the `HmppsAllocations` configuration > `Edit Configuration`
* In the `VM Options` box paste the following:
```
-Dspring.profiles.active=local
-Doauth.client.id=<retrieve_k8s_secret__AUTH_API_CLIENT_ID> 
-Doauth.client.secret=<retrieve_k8s_secret__AUTH_API_CLIENT_SECRET> 
-Ddatabase.name=<retrieve_k8s_secret__database_name> 
-Ddatabase.username=<retrieve_k8s_secret__database_username> 
-Ddatabase.password=<retrieve_k8s_secret__database_password>
```
* The placeholder values in the above properties need to be swapped out for the real secrets in your configuration
* These secrets are stored in `Kubernetes` and can be accessed in the `hmpps-allocations` and `rds-allocation-instance-output` secrets
* Here is a guide for [connecting to the Kubernetes Cluster](https://user-guide.cloud-platform.service.justice.gov.uk/documentation/getting-started/kubectl-config.html#connecting-to-the-cloud-platform-39-s-kubernetes-cluster) to access the secrets
* Once the secrets are finalised save the configuration with the following:
  * Click `Apply`
  * Click `OK`
* You should now be able to `Run` and `Debug` the application using this configuration by hitting the `Run` and `Debug` buttons next to the configuration

#### Run service in Terminal
Execute the following command:
```shell
./gradlew bootRun -Dspring.profiles.active=local -Doauth.client.id=<retrieve_k8s_secret__AUTH_API_CLIENT_ID> -Doauth.client.secret=<retrieve_k8s_secret__AUTH_API_CLIENT_SECRET> -Ddatabase.name=<retrieve_k8s_secret__database_name> -Ddatabase.username=<retrieve_k8s_secret__database_username> -Ddatabase.password=<retrieve_k8s_secret__database_password>
```

### How to run the tests locally

Execute the following commands:

```shell
docker compose up -d
./gradlew check
```

## Code Style

[ktlint](https://github.com/pinterest/ktlint) is the authority on style and is enforced on build.

Run `./gradlew ktlintFormat` to fix formatting errors in your code before commit.

To apply the formatting to Intellij and add a pre-commit hook run the following commands:

```shell
./gradlew ktlintApplyToIdea addKtlintFormatGitPreCommitHook
```
