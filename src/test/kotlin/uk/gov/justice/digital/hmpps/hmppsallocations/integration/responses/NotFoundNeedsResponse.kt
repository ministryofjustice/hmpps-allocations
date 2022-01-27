package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun notFoundNeedsResponse() = """
  {
    "status": 404,
    "developerMessage": "Assessement not found",
    "errorCode": 20012,
    "userMessage": "Assessement not found",
    "moreInfo": "Assessement not found"
  }
""".trimIndent()
