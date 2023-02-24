package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessment

fun assessmentNotFoundResponse(crn: String) = """
  {
      "status": 404,
      "developerMessage": "Offender not found for CRN, $crn"
  }
""".trimIndent()
