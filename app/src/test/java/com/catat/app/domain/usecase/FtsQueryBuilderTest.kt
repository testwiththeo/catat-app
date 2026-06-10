package com.catat.app.domain.usecase

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FtsQueryBuilderTest {

    @Test
    fun `blank query matches all`() {
        assertEquals("*", FtsQueryBuilder.build(""))
        assertEquals("*", FtsQueryBuilder.build("   "))
    }

    @Test
    fun `tokens are prefix matched`() {
        assertEquals("login* crash*", FtsQueryBuilder.build("login crash"))
    }

    @Test
    fun `single character tokens are ignored`() {
        assertEquals("ab*", FtsQueryBuilder.build("a ab c"))
    }

    @Test
    fun `injection characters are stripped`() {
        assertEquals("drop* table*", FtsQueryBuilder.build("\"drop\"; -- table"))
    }
}
