package uk.gov.justice.digital.hmpps.hmppsallocations.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDateTime

private const val CRN = "crn"

private const val TEAM_CODE = "teamCode"

private const val PROVIDER_CODE = "providerCode"

@Component
class TelemetryService(@Autowired private val telemetryClient: TelemetryClient) {

  fun trackUnallocatedCaseAllocated(unallocatedCaseEntity: UnallocatedCaseEntity) {
    trackEvent(
      TelemetryEventType.EventAllocated,
      mapOf(
        CRN to unallocatedCaseEntity.crn,
        TEAM_CODE to unallocatedCaseEntity.teamCode,
        PROVIDER_CODE to unallocatedCaseEntity.providerCode,
        "wmtPeriod" to getWmtPeriod(LocalDateTime.now())
      )
    )
  }

  fun trackAllocationDemandRaised(unallocatedCaseEntity: UnallocatedCaseEntity) {
    trackEvent(
      TelemetryEventType.ALLOCATION_DEMAND_RAISED,
      mapOf(
        CRN to unallocatedCaseEntity.crn,
        TEAM_CODE to unallocatedCaseEntity.teamCode,
        PROVIDER_CODE to unallocatedCaseEntity.providerCode
      )
    )
  }

  private fun trackEvent(eventType: TelemetryEventType, customDimensions: Map<String, String?>) {
    telemetryClient.trackEvent(eventType.eventName, customDimensions, null)
  }
}
