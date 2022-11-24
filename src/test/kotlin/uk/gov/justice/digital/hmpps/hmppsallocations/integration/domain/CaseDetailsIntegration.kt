package uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain

import java.time.LocalDate

data class CaseDetailsIntegration(val crn: String, val eventNumber: String, val initialAppointment: LocalDate?, val probationStatusDescription: String)
