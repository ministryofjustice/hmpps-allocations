package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity.repository

import java.time.LocalDateTime
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.GenerationType.IDENTITY
import javax.persistence.Id
import javax.persistence.Table

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
  val crn: String,

  @Column
  val tier: String,

  @Column
  val sentence_date: LocalDateTime,

  @Column
  val initial_appointment: LocalDateTime? = null,

  @Column
  val status: String
)
