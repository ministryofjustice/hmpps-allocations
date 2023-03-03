package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.multipart.FilePart
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsallocations.service.TriggerReprocessService

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class TriggerProcessingCrnController(private val triggerReprocessService: TriggerReprocessService) {

  @PostMapping("/crn/reprocess")
  suspend fun reprocessCrns(@RequestPart("file") file: Mono<FilePart>): ResponseEntity<Void> {
    triggerReprocessService.sendEvents(fileToCases(file))
    return ResponseEntity.ok().build()
  }

  private suspend fun fileToCases(filePart: Mono<FilePart>): List<String> {
    val alphaNumericRegex = Regex("[^A-Za-z0-9]")
    return filePart.flatMapMany { file ->
      file.content().flatMapIterable { dataBuffer ->
        dataBuffer.asInputStream().bufferedReader().use { reader ->
          reader.lineSequence()
            .filter { it.isNotBlank() }
            .map { alphaNumericRegex.replace(it, "") }
            .toList()
        }
      }
    }.asFlow().toList()
  }
}
