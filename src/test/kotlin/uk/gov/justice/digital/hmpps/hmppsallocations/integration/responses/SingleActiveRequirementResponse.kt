package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun singleActiveRequirementResponse() = """
  {
      "requirements": [
          {
              "requirementId": 1235678945,
              "commencementDate": "2019-11-21",
              "startDate": "2019-11-21",
              "expectedStartDate": "2019-11-21",
              "createdDatetime": "2019-11-21T12:26:56",
              "active": true,
              "requirementTypeSubCategory": {
                  "code": "W01",
                  "description": "Regular"
              },
              "requirementTypeMainCategory": {
                  "code": "W",
                  "description": "Unpaid Work"
              },
              "length": 100,
              "lengthUnit": "Hours",
              "restrictive": false,
              "softDeleted": false
          }
      ]
  }
""".trimIndent()
