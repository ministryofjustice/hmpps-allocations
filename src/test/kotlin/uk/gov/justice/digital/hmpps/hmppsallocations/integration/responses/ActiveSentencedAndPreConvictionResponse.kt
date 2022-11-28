package uk.gov.justice.digital.hmpps.hmppsallocations.integration.responses

fun activeSentencedAndPreConvictionResponse() = """
[
    {
        "convictionId": 123456789,
        "index": "1",
        "active": true,
        "inBreach": false,
        "failureToComplyCount": 3,
        "awaitingPsr": false,
        "convictionDate": "2019-11-17",
        "referralDate": "2019-11-17",
        "offences": [
            {
                "offenceId": "M2500292515",
                "mainOffence": true,
                "detail": {
                    "code": "04300",
                    "description": "Abstracting electricity - 04300",
                    "mainCategoryCode": "043",
                    "mainCategoryDescription": "Abstracting electricity",
                    "mainCategoryAbbreviation": "Abstracting electricity",
                    "ogrsOffenceCategory": "Theft (Non-motor)",
                    "subCategoryCode": "00",
                    "subCategoryDescription": "Abstracting electricity",
                    "form20Code": "7"
                },
                "offenceDate": "2019-11-17T00:00:00",
                "offenceCount": 1,
                "offenderId": 2500342345,
                "createdDatetime": "2019-11-17T20:50:18",
                "lastUpdatedDatetime": "2019-11-17T20:50:25"
            }
        ],
        "sentence": {
            "sentenceId": 2500278160,
            "description": "Adult Custody < 12m",
            "originalLength": 6,
            "originalLengthUnits": "Months",
            "defaultLength": 6,
            "lengthInDays": 181,
            "expectedSentenceEndDate": "2020-05-16",
            "startDate": "2019-11-17",
            "sentenceType": {
                "code": "SC",
                "description": "Adult Custody < 12m"
            },
            "failureToComplyLimit": 3,
            "cja2003Order": true,
            "legacyOrder": false
        },
        "latestCourtAppearanceOutcome": {
            "code": "307",
            "description": "Adult Custody < 12m"
        },
        "custody": {
            "institution": {
                "institutionId": 157,
                "isEstablishment": true,
                "code": "UNKNOW",
                "description": "Unknown",
                "institutionName": "Unknown",
                "establishmentType": {
                    "code": "E",
                    "description": "Prison"
                }
            },
            "keyDates": {},
            "status": {
                "code": "A",
                "description": "Sentenced - In Custody"
            },
            "sentenceStartDate": "2019-11-17"
        },
        "responsibleCourt": {
            "courtId": 1500004175,
            "code": "STHWCC",
            "selectable": true,
            "courtName": "Southwark Crown Court",
            "telephoneNumber": "020 7522 7200",
            "fax": "020 7522 7300",
            "buildingName": "1 English Grounds",
            "street": "(off Battlebridge Lane)",
            "town": "Southwark",
            "county": "london",
            "postcode": "SE1 2HU",
            "country": "England",
            "courtTypeId": 294,
            "createdDatetime": "2014-05-29T21:50:16",
            "lastUpdatedDatetime": "2017-08-29T12:32:43",
            "probationAreaId": 1500001006,
            "probationArea": {
                "code": "N07",
                "description": "NPS London"
            },
            "courtType": {
                "code": "CRN",
                "description": "Crown Court"
            }
        },
        "courtAppearance": {
            "courtAppearanceId": 2500311899,
            "appearanceDate": "2019-11-17T00:00:00",
            "courtCode": "STHWCC",
            "courtName": "Southwark Crown Court",
            "appearanceType": {
                "code": "S",
                "description": "Sentence"
            },
            "crn": "X320741"
        },
        "orderManagers": [
            {
                "probationAreaId": 123456789,
                "teamId": 123456789,
                "officerId": 123456789,
                "name": "A Unallocated Staff Name",
                "staffCode": "STFFCDEU",
                "dateStartOfAllocation": "2014-05-29T20:50:18",
                "teamCode": "TM1",
                "probationAreaCode": "PAC1"
            }
        ]
    },
    {
        "convictionId": 5654763,
        "index": "1",
        "active": true,
        "inBreach": false,
        "failureToComplyCount": 3,
        "awaitingPsr": false,
        "referralDate": "2019-11-17",
        "offences": [
            {
                "offenceId": "M2500292515",
                "mainOffence": true,
                "detail": {
                    "code": "04300",
                    "description": "Abstracting electricity - 04300",
                    "mainCategoryCode": "043",
                    "mainCategoryDescription": "Abstracting electricity",
                    "mainCategoryAbbreviation": "Abstracting electricity",
                    "ogrsOffenceCategory": "Theft (Non-motor)",
                    "subCategoryCode": "00",
                    "subCategoryDescription": "Abstracting electricity",
                    "form20Code": "7"
                },
                "offenceDate": "2019-11-17T00:00:00",
                "offenceCount": 1,
                "offenderId": 2500342345,
                "createdDatetime": "2019-11-17T20:50:18",
                "lastUpdatedDatetime": "2019-11-17T20:50:25"
            }
        ],
        "latestCourtAppearanceOutcome": {
            "code": "104",
            "description": "Adjourned Other Reason"
        },
        "courtAppearance": {
            "courtAppearanceId": 2500311899,
            "appearanceDate": "2019-11-17T00:00:00",
            "courtCode": "STHWCC",
            "courtName": "Southwark Crown Court",
            "appearanceType": {
                "code": "T",
                "description": "Trial/Adjournment"
            },
            "crn": "X320741"
        },
        "orderManagers": [
            {
                "probationAreaId": 10004172849,
                "teamId": 123456789,
                "officerId": 123456789,
                "name": "A Unallocated Staff Name",
                "staffCode": "OTHERSTFFCDEU",
                "dateStartOfAllocation": "2014-05-29T20:50:18",
                "teamCode": "TM1",
                "probationAreaCode": "PAC1"
            }
        ]
    }
]
""".trimIndent()
