package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius

fun deliusProbationRecordSingleActiveEventResponse(crn: String, convictionNumber: Int) = """
  {
    "crn": "$crn",
    "name": {
      "forename": "Dylan",
      "middleName": "Adam",
      "surname": "Armstrong"
    },
    "event": {
      "number": "$convictionNumber",
      "manager": {
        "code": "STAFF1",
        "name": {
          "forename": "John",
          "middleName": "Jacob",
          "surname": "Smith"
        },
        "teamCode": "TM1",
        "grade": "PO"
      }
    },
    "activeEvents": [
    {
        "sentence": {
          "description": "Adult Custody < 12m",
          "length": "6 Months",
          "startDate": "2021-11-22"
        },
        "offences": [
          {
            "description": "Abstracting electricity - 04300",
            "main": true
          }
        ],
        "manager": {
          "code": "STAFF1",
          "name": {
            "forename": "A",
            "middleName": "Staff",
            "surname": "Name"
          },
          "email": "email@email.com",
          "grade": "PSO"
        }
      }
    ],
    "inactiveEvents": []
  }
""".trimIndent()
