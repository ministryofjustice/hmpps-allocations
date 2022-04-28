# 2. Single Instance

Date: 2022-04-28

## Status

Accepted

## Context

A cache is used when enriching unallocated cases initial appointments. The enriching is done when all the unallocated cases are requested. The risk with introducing
a cache is that if multiple instances of the application are running the cache data sets could get out of sync and one instance would be producing different data from the other.

## Decision

Reduce the instance count down to 1.

## Consequences

It will be more difficult to increase instances as the ability to sync caches across instances would need to be implemented.
Monitoring is in place for resource use of instances and if pods become unhealthy so if the traffic is too much for one instance
alerts will automatically appear.
