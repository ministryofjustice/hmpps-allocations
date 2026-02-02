package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessrisksneeds

fun riskPredictorResponse() = """
  [
    {
      "completedDate": "2025-10-23T03:02:59",
      "source": "OASYS",
      "status": "COMPLETE",
      "outputVersion": "1",
      "output": {
        "groupReconvictionScore": {
          "oneYear": 0,
          "twoYears": 85,
          "scoreLevel": "LOW"
        },
        "violencePredictorScore": {
          "ovpStaticWeightedScore": 0,
          "ovpDynamicWeightedScore": 0,
          "ovpTotalWeightedScore": 0,
          "oneYear": 0,
          "twoYears": 0,
          "ovpRisk": "LOW"
        },
        "generalPredictorScore": {
          "ogpStaticWeightedScore": 0,
          "ogpDynamicWeightedScore": 0,
          "ogpTotalWeightedScore": 0,
          "ogp1Year": 0,
          "ogp2Year": 0,
          "ogpRisk": "LOW"
        },
        "riskOfSeriousRecidivismScore": {
          "percentageScore": 3.8,
          "staticOrDynamic": "STATIC",
          "source": "OASYS",
          "algorithmVersion": "5",
          "scoreLevel": "MEDIUM"
        },
        "sexualPredictorScore": {
          "ospIndecentPercentageScore": 0,
          "ospContactPercentageScore": 0,
          "ospIndecentScoreLevel": "LOW",
          "ospContactScoreLevel": "LOW",
          "ospIndirectImagePercentageScore": 0,
          "ospDirectContactPercentageScore": 0,
          "ospIndirectImageScoreLevel": "LOW",
          "ospDirectContactScoreLevel": "LOW"
        }
      }
    }
  ]
""".trimIndent()
