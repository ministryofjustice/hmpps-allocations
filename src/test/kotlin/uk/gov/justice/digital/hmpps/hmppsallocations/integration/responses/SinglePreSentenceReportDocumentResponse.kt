package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun singlePreSentenceReportDocumentResponse(convictionId: Long) = """
  {
      "documents": [],
      "convictions": [
          {
              "convictionId": "$convictionId",
              "documents": [
                  {
                      "id": "6c50048a-c647-4598-8fae-0b84c69ef31a",
                      "documentName": "doc.pdf",
                      "author": "Sally Socks",
                      "type": {
                          "code": "COURT_REPORT_DOCUMENT",
                          "description": "Court report"
                      },
                      "extendedDescription": "Pre-Sentence Report - Fast requested by Some Court on 03/04/2002",
                      "lastModifiedAt": "2021-12-07T15:24:43.777754",
                      "createdAt": "2021-12-07T15:24:43",
                      "parentPrimaryKeyId": 1234567890,
                      "subType": {
                          "code": "CJF",
                          "description": "Pre-Sentence Report - Fast"
                      },
                      "reportDocumentDates": {
                          "requestedDate": "2021-10-29",
                          "requiredDate": "2021-11-17",
                          "completedDate": "2021-12-03T00:00:00"
                      }
                  }
              ]
          }
      ]
  }
""".trimIndent()
