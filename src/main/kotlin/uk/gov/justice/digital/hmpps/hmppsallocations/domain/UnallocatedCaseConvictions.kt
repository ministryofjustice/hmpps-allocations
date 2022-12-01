package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity
import java.time.LocalDate

data class UnallocatedCaseConvictions @JsonCreator constructor(
  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D2")
  val tier: String,
  val active: List<UnallocatedCaseConviction>,
  val previous: List<UnallocatedCaseConviction>,
  val convictionId: Long,
  val convictionNumber: Int
) {
  companion object {
    fun from(
      case: UnallocatedCaseEntity,
      active: List<UnallocatedCaseConviction>,
      previous: List<UnallocatedCaseConviction>
    ): UnallocatedCaseConvictions {
      return UnallocatedCaseConvictions(
        case.name, case.crn, case.tier,
        active,
        previous,
        case.convictionId,
        case.convictionNumber
      )
    }
  }
}

data class UnallocatedCaseConviction @JsonCreator constructor(
  @Schema(description = "Description", example = "ORA Community Order")
  val description: String,
  @Schema(description = "Length", example = "5")
  val length: Int,
  @Schema(description = "Length Unit", example = "Months")
  val lengthUnit: String?,
  val offenderManager: UnallocatedCaseConvictionPractitioner?,
  @Schema(description = "Start of sentence", example = "2021-11-15")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val startDate: LocalDate?,
  @Schema(description = "End of sentence", example = "2021-11-15")
  @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
  val endDate: LocalDate?,
  val offences: List<UnallocatedCaseConvictionOffence>,
) {
  companion object {
    fun from(conviction: Conviction, startDate: LocalDate?, endDate: LocalDate?, practitioner: UnallocatedCaseConvictionPractitioner?): UnallocatedCaseConviction {

      return UnallocatedCaseConviction(
        conviction.sentence!!.description,
        conviction.sentence.originalLength,
        conviction.sentence.originalLengthUnits,
        practitioner,
        startDate,
        endDate,
        conviction.offences.map { UnallocatedCaseConvictionOffence.from(it) }
      )
    }
  }
}

data class UnallocatedCaseConvictionPractitioner @JsonCreator constructor(
  @Schema(description = "Full Name", example = "John William Smith")
  val name: String?,
  @Schema(description = "Grade", example = "PSO")
  val grade: String?
)

data class UnallocatedCaseConvictionOffence @JsonCreator constructor(
  @Schema(description = "Description", example = "ORA Community Order")
  val description: String,
  @Schema(description = "Main offence", example = "true|false")
  val mainOffence: Boolean,
) {
  companion object {
    fun from(offence: Offence): UnallocatedCaseConvictionOffence {
      return UnallocatedCaseConvictionOffence(offence.detail.description, offence.mainOffence)
    }
  }
}
