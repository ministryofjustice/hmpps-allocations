package uk.gov.justice.digital.hmpps.hmppsallocations.mapper

import org.springframework.stereotype.Component

@Component
class DefaultGradeMapper : GradeMapper {
  private val gradeMap: Map<String, String> = mapOf("PSQ" to "PSO", "PSP" to "PQiP", "PSM" to "PO", "PSC" to "SPO")

  override fun deliusToStaffGrade(deliusCode: String?): String? {
    return gradeMap[deliusCode]
  }
}
