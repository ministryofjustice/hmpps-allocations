package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import java.time.LocalDate

data class DeliusCaseView constructor(
  val name: Name,
  val dateOfBirth: LocalDate,
  val gender: String?,
  val pncNumber: String?,
  val mainAddress: MainAddress?,
  val sentence: Sentence,
  val offences: List<Offence>,
  val requirements: List<Requirement>,
  val cpsPack: CaseViewDocument?,
  val preConvictionDocument: CaseViewDocument?,
  val courtReport: CaseViewDocument?,
  val age: Int,
)

data class MainAddress constructor(
  val buildingName: String?,
  val addressNumber: String?,
  val streetName: String?,
  val town: String?,
  val county: String?,
  val postcode: String?,
  val noFixedAbode: Boolean,
  val typeVerified: Boolean,
  val typeDescription: String?,
  val startDate: LocalDate,
)

data class Sentence constructor(
  val description: String,
  val startDate: LocalDate,
  val length: String,
  val endDate: LocalDate,
)

data class Offence constructor(
  val mainCategory: String,
  val subCategory: String,
  val mainOffence: Boolean,
)

data class Requirement constructor(
  val mainCategory: String,
  val subCategory: String,
  val length: String,
)

data class CaseViewDocument constructor(
  val documentId: String,
  val documentName: String,
  val dateCreated: LocalDate?,
  val description: String?,
)
