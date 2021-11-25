package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun twoActiveInductionResponse() = """
[
    {
        "contactId": 9999999999,
        "contactStart": "2021-11-30T13:00:00+01:00",
        "contactEnd": "2021-11-30T13:30:00+01:00",
        "type": {
            "code": "COAI",
            "description": "Initial Appointment - In office (NS)",
            "appointment": true,
            "nationalStandard": true,
            "categories": [
                {
                    "code": "OM",
                    "description": "Offender Management"
                },
                {
                    "code": "NS",
                    "description": "National Standards/Compliance"
                },
                {
                    "code": "ET",
                    "description": "Education, Training, Employment"
                },
                {
                    "code": "AL",
                    "description": "All/Always"
                }
            ],
            "systemGenerated": false
        },
        "officeLocation": {
            "code": "ANOFFICECODE",
            "description": "An Office",
            "buildingName": "A House",
            "buildingNumber": "62",
            "streetName": "Some Street",
            "townCity": "A Town",
            "county": "A County",
            "postcode": "AB12 3CD"
        },
        "notes": "Some Notes",
        "provider": {
            "code": "REGIONCODE",
            "description": "NPS Region"
        },
        "team": {
            "code": "TEAMCODE",
            "description": "Team"
        },
        "staff": {
            "code": "STAFFCODE",
            "forenames": "John",
            "surname": "Doe",
            "unallocated": false
        },
        "sensitive": false,
        "outcome": {
            "code": "ATTC",
            "description": "Attended - Complied",
            "attended": true,
            "complied": true,
            "hoursCredited": 0.5
        },
        "rarActivity": false,
        "lastUpdatedDateTime": "2021-11-17T15:22:47+01:00",
        "lastUpdatedByUser": {
            "forenames": "John",
            "surname": "Doe"
        }
    },
    {
        "contactId": 9999999999,
        "contactStart": "2021-10-30T13:00:00+01:00",
        "contactEnd": "2021-11-30T13:30:00+01:00",
        "type": {
            "code": "COAI",
            "description": "Initial Appointment - In office (NS)",
            "appointment": true,
            "nationalStandard": true,
            "categories": [
                {
                    "code": "OM",
                    "description": "Offender Management"
                },
                {
                    "code": "NS",
                    "description": "National Standards/Compliance"
                },
                {
                    "code": "ET",
                    "description": "Education, Training, Employment"
                },
                {
                    "code": "AL",
                    "description": "All/Always"
                }
            ],
            "systemGenerated": false
        },
        "officeLocation": {
            "code": "ANOFFICECODE",
            "description": "An Office",
            "buildingName": "A House",
            "buildingNumber": "62",
            "streetName": "Some Street",
            "townCity": "A Town",
            "county": "A County",
            "postcode": "AB12 3CD"
        },
        "notes": "Some Notes",
        "provider": {
            "code": "REGIONCODE",
            "description": "NPS Region"
        },
        "team": {
            "code": "TEAMCODE",
            "description": "Team"
        },
        "staff": {
            "code": "STAFFCODE",
            "forenames": "John",
            "surname": "Doe",
            "unallocated": false
        },
        "sensitive": false,
        "outcome": {
            "code": "ATTC",
            "description": "Attended - Complied",
            "attended": true,
            "complied": true,
            "hoursCredited": 0.5
        },
        "rarActivity": false,
        "lastUpdatedDateTime": "2021-11-17T15:22:47+01:00",
        "lastUpdatedByUser": {
            "forenames": "John",
            "surname": "Doe"
        }
    }
]
""".trimIndent()
