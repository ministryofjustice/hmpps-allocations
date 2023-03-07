package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius

fun impactNoStaffResponse(crn: String): String {
  return """
  {
      "crn": "$crn",
      "name": {
          "forename": "Jonathon",
          "middleName": "",
          "surname": "Jones"
      }
  }
  """.trimIndent()
}
