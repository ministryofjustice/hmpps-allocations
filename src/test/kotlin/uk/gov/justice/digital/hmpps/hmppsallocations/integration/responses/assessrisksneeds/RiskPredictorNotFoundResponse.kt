package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessrisksneeds

fun riskPredictorNotFoundResponse() = """
{
  "status": 404,
  "developerMessage": "System is down",
  "errorCode": 20012,
  "userMessage": "Prisoner Not Found",
  "moreInfo": "Hard disk failure"
}
""".trimIndent()
