package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun documentsResponse(convictionId: Long) = """
  {
      "documents": [
      {
            "id": "626aa1d1-71c6-4b76-92a1-bf2f9250c143",
            "documentName": "Pre Cons.pdf",
            "author": "Sally Socks",
            "type": {
                "code": "PRECONS_DOCUMENT",
                "description": "PNC previous convictions"
            },
            "extendedDescription": "Previous convictions as of 27/07/2021",
            "createdAt": "2021-11-17T00:00:00"
        }
      ],
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
                  },
                  {
                    "id": "efb7a4e8-3f4a-449c-bf6f-b1fc8def3410",
                    "documentName": "cps.pdf",
                    "author": "Sally Socks",
                    "type": {
                        "code": "CPSPACK_DOCUMENT",
                        "description": "Crown Prosecution Service case pack"
                    },
                    "extendedDescription": "Crown Prosecution Service case pack for 29/10/2021",
                    "createdAt": "2021-10-17T00:00:00"
                }
              ]
          }
      ]
  }
""".trimIndent()
