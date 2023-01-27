package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.NotNull
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Entity
@Table(name = "unallocated_cases")
data class UnallocatedCaseEntity(
  @Id
  @Column
  @GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)
  val id: Long? = null,

  @Column
  var name: String,

  @Column
  @NotNull
  val crn: String,

  @Column
  var tier: String,

  @Column(name = "initial_appointment")
  var initialAppointment: LocalDate? = null,

  @Column(name = "conviction_id")
  @NotNull
  val convictionId: Long,

  @Column(name = "case_type")
  @Enumerated(jakarta.persistence.EnumType.STRING)
  var caseType: CaseTypes,

  @Column
  var teamCode: String? = null,

  @Column
  var providerCode: String,

  @Column
  val createdDate: ZonedDateTime = ZonedDateTime.now().truncatedTo(ChronoUnit.SECONDS),

  @Column
  var convictionNumber: Int
)
