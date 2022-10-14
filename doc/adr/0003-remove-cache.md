# 3. Remove Cache

Date: 2022-10-13

## Status

Accepted

## Context

Removed the cache and retrieving individual case initial appointments and added retrieving from a search endpoint which expected a list of case identifiers.

## Decision

Removed the cache

## Consequences

Easier to debug if an issue occurs, no longer constrained to 1 instance
