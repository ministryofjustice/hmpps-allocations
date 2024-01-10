package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.communityapi

fun deliusUserAccessResponse(crn: String, restricted: Boolean, excluded: Boolean) = """
  {
    "access": [
      ${deliusCaseAccess(crn, restricted, excluded)}
    ]
  }
""".trimIndent()

fun deliusUserAccessResponse(caseAccessList: List<Triple<String, Boolean, Boolean>>): String =
  """ {
    "access": [
      ${caseAccessList.joinToString { 
        deliusCaseAccess(it.first, it.second, it.third)
      }}
    ]
  }"""

fun deliusCaseAccess(crn: String, restricted: Boolean, excluded: Boolean): String {
  return """
  {
    "crn": "$crn",
    "userRestricted": $restricted,
    "userExcluded": $excluded
  }
  """
}
