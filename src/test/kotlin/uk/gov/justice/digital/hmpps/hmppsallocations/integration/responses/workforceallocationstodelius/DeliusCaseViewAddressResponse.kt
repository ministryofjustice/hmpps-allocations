package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses.workforceallocationstodelius

import uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain.CaseViewAddressIntegration

fun deliusCaseViewAddressResponse(caseViewAddressIntegration: CaseViewAddressIntegration?) = """
  {
    "name": {
      "forename": "Dylan",
      "middleName": "Adam",
      "surname": "Armstrong"
    },
    "dateOfBirth": "2001-11-17",
    "gender": "Male",
    "pncNumber": "9999/1234567A",
    ${address(caseViewAddressIntegration)}
    "sentence": {
      "description": "Adult Custody < 12m",
      "startDate": "2023-01-04",
      "length": "12 Months",
      "endDate": "2024-01-03"
    },
    "offences": [
      {
        "mainCategory": "Abstracting electricity",
        "subCategory": "Abstracting electricity",
        "mainOffence": true
      }
    ],
    "requirements": [],
    "age": 21
  }
""".trimIndent()

fun address(caseViewAddressIntegration: CaseViewAddressIntegration?): String = caseViewAddressIntegration?.let {
  """
      "mainAddress": {
      ${addressFieldOrEmpty("buildingName",caseViewAddressIntegration.buildingName)} 
      ${addressFieldOrEmpty("addressNumber",caseViewAddressIntegration.addressNumber)}
      ${addressFieldOrEmpty("streetName",caseViewAddressIntegration.streetName)}
      ${addressFieldOrEmpty("town",caseViewAddressIntegration.town)}
      ${addressFieldOrEmpty("county",caseViewAddressIntegration.county)}
      ${addressFieldOrEmpty("postcode",caseViewAddressIntegration.postcode)}
      "noFixedAbode": ${caseViewAddressIntegration.noFixedAbode},
      "typeVerified": ${caseViewAddressIntegration.typeVerified},
      "typeDescription": "${caseViewAddressIntegration.typeDescription}",
      "startDate": "${caseViewAddressIntegration.startDate}"
    },
  """.trimIndent()
} ?: ""

fun addressFieldOrEmpty(key: String, value: String?) = value?.let { "\"$key\":\"$value\"," } ?: ""
