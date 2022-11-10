package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.client.DeliusCaseDetail
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
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val sentenceDate: LocalDate,
  @Schema(description = "Initial Appointment Date", example = "2020-03-21")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val initialAppointment: LocalDate?,
  @Schema(description = "Probation Status", example = "Currently managed")
  val status: String,
  @Schema(description = "Previous Conviction End Date", example = "2021-03-25")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val previousConvictionEndDate: LocalDate?,
  val offenderManager: OffenderManagerDetails,
  @Schema(description = "Conviction Id")
  val convictionId: Long,
  @Schema(description = "Case Type")
  val caseType: CaseTypes,
  @Schema(description = "Sentence Length")
  val sentenceLength: String?
) {
  companion object {
    fun from(case: UnallocatedCaseEntity, deliusCaseDetail: DeliusCaseDetail?): UnallocatedCase {
      return UnallocatedCase(
        "${deliusCaseDetail!!.name.forename} ${deliusCaseDetail.name.surname}",
        deliusCaseDetail.crn, case.tier, deliusCaseDetail.sentence.date, deliusCaseDetail.initialAppointment?.date, case.status,
        case.previousConvictionDate,
        OffenderManagerDetails(
          case.offenderManagerForename,
          case.offenderManagerSurname,
          case.offenderManagerGrade
        ),
        case.convictionId,
        case.caseType,
        deliusCaseDetail.sentence.length
      )
    }

    fun from(case: UnallocatedCaseEntity): UnallocatedCase {
      return UnallocatedCase(
        case.name,
        case.crn, case.tier, case.sentenceDate, case.initialAppointment, case.status,
        case.previousConvictionDate,
        OffenderManagerDetails(
          case.offenderManagerForename,
          case.offenderManagerSurname,
          case.offenderManagerGrade
        ),
        case.convictionId,
        case.caseType,
        case.sentenceLength
      )
    }
  }
}
