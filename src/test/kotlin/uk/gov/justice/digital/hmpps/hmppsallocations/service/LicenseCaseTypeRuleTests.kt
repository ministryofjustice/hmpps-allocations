package uk.gov.justice.digital.hmpps.hmppsallocations.service

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsallocations.domain.CaseTypes

class LicenseCaseTypeRuleTests {
  private val licenseCaseTypeRule = LicenseCaseTypeRule()

  @Test
  fun `must return false when sentence type code is Statutory Custody and Custodial Status is Sentenced In Custody`() {
    val result = licenseCaseTypeRule.isCaseType("SC", "A")
    Assertions.assertFalse(result)
  }

  @Test
  fun `must return false when sentence type code is Non Statutory Custody and Custodial Status is Sentenced In Custody`() {
    val result = licenseCaseTypeRule.isCaseType("NC", "A")
    Assertions.assertFalse(result)
  }

  @Test
  fun `must return true when sentence type code is Statutory Custody and Custodial Status is Released On Licence`() {
    val result = licenseCaseTypeRule.isCaseType("SC", "B")
    Assertions.assertTrue(result)
  }

  @Test
  fun `must return true when sentence type code is Non Statutory Custody and Custodial Status is Released On Licence`() {
    val result = licenseCaseTypeRule.isCaseType("NC", "B")
    Assertions.assertTrue(result)
  }

  @Test
  fun `must return false when sentence type code is Statutory Community`() {
    val result = licenseCaseTypeRule.isCaseType("SP", "A")
    Assertions.assertFalse(result)
  }

  @Test
  fun `must return custody case type when getting case type`() {
    val result = licenseCaseTypeRule.getCaseType()
    Assertions.assertEquals(CaseTypes.LICENSE, result)
  }
}
