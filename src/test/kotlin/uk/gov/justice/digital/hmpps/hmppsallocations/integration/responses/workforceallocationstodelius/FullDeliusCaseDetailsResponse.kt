package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius

import uk.gov.justice.digital.hmpps.hmppsallocations.client.CommunityPersonManager
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain.CaseDetailsIntegration
import java.time.LocalDate
import java.time.format.DateTimeFormatter

fun fullDeliusCaseDetailsResponse(vararg caseDetailsIntegrations: CaseDetailsIntegration) = """
{
  "cases": [
    ${caseDetailsIntegrations.map { deliusCaseDetail(it) }.joinToString(",")}
  ]
}
""".trimIndent()

private fun deliusCaseDetail(caseDetailsIntegration: CaseDetailsIntegration) = """
  {
      "crn": "${caseDetailsIntegration.crn}",
      "name": {
        "forename": "Dylan Adam",
        "middleName": "string",
        "surname": "Armstrong"
      },
      "event": {
        "number": "${caseDetailsIntegration.eventNumber}",
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
      },
      "type": "CUSTODY",
      "probationStatus": {
         "status": "STATUS",
         "description": "${caseDetailsIntegration.probationStatusDescription}"
      }
  """ +
  initialAppointment(caseDetailsIntegration.initialAppointment) +
  communityPersonManager(caseDetailsIntegration.communityPersonManager) +
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

private fun communityPersonManager(communityPersonManager: CommunityPersonManager?): String {
  return communityPersonManager?.let {
    """
      ,"communityPersonManager": {
        "code": "N54B999",
        "name": {
          "forename": "${communityPersonManager.name.forename}",
          "surname": "${communityPersonManager.name.surname}"
        },
        "teamCode": "N54NGH"
        ${showGrade(communityPersonManager)}
      }
    """.trimIndent()
  } ?: ""
}

private fun showGrade(communityPersonManager: CommunityPersonManager): String {
  if (communityPersonManager.grade != null) {
    return ""","grade": "${communityPersonManager.grade}""""
  }
  return ""
}
