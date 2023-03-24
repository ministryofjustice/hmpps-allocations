package uk.gov.justice.digital.hmpps.hmppsallocations.integration.domain

data class CaseViewAddressIntegration constructor(
  val buildingName: String? = null,
  val addressNumber: String? = null,
  val streetName: String? = null,
  val town: String? = null,
  val county: String? = null,
  val postcode: String? = null,
  val noFixedAbode: Boolean,
  val typeVerified: Boolean,
  val typeDescription: String,
  val startDate: String,
)
