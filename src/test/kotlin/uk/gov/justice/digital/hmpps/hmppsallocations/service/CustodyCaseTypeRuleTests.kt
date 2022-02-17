package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class CustodyCaseTypeRuleTests {
  private val custodyCaseTypeRule = CustodyCaseTypeRule()

  @Test
  fun `must return true when sentence type code is Statutory Custody`() {
    val result = custodyCaseTypeRule.isCaseType("SC", "A")
    Assertions.assertTrue(result)
  }

  @Test
  fun `must return true when sentence type code is Non Statutory Custody`() {
    val result = custodyCaseTypeRule.isCaseType("NC", "A")
    Assertions.assertTrue(result)
  }

  @Test
  fun `must return false when sentence type code is Statutory Community`() {
    val result = custodyCaseTypeRule.isCaseType("SP", "A")
    Assertions.assertFalse(result)
  }

  @Test
  fun `must return custody case type when getting case type`() {
    val result = custodyCaseTypeRule.getCaseType()
    Assertions.assertEquals("CUSTODY", result)
  }
}
