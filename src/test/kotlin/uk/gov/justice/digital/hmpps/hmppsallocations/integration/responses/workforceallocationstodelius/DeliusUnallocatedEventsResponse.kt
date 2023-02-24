package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius

fun deliusUnallocatedEventsResponse() = """
  {
    "crn":"J678910",
    "name":{
      "forename":"Tester",
      "middleName":"",
      "surname":"TestSurname"
    },
    "activeEvents":[
      {
        "eventNumber":"1",
        "teamCode":"TM1",
        "providerCode":"PAC1"
      }
    ]
  }
""".trimIndent()
