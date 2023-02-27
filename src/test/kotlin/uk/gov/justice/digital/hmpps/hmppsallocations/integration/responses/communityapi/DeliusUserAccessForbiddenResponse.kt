package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.communityapi

fun deliusUserAccessForbiddenResponse() = """
  {
      "userRestricted": true,
      "restrictionMessage": "This is a restricted offender record. Please contact a system administrator",
      "userExcluded": false
  }
""".trimIndent()
