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
  var id: Long? = null,

  @Column
  var name: String,

  @Column
  @NotNull
  var crn: String,

  @Column
  var tier: String,

  @Column(name = "sentence_date")
  var sentenceDate: LocalDate,

  @Column(name = "initial_appointment")
  var initialAppointment: LocalDate? = null,

  @Column
  var status: String,

  @Column(name = "previous_conviction_date")
  val previousConvictionDate: LocalDate? = null

)
