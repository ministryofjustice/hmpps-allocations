package uk.gov.justice.digital.hmpps.hmppsallocations.integration.requests

class OffenderManagerPotentialCaseRequest
fun offenderManagerPotentialCaseRequest() = """
  {
  "tier": "C1",
  "type": "COMMUNITY",
  "t2A": false
}
""".trimIndent()
