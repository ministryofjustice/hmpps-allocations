package uk.gov.justice.digital.hmpps.hmppsallocations.controller

import com.opencsv.bean.CsvBindByPosition
import com.opencsv.bean.CsvToBeanBuilder
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCase
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseConvictions
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.UnallocatedCaseRisks
import uk.gov.justice.digital.hmpps.hmppsallocations.service.GetUnallocatedCaseService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.UploadUnallocatedCasesService
import uk.gov.justice.digital.hmpps.hmppsallocations.service.exception.EntityNotFoundException
import java.io.InputStreamReader

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class UnallocatedCasesController(
  private val uploadUnallocatedCasesService: UploadUnallocatedCasesService,
  private val getUnallocatedCaseService: GetUnallocatedCaseService
) {

  @Operation(summary = "Retrieve all unallocated cases")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated")
  fun getUnallocatedCases(): ResponseEntity<List<UnallocatedCase>> {
    return ResponseEntity.ok(
      getUnallocatedCaseService.getAll()
    )
  }

  @Operation(summary = "Retrieve unallocated cases by crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}")
  fun getUnallocatedCase(@PathVariable(required = true) crn: String): ResponseEntity<UnallocatedCase> =
    ResponseEntity.ok(
      getUnallocatedCaseService.getCase(crn) ?: throw EntityNotFoundException("Unallocated case Not Found for $crn")
    )

  @Operation(summary = "Retrieve unallocated case convictions by crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}/convictions")
  fun getUnallocatedCaseConvictions(@PathVariable(required = true) crn: String): ResponseEntity<UnallocatedCaseConvictions> =
    ResponseEntity.ok(
      getUnallocatedCaseService.getCaseConvictions(crn) ?: throw EntityNotFoundException("Unallocated case Not Found for $crn")
    )

  @Operation(summary = "Retrieve unallocated case risks by crn")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "OK"),
      ApiResponse(responseCode = "404", description = "Result Not Found")
    ]
  )
  @PreAuthorize("hasRole('ROLE_MANAGE_A_WORKFORCE_ALLOCATE')")
  @GetMapping("/cases/unallocated/{crn}/risks")
  fun getUnallocatedCaseRisks(@PathVariable(required = true) crn: String): ResponseEntity<UnallocatedCaseRisks> =
    ResponseEntity.ok(
      getUnallocatedCaseService.getCaseRisks(crn) ?: throw EntityNotFoundException("Unallocated case risks Not Found for $crn")
    )

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
  var crn: String? = null
)
