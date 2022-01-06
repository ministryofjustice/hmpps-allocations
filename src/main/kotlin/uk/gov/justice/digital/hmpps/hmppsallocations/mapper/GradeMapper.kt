package uk.gov.justice.digital.hmpps.hmppsallocations.mapper

interface GradeMapper {

  fun deliusToStaffGrade(deliusCode: String?): String?
}
