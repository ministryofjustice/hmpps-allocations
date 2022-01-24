package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun singleCourtReportResponse() = """
  [
      {
          "courtReportId": 1234567890,
          "offenderId": 1234567890,
          "requestedDate": "2019-11-05T00:00:00",
          "requiredDate": "2019-11-14T00:00:00",
          "allocationDate": "2019-11-07T00:00:00",
          "completedDate": "2019-11-11T00:00:00",
          "sentToCourtDate": "2019-11-11T00:00:00",
          "receivedByCourtDate": "2019-11-11T00:00:00",
          "courtReportType": {
              "code": "CJF",
              "description": "Pre-Sentence Report - Fast"
          },
          "reportManagers": [
              {
                  "staff": {
                      "code": "STAFFCODE",
                      "forenames": "John",
                      "surname": "Smith",
                      "unallocated": false
                  },
                  "active": true
              },
              {
                  "staff": {
                      "code": "STAFFCODE",
                      "forenames": "IN",
                      "surname": "House",
                      "unallocated": false
                  },
                  "active": false
              }
          ]
      }
  ]
""".trimIndent()
