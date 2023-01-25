package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius

fun deliusProbationRecordSingleInactiveEventResponse(crn: String, convictionNumber: Int) = """
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
    "activeEvents": [],
    "inactiveEvents": [
      {
        "sentence": {
          "description": "Absolute/Conditional Discharge",
          "length": "0",
          "terminationDate": "2009-10-12",
          "startDate": "2009-04-12"
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
          "grade": "PQiP"
        }
      }
    ]
  }
""".trimIndent()
