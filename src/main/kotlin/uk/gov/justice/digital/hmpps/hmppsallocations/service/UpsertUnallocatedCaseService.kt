package uk.gov.justice.digital.hmpps.hmppsallocations.service

interface UpsertUnallocatedCaseService {

  fun upsertUnallocatedCase(crn: String, convictionId: Long)
}
