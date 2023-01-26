package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius

fun deliusProbationRecordNoEventsResponse(crn: String, convictionNumber: Int) = """
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
    "inactiveEvents": []
  }
""".trimIndent()
