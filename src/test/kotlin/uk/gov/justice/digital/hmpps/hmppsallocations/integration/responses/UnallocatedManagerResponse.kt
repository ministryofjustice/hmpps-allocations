package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun unallocatedOffenderManagerResponse() = """
[
  {
    "fromDate": "2019-12-04",
    "grade": {
      "code": "PSQ",
      "description": "Some description"
    },
    "isPrisonOffenderManager": true,
    "isResponsibleOfficer": true,
    "isUnallocated": true,
    "probationArea": {
      "code": "N01",
      "description": "NPS North West",
      "institution": {
        "code": "string",
        "description": "string",
        "establishmentType": {
          "code": "ABC123",
          "description": "Some description"
        },
        "institutionId": 0,
        "institutionName": "string",
        "isEstablishment": true,
        "isPrivate": true,
        "nomsPrisonInstitutionCode": "string"
      },
      "nps": true,
      "organisation": {
        "code": "ABC123",
        "description": "Some description"
      },
      "probationAreaId": 0,
      "teams": [
        {
          "borough": {
            "code": "ABC123",
            "description": "Some description"
          },
          "code": "string",
          "description": "string",
          "district": {
            "code": "ABC123",
            "description": "Some description"
          },
          "externalProvider": {
            "code": "ABC123",
            "description": "Some description"
          },
          "isPrivate": true,
          "localDeliveryUnit": {
            "code": "ABC123",
            "description": "Some description"
          },
          "name": "string",
          "providerTeamId": 0,
          "scProvider": {
            "code": "ABC123",
            "description": "Some description"
          },
          "teamId": 0
        }
      ]
    },
    "staff": {
      "email": "officer@gov.uk",
      "forenames": "UNALLOCATED",
      "phoneNumber": "0123411278",
      "surname": "STAFF"
    },
    "staffCode": "STFFCDEU",
    "staffId": 123455,
    "team": {
      "borough": {
        "code": "ABC123",
        "description": "Some description"
      },
      "code": "C01T04",
      "description": "OMU A",
      "district": {
        "code": "ABC123",
        "description": "Some description"
      },
      "emailAddress": "first.last@digital.justice.gov.uk",
      "endDate": "2021-12-17",
      "localDeliveryUnit": {
        "code": "ABC123",
        "description": "Some description"
      },
      "startDate": "2021-12-17",
      "teamType": {
        "code": "ABC123",
        "description": "Some description"
      },
      "telephone": "OMU A"
    }
  }
]


"""
