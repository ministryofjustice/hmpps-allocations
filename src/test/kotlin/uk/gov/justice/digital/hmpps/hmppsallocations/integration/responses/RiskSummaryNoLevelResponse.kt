package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun riskSummaryNoLevelResponse() = """
  {
    "whoIsAtRisk": "X, Y and Z are at risk",
    "natureOfRisk": "The nature of the risk is X",
    "riskImminence": "the risk is imminent and more probably in X situation",
    "riskIncreaseFactors": "If offender in situation X the risk can be higher",
    "riskMitigationFactors": "Giving offender therapy in X will reduce the risk",
    "riskInCommunity": {
      "HIGH ": [
        "Children",
        "Public",
        "Know adult"
      ],
      "MEDIUM": [
        "Staff"
      ],
      "LOW": [
        "Prisoners"
      ]
    },
    "riskInCustody": {
      "HIGH ": [
        "Know adult"
      ],
      "VERY_HIGH": [
        "Staff",
        "Prisoners"
      ],
      "LOW": [
        "Children",
        "Public"
      ]
    },
    "assessedOn": "2022-02-02T14:37:21.419"
  }
""".trimIndent()
