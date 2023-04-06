package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius

fun impactResponse(crn: String, staffCode: String): String {
  return """
  {
      "crn": "$crn",
      "name": {
          "forename": "Jonathon",
          "middleName": "",
          "surname": "Jones"
      },
      "staff": {
          "code": "$staffCode",
          "name": {
              "forename": "Sheila",
              "surname": "Hancock"
          },
          "email":"Sheila.hancock@email.com",
          "grade": "PO"
      }
  }
  """.trimIndent()
}
