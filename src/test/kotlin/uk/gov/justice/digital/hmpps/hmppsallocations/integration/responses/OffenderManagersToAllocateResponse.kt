package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun offenderManagersToAllocateResponse() = """
  {
    "offenderManagers": [
      {
        "forename": "Ben",
        "surname": "Doe",
        "grade": "PO",
        "totalCommunityCases": 15,
        "totalCustodyCases": 20,
        "capacity": 0.5,
        "code": "OM1"
      },
      {
        "forename": "Sally",
        "surname": "Smith",
        "grade": "TPO",
        "totalCommunityCases": 27,
        "totalCustodyCases": 6,
        "capacity": 0.25,
        "code": "OM2"
      }
    ]
  }
""".trimIndent()
