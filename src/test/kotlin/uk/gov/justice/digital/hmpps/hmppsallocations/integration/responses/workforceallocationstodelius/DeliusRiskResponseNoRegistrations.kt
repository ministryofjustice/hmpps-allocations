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
  "inactiveRegistrations": []
}
""".trimIndent()
