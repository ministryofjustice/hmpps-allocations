package uk.gov.justice.digital.hmpps.hmppsallocations.service.auditing

data class AuditMessage(
  val auditObject: AuditObject,
  val crn: String,
  val operation: String,
  val loggedInUser: String,
)
