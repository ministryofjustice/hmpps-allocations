package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity

import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table
import javax.validation.constraints.NotNull

@Entity
@Table(name = "unallocated_cases")
data class UnallocatedCaseEntity(
  @Id
  @Column
  @GeneratedValue(strategy = IDENTITY)
  val id: Long? = null,

  @Column
  val name: String,

  @Column
  @NotNull
  val crn: String,

  @Column
  var tier: String,

  @Column(name = "sentence_date")
  val sentenceDate: LocalDate,

  @Column(name = "initial_appointment")
  val initialAppointment: LocalDate? = null,

  @Column
  val status: String,

  @Column(name = "previous_conviction_date")
  val previousConvictionDate: LocalDate? = null,

  @Column(name = "offender_manager_forename")
  val offenderManagerForename: String? = null,

  @Column(name = "offender_manager_surname")
  val offenderManagerSurname: String? = null

)
