package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius

fun deliusRiskResponse() = """
{
  "crn": "J678910",
  "name": {
    "forename": "Dylan",
    "middleName": "Adam",
    "surname": "Armstrong"
  },
  "activeRegistrations": [
    {
      "description": "ALT Under MAPPA Arrangements",
      "startDate": "2021-08-30",
      "notes": "Some Notes"
    }
  ],
  "inactiveRegistrations": [
    {
      "description": "Child Protection",
      "startDate": "2021-05-20",
      "endDate": "2021-08-30",
      "notes": "Some Notes."
    }
  ],
  "ogrs": {
    "lastUpdatedDate": "2018-11-17",
    "score": 85
  },
  "rosh": {
    "overallRisk": "MEDIUM",
    "assessmentDate": "2022-10-07",
    "riskInCommunity": {
      "Public": "HIGH",
      "Children": "LOW",
      "Known Adult": "MEDIUM",
      "Staff": "VERY_HIGH"
    }
  },
  "rsr": {
    "percentageScore": 3.8,
    "levelScore": "MEDIUM",
    "completedDate": "2019-02-12"
  }
}
""".trimIndent()
