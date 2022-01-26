package uk.gov.justice.digital.hmpps.hmppsallocations.mapper

interface CourtReportMapper {

  fun deliusToReportType(reportTypeCode: String, reportTypeDescription: String): String
}
