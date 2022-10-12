package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun initialAppointmentResponse(crn: String, InitialAppointment: LocalDate) = """
{
  "cases": [
    {
      "crn": "$crn",
      "name": {
        "forename": "string",
        "middleName": "string",
        "surname": "string"
      },
      "event": {
        "number": "3",
        "manager": {
          "code": "string",
          "name": {
            "forename": "string",
            "middleName": "string",
            "surname": "string"
          },
          "teamCode": "string"
        }
      },
      "sentence": {
        "type": "string",
        "date": "2022-10-12",
        "length": "string"
      },
      "initialAppointment": {
        "date": "${InitialAppointment.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}"
      }
    }
  ]
}
""".trimIndent()
