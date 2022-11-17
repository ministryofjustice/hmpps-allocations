package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun documentsResponse() = """
  [
    {
        "id": "efb7a4e8-3f4a-449c-bf6f-b1fc8def3410",
        "name": "cps.pdf",
        "relatedTo": {
            "type": "CPSPACK",
            "name": "SA2020 Suspended Sentence Order",
            "event": {
                "eventType": "CURRENT",
                "eventNumber": "1",
                "mainOffence": "Attempt/Common/Assault of an Emergency Worker   (Act 2018) 00873"
            },
            "description": "Crown Prosecution Service case pack"
        },
        "dateCreated": "2021-10-17T00:00:00+01:00",
        "sensitive": false
    },
    {
      "id": "6c50048a-c647-4598-8fae-0b84c69ef31a",
        "name": "doc.pdf",
        "relatedTo": {
            "type": "COURT_REPORT",
            "name": "Pre-Sentence Report - Fast",
            "event": {
                "eventType": "CURRENT",
                "eventNumber": "1",
                "mainOffence": "Attempt/Common/Assault of an Emergency Worker   (Act 2018) 00873"
            },
            "description": "Court Report"
        },
        "dateSaved": "2021-12-07T14:36:48.937372+01:00",
        "dateCreated": "2021-12-07T15:24:43+01:00",
        "sensitive": false
    },
    {
        "id": "626aa1d1-71c6-4b76-92a1-bf2f9250c143",
        "name": "Pre Cons.pdf",
        "relatedTo": {
            "type": "PRECONS",
            "name": "",
            "description": "PNC previous convictions"
        },
        "sensitive": false
    }
  ]
""".trimIndent()
