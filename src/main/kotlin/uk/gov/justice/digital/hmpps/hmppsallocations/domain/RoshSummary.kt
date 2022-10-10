package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import java.time.LocalDate

data class RoshSummary(val overallRisk: String, val assessedOn: LocalDate, val riskInCommunity: Map<String, String?>)

