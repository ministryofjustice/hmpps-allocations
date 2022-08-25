package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun offenderDetailsNoFixedAbodeResponse() = """
  {
    "title": "Mr",
    "offenderId": 9999999999,
      "firstName": "Tester",
      "middleNames": [
          "hi",
          "hi"
      ],
      "surname": "TestSurname",
      "dateOfBirth": "2001-11-17",
      "gender": "Male",
    "otherIds": {
        "crn": "E499787",
          "pncNumber": "9999/1234567A"
    },
    "contactDetails": {
        "addresses": [
            {
                "from": "2022-08-22",
                "noFixedAbode": true,
                "postcode": "NF1 1NF",
                "status": {
                    "code": "M",
                    "description": "Main"
                },
                "typeVerified": false,
                "createdDatetime": "2022-08-25T16:20:09",
                "lastUpdatedDatetime": "2022-08-25T16:20:09"
            }
        ]
    },
    "offenderProfile": {
        "offenderLanguages": {},
        "offenderDetails": "Comment added by Peter Robinson on 26/07/2022 at 10:21\nTitus has another mobile number that he can be contacted on [mobile number] and has recently moved from Salt's Mill, Saltaire to Wrexham",
        "previousConviction": {},
        "riskColour": "Red"
    },
    "offenderManagers": [
        {
            "trustOfficer": {
                "forenames": "Staff",
                "surname": "Unallocated"
            },
            "staff": {
                "code": "N03F01U",
                "forenames": "Staff",
                "surname": "Unallocated",
                "unallocated": true
            },
            "partitionArea": "National Data",
            "softDeleted": false,
            "team": {
                "code": "N03F01",
                "description": "Wrexham - Team 1",
                "localDeliveryUnit": {
                    "code": "WPTNWS",
                    "description": "North Wales"
                },
                "district": {
                    "code": "WPTNWS",
                    "description": "North Wales"
                },
                "borough": {
                    "code": "WPTNWS",
                    "description": "North Wales"
                }
            },
            "probationArea": {
                "code": "N03",
                "description": "Wales",
                "nps": true
            },
            "fromDate": "2022-08-10",
            "active": true,
            "allocationReason": {
                "code": "IN1",
                "description": "Initial Allocation"
            }
        }
    ],
    "softDeleted": false,
    "currentDisposal": "1",
    "partitionArea": "National Data",
    "currentRestriction": false,
    "restrictionMessage": "This is a restricted offender record. Please contact a system administrator",
    "currentExclusion": false,
    "exclusionMessage": "You are excluded from viewing this offender record. Please contact a system administrator",
    "currentTier": "A_2",
    "activeProbationManagedSentence": true
  }
""".trimIndent()
