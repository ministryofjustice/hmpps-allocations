package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun assessmentResponse() = """
  [
    {
        "assessmentId": 123456789,
        "refAssessmentVersionCode": "LAYER3",
        "refAssessmentVersionNumber": "1",
        "refAssessmentId": 4,
        "assessmentType": "LAYER_3",
        "assessmentStatus": "COMPLETE",
        "historicStatus": "HISTORIC",
        "refAssessmentOasysScoringAlgorithmVersion": 3,
        "assessorName": "Sally Smith",
        "created": "2014-03-26T16:54:57",
        "completed": "2014-03-28T13:47:08"
    },
    {
        "assessmentId": 987654321,
        "refAssessmentVersionCode": "LAYER3",
        "refAssessmentVersionNumber": "1",
        "refAssessmentId": 4,
        "assessmentType": "LAYER_3",
        "assessmentStatus": "COMPLETE",
        "historicStatus": "HISTORIC",
        "refAssessmentOasysScoringAlgorithmVersion": 3,
        "assessorName": "Sally Smith",
        "created": "2013-02-25T23:44:31",
        "completed": "2012-10-04T00:00:00"
    }
  ]
""".trimIndent()
