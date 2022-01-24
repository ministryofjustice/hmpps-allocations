package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator

data class ConvictionRequirements @JsonCreator constructor(
  val requirements: List<ConvictionRequirement>
)

data class ConvictionRequirement @JsonCreator constructor(
  val requirementTypeSubCategory: RequirementCategory,
  val requirementTypeMainCategory: RequirementCategory,
  val length: Long,
  val lengthUnit: String,
)

data class RequirementCategory @JsonCreator constructor(
  val description: String
)
