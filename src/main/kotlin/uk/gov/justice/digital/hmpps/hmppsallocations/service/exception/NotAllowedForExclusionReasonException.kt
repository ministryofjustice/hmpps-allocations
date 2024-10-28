package uk.gov.justice.digital.hmpps.hmppsallocations.service.exception

class NotAllowedForExclusionReasonException(msg: String, val crn: String) : RuntimeException(msg)
