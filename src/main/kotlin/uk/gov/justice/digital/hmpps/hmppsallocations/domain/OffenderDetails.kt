package uk.gov.justice.digital.hmpps.hmppsallocations.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDate
import java.time.Period

data class OffenderDetails @JsonCreator constructor(
  val firstName: String,
  val surname: String,
  val gender: String,
  val dateOfBirth: LocalDate,
  val otherIds: OffenderDetailsOtherIds?,
  val contactDetails: ContactDetails
) {
  fun getAge(): Int = Period.between(dateOfBirth, LocalDate.now()).years
}

data class OffenderDetailsOtherIds @JsonCreator constructor(
  val pncNumber: String?
)

data class ContactDetails @JsonCreator constructor(
  val addresses: List<Address>?
)

data class Address @JsonCreator constructor(
  val addressNumber: String?,
  val buildingName: String?,
  val streetName: String?,
  val town: String?,
  val county: String?,
  val postcode: String?,
  val status: AddressStatus,
  val noFixedAbode: Boolean
)

data class AddressStatus @JsonCreator constructor(
  val code: String
)
