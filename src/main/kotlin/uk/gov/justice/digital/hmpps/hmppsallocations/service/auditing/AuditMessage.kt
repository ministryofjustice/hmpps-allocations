package uk.gov.justice.digital.hmpps.hmppsallocations.service.auditing

data class AuditMessage(
  val operationId: String,
  val what: String,
  val `when`: String,
  val who: String,
  val service: String,
  val details: String,
)
