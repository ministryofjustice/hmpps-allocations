package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonInclude
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
  @JsonInclude(JsonInclude.Include.NON_NULL)
  val initialAppointment: InitialAppointmentDetails?,
  @Schema(description = "Probation Status", example = "Currently managed")
  val status: String,
  @JsonInclude(JsonInclude.Include.NON_NULL)
  val offenderManager: OffenderManagerDetails?,
  @Schema(description = "Case Type")
  val caseType: String,
  @Schema(description = "Sentence Length")
  val sentenceLength: String?,
  val convictionNumber: Int,
) {
  companion object {
    fun from(case: UnallocatedCaseEntity, deliusCaseDetail: DeliusCaseDetail): UnallocatedCase {
      return UnallocatedCase(
        "${deliusCaseDetail.name.forename} ${deliusCaseDetail.name.surname}",
        deliusCaseDetail.crn,
        case.tier,
        deliusCaseDetail.sentence.date,
        InitialAppointmentDetails.from(
          deliusCaseDetail.initialAppointment,
        ),
        deliusCaseDetail.probationStatus.description,
        OffenderManagerDetails.from(
          deliusCaseDetail.communityPersonManager,
          deliusCaseDetail.probationStatus.description,
        ),
        deliusCaseDetail.type,
        deliusCaseDetail.sentence.length,
        case.convictionNumber,
      )
    }
  }
}
