package uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain

import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityPersonManager
import uk.gov.justice.digital.hmpps.hmppsallocations.client.InitialAppointment

data class CaseDetailsIntegration(
  val crn: String,
  val eventNumber: String,
  val initialAppointment: InitialAppointment?,
  val probationStatus: String,
  val probationStatusDescription: String,
  val communityPersonManager: CommunityPersonManager?,
  val handoverDate: String?,
)
