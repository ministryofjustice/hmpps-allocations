package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun needsResponse() = """
  {
  "identifiedNeeds": [
    {
      "section": "DRUG_MISUSE",
      "name": "Drug misuse",
      "overThreshold": true,
      "riskOfHarm": false,
      "riskOfReoffending": false,
      "flaggedAsNeed": true,
      "severity": "true",
      "identifiedAsNeed": true,
      "needScore": 4
    }
  ],
  "notIdentifiedNeeds": [
    {
      "section": "DRUG_MISUSE",
      "name": "Drug misuse",
      "overThreshold": true,
      "riskOfHarm": false,
      "riskOfReoffending": false,
      "flaggedAsNeed": true,
      "severity": "true",
      "identifiedAsNeed": true,
      "needScore": 4
    }
  ],
  "unansweredNeeds": [
    {
      "section": "DRUG_MISUSE",
      "name": "Drug misuse",
      "overThreshold": true,
      "riskOfHarm": false,
      "riskOfReoffending": false,
      "flaggedAsNeed": true,
      "severity": "true",
      "identifiedAsNeed": true,
      "needScore": 4
    }
  ],
  "assessedOn": "2022-01-26T16:30:06.942Z"
}
""".trimIndent()
