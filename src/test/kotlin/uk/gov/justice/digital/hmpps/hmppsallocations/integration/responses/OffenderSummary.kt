package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun offenderSummary() = """
  {
      "offenderId": 9999999999,
      "firstName": "Tester",
      "middleNames": [
          "hi",
          "hi"
      ],
      "surname": "TestSurname",
      "dateOfBirth": "2001-11-17",
      "gender": "Male",
      "otherIds": {
          "crn": "XXXXXXX"
      },
      "contactDetails": {},
      "offenderProfile": {
          "offenderLanguages": {},
          "previousConviction": {}
      },
      "softDeleted": false,
      "currentDisposal": "1",
      "partitionArea": "National Data",
      "currentRestriction": false,
      "currentExclusion": false,
      "activeProbationManagedSentence": true
  }
""".trimIndent()
