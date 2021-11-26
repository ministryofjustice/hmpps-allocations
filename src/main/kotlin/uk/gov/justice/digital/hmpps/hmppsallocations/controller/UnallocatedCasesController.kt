package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import com.opencsv.bean.CsvBindByPosition
import com.opencsv.bean.CsvToBeanBuilder
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UnallocatedCasesService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UploadUnallocatedCasesService
import java.io.InputStreamReader

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class UnallocatedCasesController(
  private val unallocatedCasesService: UnallocatedCasesService,
  private val uploadUnallocatedCasesService: UploadUnallocatedCasesService
) {

  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated")
  fun getUnallocatedCases(): ResponseEntity<List<UnallocatedCase>> {
    return ResponseEntity.ok(
      unallocatedCasesService.getAll()
    )
  }

  @PostMapping("/cases/unallocated/upload")
  fun uploadUnallocatedCases(@RequestParam("file") file: MultipartFile): ResponseEntity<Void> {
    uploadUnallocatedCasesService.sendEvents(fileToUnallocatedCases(file))
    return ResponseEntity.ok().build()
  }

  @Throws(Exception::class)
  fun fileToUnallocatedCases(file: MultipartFile): List<UnallocatedCaseCsv> {
    val reader = InputStreamReader(file.inputStream)
    val cb = CsvToBeanBuilder<UnallocatedCaseCsv>(reader)
      .withType(UnallocatedCaseCsv::class.java)
      .build()
    val unallocatedCases = cb.parse()
    reader.close()
    return unallocatedCases
  }
}

data class UnallocatedCaseCsv(
  @CsvBindByPosition(position = 0)
  var crn: String? = null,
  @CsvBindByPosition(position = 2)
  var tier: String? = null,
  @CsvBindByPosition(position = 3)
  var status: String? = null
)
