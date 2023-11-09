package uk.gov.justice.digital.hmpps.hmppsallocations.service

import com.microsoft.applicationinsights.TelemetryClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private const val CRN = "crn"

private const val TEAM_CODE = "teamCode"

private const val PROVIDER_CODE = "providerCode"

@Component
class TelemetryService(@Autowired private val telemetryClient: TelemetryClient) {

  fun trackUnallocatedCaseAllocated(unallocatedCaseEntity: UnallocatedCaseEntity, teamCode: String?) {
    trackEvent(
      TelemetryEventType.EventAllocated,
      mapOf(
        CRN to unallocatedCaseEntity.crn,
        TEAM_CODE to unallocatedCaseEntity.teamCode,
        PROVIDER_CODE to unallocatedCaseEntity.providerCode,
        "allocatedTeamCode" to teamCode,
        "wmtPeriod" to getWmtPeriod(LocalDateTime.now()),
        "startTime" to unallocatedCaseEntity.createdDate.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        "endTime" to ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
      ),
    )
  }

  fun trackAllocationDemandRaised(unallocatedCaseEntity: UnallocatedCaseEntity) {
    trackEvent(
      TelemetryEventType.ALLOCATION_DEMAND_RAISED,
      mapOf(
        CRN to unallocatedCaseEntity.crn,
        TEAM_CODE to unallocatedCaseEntity.teamCode,
        PROVIDER_CODE to unallocatedCaseEntity.providerCode,
      ),
    )
  }

  private fun trackEvent(eventType: TelemetryEventType, customDimensions: Map<String, String?>) {
    telemetryClient.trackEvent(eventType.eventName, customDimensions, null)
  }
}
