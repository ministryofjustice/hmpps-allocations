package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate

data class UnallocatedCase @JsonCreator constructor(

  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D2")
  val tier: String,
  @Schema(description = "Sentence Date", example = "2020-01-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val sentenceDate: LocalDate,
  @Schema(description = "Initial Appointment Date", example = "2020-03-21")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val initialAppointment: LocalDate?,
  @Schema(description = "Probation Status", example = "Currently managed")
  val status: String,
  @Schema(description = "Previous Conviction End Date", example = "2021-03-25")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val previousConvictionEndDate: LocalDate?,
  val offenderManager: OffenderManagerDetails,
  @Schema(description = "Gender", example = "Male")
  val gender: String?,
  @Schema(description = "Date Of Birth", example = "2021-06-19")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val dateOfBirth: LocalDate?,
  @Schema(description = "Age", example = "34")
  val age: Int?

) {

  companion object {
    fun from(case: UnallocatedCaseEntity): UnallocatedCase {
      return from(case, null, null, null)
    }

    fun from(case: UnallocatedCaseEntity, gender: String?, dateOfBirth: LocalDate?, age: Int?): UnallocatedCase {
      return UnallocatedCase(
        case.name,
        case.crn, case.tier, case.sentenceDate, case.initialAppointment, case.status,
        case.previousConvictionDate,
        OffenderManagerDetails(
          case.offenderManagerForename,
          case.offenderManagerSurname,
          case.offenderManagerGrade
        ),
        gender,
        dateOfBirth,
        age
      )
    }
  }
}

data class OffenderManagerDetails @JsonCreator constructor(
  @Schema(description = "Forenames", example = "John William")
  val forenames: String?,
  @Schema(description = "Surname", example = "Doe")
  val surname: String?,
  @Schema(description = "Grade", example = "PSO")
  val grade: String?
)
