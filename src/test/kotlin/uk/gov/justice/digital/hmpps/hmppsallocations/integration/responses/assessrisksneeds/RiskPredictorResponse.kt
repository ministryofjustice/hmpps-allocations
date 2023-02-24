package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessrisksneeds

fun riskPredictorResponse() = """
  [
    {
      "rsrPercentageScore": 3.8,
      "rsrScoreLevel": "MEDIUM",
      "ospcPercentageScore": 0,
      "ospcScoreLevel": "LOW",
      "ospiPercentageScore": 0,
      "ospiScoreLevel": "LOW",
      "completedDate": "2019-02-12T16:09:10.271",
      "staticOrDynamic": "STATIC",
      "source": "OASYS",
      "status": "COMPLETE",
      "algorithmVersion": "string"
    }
  ]
""".trimIndent()
