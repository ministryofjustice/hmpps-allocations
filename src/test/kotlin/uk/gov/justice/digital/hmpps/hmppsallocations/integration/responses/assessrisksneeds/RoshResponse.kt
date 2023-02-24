package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessrisksneeds

fun roshResponse() = """
   {
    "overallRisk": "VERY_HIGH",
    "assessedOn": "2022-10-07T13:11:50",
    "riskInCommunity": {
      "Public": "HIGH","Children": "LOW","Known Adult": "MEDIUM","Staff": "VERY_HIGH"
    },
    "riskInCustody": {
      "Public": "HIGH",  "Children": "LOW",  "Known Adult": "MEDIUM",  "Staff": "VERY_HIGH",  "Prisoners": "MEDIUM"
    }
  }
""".trimIndent()
