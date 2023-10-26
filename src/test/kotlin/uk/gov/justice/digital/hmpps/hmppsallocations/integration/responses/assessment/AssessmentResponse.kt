package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessment

fun assessmentResponse() = """
  {
    "timeline": [
      {
        "assessmentPk": 123456789,
        "assessmentType": "LAYER_3",
        "status": "COMPLETE",
        "completedDate": "2014-03-28T13:47:08"
      },
      {
        "assessmentPk": 987654321,
        "assessmentType": "LAYER_3",
        "status": "COMPLETE",
        "completedDate": "2012-10-04T00:00:00"
      }
    ]
  }
""".trimIndent()
