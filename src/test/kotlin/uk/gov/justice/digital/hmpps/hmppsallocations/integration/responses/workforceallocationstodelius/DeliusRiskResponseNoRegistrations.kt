package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius

fun deliusRiskResponseNoRegistrationsNoOgrs() = """
{
  "crn": "X373307",
  "name": {
    "forename": "firstName",
    "middleName": "middleName",
    "surname": "lastName"
  },
  "activeRegistrations": [],
  "inactiveRegistrations": [],
  "rosh": {
    "overallRisk": "MEDIUM",
    "assessmentDate": "2022-10-07T13:11:50",
    "riskInCommunity": {
      "Public": "HIGH",
      "Children": "LOW",
      "Known Adult": "MEDIUM",
      "Staff": "HIGH"
    }
  },
  "rsr": {
    "percentageScore": 2.2,
    "levelScore": "LOW",
    "completedDate": "2019-02-12"
  }
}
""".trimIndent()
