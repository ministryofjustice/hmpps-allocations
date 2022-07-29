package uk.gov.justice.digital.hmpps.hmppsallocations.service

enum class TelemetryEventType(val eventName: String) {
  EventAllocated("EventAllocated"), ALLOCATION_DEMAND_RAISED("AllocationDemandRaised")
}
