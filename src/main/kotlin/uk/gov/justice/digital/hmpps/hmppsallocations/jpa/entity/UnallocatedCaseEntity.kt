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
  var name: String,

  @Column
  @NotNull
  val crn: String,

  @Column
  var tier: String,

  @Column(name = "sentence_date")
  var sentenceDate: LocalDate,

  @Column(name = "initial_appointment")
  var initialAppointment: LocalDate? = null,

  @Column
  var status: String,

  @Column(name = "previous_conviction_date")
  var previousConvictionDate: LocalDate? = null,

  @Column(name = "offender_manager_forename")
  var offenderManagerForename: String? = null,

  @Column(name = "offender_manager_surname")
  var offenderManagerSurname: String? = null,

  @Column(name = "offender_manager_grade")
  var offenderManagerGrade: String? = null,

  @Column(name = "conviction_id")
  @NotNull
  val convictionId: Long,

)
