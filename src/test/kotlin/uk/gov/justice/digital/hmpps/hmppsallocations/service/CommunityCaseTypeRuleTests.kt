package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CommunityCaseTypeRuleTests {
  private val communityCaseTypeRule = CommunityCaseTypeRule()

  @Test
  fun `must return true when sentence type code is Statutory Community`() {
    val result = communityCaseTypeRule.isCaseType("SP", "A")
    Assertions.assertTrue(result)
  }

  @Test
  fun `must return true when sentence type code is Non Statutory Community`() {
    val result = communityCaseTypeRule.isCaseType("NP", "A")
    Assertions.assertTrue(result)
  }

  @Test
  fun `must return false when sentence type code is Statutory Custody`() {
    val result = communityCaseTypeRule.isCaseType("SC", "A")
    Assertions.assertFalse(result)
  }

  @Test
  fun `must return community case type when getting case type`() {
    val result = communityCaseTypeRule.getCaseType()
    Assertions.assertEquals("COMMUNITY", result)
  }
}
