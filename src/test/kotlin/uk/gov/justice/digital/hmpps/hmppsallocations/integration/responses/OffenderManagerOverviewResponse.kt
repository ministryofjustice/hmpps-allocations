package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun offenderManagerOverviewResponse() = """
  {
    "forename": "John",
    "surname": "Smith",
    "grade": "PO",
    "code": "OM1",
    "teamName": "Test Team",
    "totalCases": 22,
    "capacity": 67.4,
    "weeklyHours": 22.5,
    "totalReductionHours": 10,
    "pointsAvailable": 1265,
    "pointsUsed": 1580,
    "pointsRemaining": -315
  }
""".trimIndent()
