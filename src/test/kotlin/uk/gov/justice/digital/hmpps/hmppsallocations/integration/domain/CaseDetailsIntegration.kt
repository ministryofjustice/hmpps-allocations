package uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain

import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityPersonManager
import java.time.LocalDate

data class CaseDetailsIntegration(
  val crn: String,
  val eventNumber: String,
  val initialAppointment: LocalDate?,
  val probationStatusDescription: String,
  val communityPersonManager: CommunityPersonManager?,
)
