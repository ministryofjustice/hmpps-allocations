package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun multipleRegistrationResponse() = """
  {
    "registrations": [
        {
            "registrationId": 1234567890,
            "offenderId": 1234567890,
            "register": {
                "code": "5",
                "description": "Public Protection"
            },
            "type": {
                "code": "ALT11",
                "description": "ALT Under MAPPA Arrangements"
            },
            "riskColour": "Green",
            "startDate": "2021-08-30",
            "reviewPeriodMonths": 0,
            "registeringTeam": {
                "code": "TEAMCODE",
                "description": "Team Description"
            },
            "registeringOfficer": {
                "code": "OMCODE",
                "forenames": "Sarah",
                "surname": "Smith",
                "unallocated": false
            },
            "registeringProbationArea": {
                "code": "REGIONCODE",
                "description": "Region Description"
            },
            "warnUser": false,
            "active": true,
            "numberOfPreviousDeregistrations": 0
        },
        {
            "registrationId": 1504755911,
            "offenderId": 1234567890,
            "register": {
                "code": "3",
                "description": "Safeguarding"
            },
            "type": {
                "code": "ALT7",
                "description": "ALT Suicide/Self harm"
            },
            "riskColour": "Green",
            "startDate": "2021-08-30",
            "reviewPeriodMonths": 0,
            "registeringTeam": {
                "code": "TEAMCODE",
                "description": "Team Description"
            },
            "registeringOfficer": {
                "code": "OMCODE",
                "forenames": "Sarah",
                "surname": "Smith",
                "unallocated": false
            },
            "registeringProbationArea": {
                "code": "REGIONCODE",
                "description": "Region Description"
            },
            "warnUser": false,
            "active": true,
            "numberOfPreviousDeregistrations": 0
        },
        {
            "registrationId": 1504577237,
            "offenderId": 1234567890,
            "register": {
                "code": "3",
                "description": "Safeguarding"
            },
            "type": {
                "code": "RCPR",
                "description": "Child Protection"
            },
            "riskColour": "Red",
            "startDate": "2021-05-20",
            "reviewPeriodMonths": 3,
            "notes": "Some Notes.",
            "registeringTeam": {
                "code": "TEAMCODE",
                "description": "Team Description"
            },
            "registeringOfficer": {
                "code": "OMCODE",
                "forenames": "Sarah",
                "surname": "Smith",
                "unallocated": false
            },
            "registeringProbationArea": {
                "code": "REGIONCODE",
                "description": "Region Description"
            },
            "warnUser": false,
            "active": false,
            "endDate": "2021-08-30",
            "deregisteringTeam": {
                "code": "TEAMCODE",
                "description": "Team Description"
            },
            "deregisteringOfficer": {
                "code": "OMCODE",
                "forenames": "Sarah",
                "surname": "Smith",
                "unallocated": false
            },
            "deregisteringProbationArea": {
                "code": "REGIONCODE",
                "description": "Region Description"
            },
            "numberOfPreviousDeregistrations": 1
        }
    ]
}
""".trimIndent()
