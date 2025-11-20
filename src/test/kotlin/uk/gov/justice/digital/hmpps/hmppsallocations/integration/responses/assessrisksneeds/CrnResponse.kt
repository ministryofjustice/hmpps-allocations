package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.assessrisksneeds

fun crnResponse() = """
{
  "crn": "J678910",
    "name": {
        "forename": "Dylan",
        "middleName": "Alan",
        "surname": "Armstrong",
        "combinedName": "Dylan Adam Armstrong"
    },
    "dateOfBirth": "1959-01-20",
    "manager": {
        "code": "N57A046",
        "name": {
            "forename": "Samantha",
            "middleName": "",
            "surname": "Bryant",
            "combinedName": "Samantha Bryant"
        },
        "teamCode": "N03S0D",
        "grade": "PO",
        "allocated": true
    },
    "hasActiveOrder": true
}
""".trimIndent()
