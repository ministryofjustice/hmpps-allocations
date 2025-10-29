package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.AllocatedActiveEvent
import java.time.LocalDate

data class DeliusAllocatedCaseView(
  val name: Name,
  val dateOfBirth: LocalDate,
  val gender: String,
  val pncNumber: String,
  val mainAddress: MainAddress,
  val nextAppointmentDate: LocalDate,
  val activeEvents: List<AllocatedActiveEvent>,
)
