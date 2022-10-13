package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

import uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain.CaseDetailsInitialAppointment
import java.time.format.DateTimeFormatter

fun fullDeliusCaseDetailsResponse(vararg caseDetailsInitialAppointments: CaseDetailsInitialAppointment) = """
{
  "cases": [
    ${caseDetailsInitialAppointments.map { deliusCaseDetail(it) }.joinToString(",")}
  ]
}
""".trimIndent()

private fun deliusCaseDetail(caseDetailsInitialAppointment: CaseDetailsInitialAppointment) = """
  {
      "crn": "${caseDetailsInitialAppointment.crn}",
      "name": {
        "forename": "string",
        "middleName": "string",
        "surname": "string"
      },
      "event": {
        "number": "${caseDetailsInitialAppointment.eventNumber}",
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
        "date": ${caseDetailsInitialAppointment.initialAppointment?.format(DateTimeFormatter.ofPattern("\"yyyy-MM-dd\""))}
      }
    }
""".trimIndent()
