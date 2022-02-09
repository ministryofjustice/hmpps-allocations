package uk.gov.justice.digital.hmpps.hmppsallocations.mapper

interface GradeMapper {

  fun deliusToStaffGrade(deliusCode: String?): String?
  fun workloadToStaffGrade(workloadGrade: String): String
}
