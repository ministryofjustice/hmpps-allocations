package uk.gov.justice.digital.hmpps.hmppsallocations.client.dto

import uk.gov.justice.digital.hmpps.hmppsallocations.client.Name
import java.time.LocalDate

data class DeliusProbationRecord(
  val crn: String,
  val name: Name,
  val activeEvents: List<SentencedEvent>,
  val inactiveEvents: List<SentencedEvent>,
)

data class SentencedEvent(
  val sentence: ProbationRecordSentence,
  val offences: List<SentenceOffence>,
  val manager: CommunityEventManager?
)

data class CommunityEventManager(
  val code: String,
  val name: Name,
  val email: String?,
  val grade: String?,
)

data class ProbationRecordSentence(
  val description: String,
  val length: String,
  val startDate: LocalDate,
  val terminationDate: LocalDate?,
)

data class SentenceOffence(
  val description: String,
  val main: Boolean,
)
