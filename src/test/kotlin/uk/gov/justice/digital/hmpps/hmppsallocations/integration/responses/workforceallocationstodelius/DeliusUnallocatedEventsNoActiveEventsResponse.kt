package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius

fun deliusUnallocatedEventsNoActiveEventsResponse() = """
  {
    "crn":"J678910",
    "name":{
      "forename":"Tester",
      "middleName":"",
      "surname":"TestSurname"
    },
    "activeEvents":[]
  }
""".trimIndent()
