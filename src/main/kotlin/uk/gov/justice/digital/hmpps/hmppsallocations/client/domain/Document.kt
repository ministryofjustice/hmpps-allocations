package uk.gov.justice.digital.hmpps.hmppsallocations.client.domain

import com.fasterxml.jackson.annotation.JsonCreator
import java.time.LocalDateTime

data class Document @JsonCreator constructor(
  val id: String,
  val type: DocumentType,
  val subType: DocumentType?,
  val reportDocumentDates: DocumentDates?,
  val createdAt: LocalDateTime,
  val lastModifiedAt: LocalDateTime?
)

data class DocumentType @JsonCreator constructor(
  val code: String,
  var description: String
)

data class DocumentDates @JsonCreator constructor(
  val completedDate: LocalDateTime?
)
