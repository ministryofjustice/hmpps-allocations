package uk.gov.justice.digital.hmpps.hmppsallocations.mapper

private val gradeMap: Map<String, String> = mapOf("PSQ" to "PSO", "PSP" to "PQiP", "PSM" to "PO", "PSC" to "SPO")

fun deliusToStaffGrade(deliusCode: String?): String? = gradeMap[deliusCode]
