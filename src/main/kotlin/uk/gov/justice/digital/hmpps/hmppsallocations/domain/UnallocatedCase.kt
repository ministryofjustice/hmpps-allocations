package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate
import java.time.LocalDateTime

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
  val age: Int?,
  val offences: List<UnallocatedCaseOffence>?,
  @Schema(description = "Expected Sentence Date", example = "2020-05-16")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val expectedSentenceEndDate: LocalDate?,
  @Schema(description = "Sentence description", example = "SA2020 Suspended Sentence Order")
  val sentenceDescription: String?,
  val requirements: List<UnallocatedCaseRequirement>?,
  @Schema(description = "PNC Number")
  val pncNumber: String?,
  val courtReport: UnallocatedCaseCourtReport?,
  val assessment: UnallocatedAssessment?,
  @Schema(description = "Conviction Id")
  val convictionId: Long
) {

  companion object {
    fun from(case: UnallocatedCaseEntity): UnallocatedCase {
      return from(case, null, null, null, null, null, null, null, null)
    }

    fun from(
      case: UnallocatedCaseEntity,
      offenderSummary: OffenderSummary?,
      age: Int?,
      offences: List<Offence>?,
      expectedSentenceEndDate: LocalDate?,
      sentenceDescription: String?,
      requirements: List<ConvictionRequirement>?,
      courtReport: CourtReport?,
      assessment: Assessment?,
    ): UnallocatedCase {
      return UnallocatedCase(
        case.name,
        case.crn, case.tier, case.sentenceDate, case.initialAppointment, case.status,
        case.previousConvictionDate,
        OffenderManagerDetails(
          case.offenderManagerForename,
          case.offenderManagerSurname,
          case.offenderManagerGrade
        ),
        offenderSummary?.gender,
        offenderSummary?.dateOfBirth,
        age,
        offences?.map { UnallocatedCaseOffence.from(it) },
        expectedSentenceEndDate,
        sentenceDescription,
        requirements?.map { UnallocatedCaseRequirement.from(it) },
        offenderSummary?.otherIds?.pncNumber,
        UnallocatedCaseCourtReport.from(courtReport),
        UnallocatedAssessment.from(assessment),
        case.convictionId
      )
    }
  }
}

data class OffenderManagerDetails @JsonCreator constructor(
  @Schema(description = "Forenames", example = "John William")
  val forenames: String?,
  @Schema(description = "Surname", example = "Smith")
  val surname: String?,
  @Schema(description = "Grade", example = "PSO")
  val grade: String?
)

data class UnallocatedCaseOffence @JsonCreator constructor(
  @Schema(description = "Main Offence", example = "True")
  val mainOffence: Boolean,
  @Schema(description = "Main Category", example = "Abstracting electricity")
  val mainCategory: String,
  @Schema(description = "Sub Category", example = "Abstracting electricity")
  val subCategory: String
) {
  companion object {
    fun from(offence: Offence): UnallocatedCaseOffence {
      return UnallocatedCaseOffence(
        offence.mainOffence,
        offence.detail.mainCategoryDescription,
        offence.detail.subCategoryDescription
      )
    }
  }
}

data class UnallocatedCaseRequirement @JsonCreator constructor(
  @Schema(description = "Main Category", example = "Unpaid Work")
  val mainCategory: String,
  @Schema(description = "Sub Category", example = "Regular")
  val subCategory: String,
  @Schema(description = "Length", example = "100")
  val length: Long,
  @Schema(description = "Length Unit", example = "Hours")
  val lengthUnit: String,
) {
  companion object {
    fun from(requirement: ConvictionRequirement): UnallocatedCaseRequirement {
      return UnallocatedCaseRequirement(
        requirement.requirementTypeMainCategory.description,
        requirement.requirementTypeSubCategory.description,
        requirement.length,
        requirement.lengthUnit
      )
    }
  }
}

data class UnallocatedCaseCourtReport @JsonCreator constructor(
  @Schema(description = "code", example = "CJF")
  val code: String,
  @Schema(description = "Description", example = "Fast")
  val description: String,
  @Schema(description = "Completed Date", example = "2019-11-11")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val completedDate: LocalDateTime?
) {
  companion object {
    fun from(courtReport: CourtReport?): UnallocatedCaseCourtReport? {
      return courtReport?.let {
        UnallocatedCaseCourtReport(
          it.courtReportType.code,
          it.courtReportType.description,
          it.completedDate
        )
      }
    }
  }
}

data class UnallocatedAssessment @JsonCreator constructor(
  @Schema(description = "Completed Date", example = "2019-11-11")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = STRING)
  val lastAssessedOn: LocalDateTime,
  val type: String,
) {
  companion object {
    fun from(assessment: Assessment?): UnallocatedAssessment? {
      return assessment?.let {
        UnallocatedAssessment(assessment.completed, assessment.assessmentType)
      }
    }
  }
}
