package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessrisksneeds

fun riskPredictorResponseBlankLevel() = """
  [
    {
      "rsrPercentageScore": null,
      "rsrScoreLevel": " ",
      "ospcPercentageScore": 0,
      "ospcScoreLevel": " ",
      "ospiPercentageScore": 0,
      "ospiScoreLevel": " ",
      "completedDate": "2013-03-12T16:09:10.271",
      "staticOrDynamic": "STATIC",
      "source": "OASYS",
      "status": "COMPLETE",
      "algorithmVersion": "string"
    }
  ]
""".trimIndent()
