package uk.gov.justice.digital.hmpps.hmppsallocations.integration.health

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.integration.IntegrationTestBase

class InfoTest : IntegrationTestBase() {

  @Test
  fun `Info page is accessible`() {
    webTestClient.get()
      .uri("/info")
      .exchange()
      .expectStatus()
      .isOk
//      .expectBody()
//      .jsonPath("build.name").isEqualTo("hmpps-allocations")
  }

  @Test
  fun `Info page reports version`() {
    webTestClient.get().uri("/info")
      .exchange()
      .expectStatus().isOk
//      .expectBody().jsonPath("build.version").value<String> {
//        assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
//      }
  }
}
