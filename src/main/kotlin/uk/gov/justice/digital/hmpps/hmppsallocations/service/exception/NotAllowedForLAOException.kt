package uk.gov.justice.digital.hmpps.hmppsallocations.service.exception

class NotAllowedForLAOException(msg: String, val crn: String) : RuntimeException(msg)
