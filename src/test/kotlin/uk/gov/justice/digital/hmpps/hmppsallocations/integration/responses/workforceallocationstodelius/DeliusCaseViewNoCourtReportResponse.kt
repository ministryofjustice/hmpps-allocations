package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius

fun deliusCaseViewNoCourtReportResponse() = """
  {
    "name": {
      "forename": "Dylan",
      "middleName": "Adam",
      "surname": "Armstrong"
    },
    "dateOfBirth": "2001-11-17",
    "gender": "Male",
    "pncNumber": "9999/1234567A",
    "sentence": {
      "description": "Adult Custody < 12m",
      "startDate": "2023-01-04",
      "length": "12 Months",
      "endDate": "2024-01-03"
    },
    "offences": [
      {
        "mainCategory": "Abstracting electricity",
        "subCategory": "Abstracting electricity",
        "mainOffence": true
      }
    ],
    "requirements": [
      {
        "mainCategory": "Unpaid Work",
        "subCategory": "Regular",
        "length": "100 Hours"
      }
    ],
    "cpsPack": {
      "documentId": "efb7a4e8-3f4a-449c-bf6f-b1fc8def3410",
      "documentName": "cps.pdf",
      "dateCreated": "2021-10-16",
      "description": "Crown Prosecution Service case pack"
    },
    "preConvictionDocument": {
      "documentId": "626aa1d1-71c6-4b76-92a1-bf2f9250c143",
      "documentName": "Pre Cons.pdf",
      "description": "PNC previous convictions"
    },
    "age": 21
  }
""".trimIndent()
