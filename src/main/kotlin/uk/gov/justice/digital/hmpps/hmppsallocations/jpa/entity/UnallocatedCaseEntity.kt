package uk.gov.justice.digital.hmpps.hmppsallocations.jpa.entity

import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes
import java.time.LocalDate
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
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

  @Column(name = "case_type")
  @Enumerated(EnumType.STRING)
  var caseType: CaseTypes,

  @Column
  var teamCode: String? = null,

  @Column
  var providerCode: String
) {
  data class Builder(
    var id: Long? = null,
    var name: String? = null,
    var crn: String? = null,
    var tier: String? = null,
    var sentenceDate: LocalDate? = null,
    var initialAppointment: LocalDate? = null,
    var status: String? = null,
    var previousConvictionDate: LocalDate? = null,
    var offenderManagerForename: String? = null,
    var offenderManagerSurname: String? = null,
    var offenderManagerGrade: String? = null,
    var convictionId: Long? = null,
    var caseType: CaseTypes? = null,
    var teamCode: String? = null,
    var providerCode: String? = null
  ) {
    fun id(id: Long) = apply { this.id = id }
    fun name(name: String) = apply { this.name = name }
    fun crn(crn: String) = apply { this.crn = crn }
    fun tier(tier: String) = apply { this.tier = tier }
    fun sentenceDate(sentenceDate: LocalDate) = apply { this.sentenceDate = sentenceDate }

    fun initialAppointment(initialAppointment: LocalDate) = apply { this.initialAppointment = initialAppointment }
    fun status(status: String) = apply { this.status = status }
    fun previousConvictionDate(previousConvictionDate: LocalDate) = apply { this.previousConvictionDate = previousConvictionDate }
    fun offenderManagerForename(offenderManagerForename: String) = apply { this.offenderManagerForename = offenderManagerForename }
    fun offenderManagerSurname(offenderManagerSurname: String) = apply { this.offenderManagerSurname = offenderManagerSurname }
    fun offenderManagerGrade(offenderManagerGrade: String) = apply { this.offenderManagerGrade = offenderManagerGrade }
    fun convictionId(convictionId: Long) = apply { this.convictionId = convictionId }

    fun caseType(caseType: CaseTypes) = apply { this.caseType = caseType }
    fun teamCode(teamCode: String) = apply { this.teamCode = teamCode }
    fun providerCode(providerCode: String) = apply { this.providerCode = providerCode }

    fun build() = UnallocatedCaseEntity(
      id, name ?: "", crn!!, tier ?: "", sentenceDate!!, initialAppointment, status ?: "",
      previousConvictionDate, offenderManagerForename, offenderManagerSurname, offenderManagerGrade, convictionId!!,
      caseType ?: CaseTypes.UNKNOWN, teamCode, providerCode ?: ""
    )
  }
}
