package uk.gov.justice.digital.hmpps.hmppsallocations.client.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDateTime

data class Document @JsonCreator constructor(
  val id: String,
  val subType: DocumentType,
  val reportDocumentDates: DocumentDates
)

data class DocumentType @JsonCreator constructor(
  val code: String,
  var description: String
)

data class DocumentDates @JsonCreator constructor(
  val completedDate: LocalDateTime?
)
