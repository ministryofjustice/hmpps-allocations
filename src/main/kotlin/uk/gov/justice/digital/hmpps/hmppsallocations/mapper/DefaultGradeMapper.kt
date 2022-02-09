package uk.gov.justice.digital.hmpps.hmppsallocations.mapper

import org.springframework.stereotype.Component

@Component
class DefaultGradeMapper : GradeMapper {
  private val gradeMap: Map<String, String> = mapOf("PSQ" to "PSO", "PSP" to "PQiP", "PSM" to "PO", "PSC" to "SPO", "TPO" to "PQiP")

  override fun deliusToStaffGrade(deliusCode: String?): String? {
    return gradeMap[deliusCode]
  }

  override fun workloadToStaffGrade(workloadGrade: String): String {
    return gradeMap.getOrDefault(workloadGrade, workloadGrade)
  }
}
