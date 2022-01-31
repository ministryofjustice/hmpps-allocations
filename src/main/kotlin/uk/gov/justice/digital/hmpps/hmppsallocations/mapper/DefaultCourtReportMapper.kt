package uk.gov.justice.digital.hmpps.hmppsallocations.mapper

import org.springframework.stereotype.Component

@Component
class DefaultCourtReportMapper : CourtReportMapper {

  private val courtReportTypes: Map<String, String> = mapOf("CJF" to "Fast", "CJO" to "Oral", "CJS" to "Standard", "PSA" to "Addendum")

  override fun deliusToReportType(reportTypeCode: String, reportTypeDescription: String): String {
    return courtReportTypes.getOrDefault(reportTypeCode, reportTypeDescription)
  }
}
