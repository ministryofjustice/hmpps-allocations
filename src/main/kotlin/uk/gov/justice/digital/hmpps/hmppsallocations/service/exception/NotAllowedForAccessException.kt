package uk.gov.justice.digital.hmpps.hmppsallocations.service.exception

class NotAllowedForAccessException(msg: String, val crn: String) : RuntimeException(msg)
