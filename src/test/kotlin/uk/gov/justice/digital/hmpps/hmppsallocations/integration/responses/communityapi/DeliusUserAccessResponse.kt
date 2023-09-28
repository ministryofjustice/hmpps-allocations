package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.communityapi

fun deliusUserAccessResponse(crn: String, restricted: Boolean, excluded: Boolean) = """
  {
    "access": [
      {
        "crn": "$crn",
        "userRestricted": $restricted,
        "userExcluded": $excluded
      }
    ]
  }
""".trimIndent()
