package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

import uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain.CaseDetailsInitialAppointment
import java.time.LocalDate
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
        "forename": "Dylan Adam",
        "middleName": "string",
        "surname": "Armstrong"
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
        "date": "2022-11-05",
        "length": "5 Weeks"
      }
  """ +
  initialAppointment(caseDetailsInitialAppointment.initialAppointment) +
  "}".trimIndent()

private fun initialAppointment(initialAppointment: LocalDate?): String {
  return initialAppointment?.let {
    """
      ,"initialAppointment": {
        "date": ${initialAppointment.format(DateTimeFormatter.ofPattern("\"yyyy-MM-dd\""))}
      }
    """.trimIndent()
  } ?: ""
}
