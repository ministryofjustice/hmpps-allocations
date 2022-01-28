package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.UnallocatedCaseEntity

data class UnallocatedCaseConvictions @JsonCreator constructor (
  @Schema(description = "Offender Name", example = "John Smith")
  val name: String,
  @Schema(description = "CRN", example = "J111111")
  val crn: String,
  @Schema(description = "Latest tier of case", example = "D2")
  val tier: String,
  val active: List<UnallocatedCaseConviction>,
  val previous: List<UnallocatedCaseConviction>,
) {
  companion object {
    fun from(
      case: UnallocatedCaseEntity,
      active: List<Conviction>,
      previous: List<Conviction>
    ): UnallocatedCaseConvictions {
      return UnallocatedCaseConvictions(
        case.name, case.crn, case.tier,
        active.map { UnallocatedCaseConviction.from(it) },
        previous.map { UnallocatedCaseConviction.from(it) }
      )
    }
  }
}

data class UnallocatedCaseConviction @JsonCreator constructor(
  val description: String,
  val length: Int,
  val lengthUnit: String,
) {
  companion object {
    fun from(conviction: Conviction): UnallocatedCaseConviction {
      return UnallocatedCaseConviction(
        conviction.sentence!!.description,
        conviction.sentence.originalLength,
        conviction.sentence.originalLengthUnits,
      )
    }
  }
}
