package com.octo.montecarlo;

import static org.junit.Assert.*;

import org.junit.Test;

public class BatmanMonteCarloTest {

    private BatmanMonteCarlo b = new BatmanMonteCarlo(0);

    @Test
    public void testZero() {
        assertTrue(b.f(0, 0));
    }

    @Test
    public void testTenZero() {
        assertFalse(b.f(10, 0));
    }

    @Test
    public void testZeroTen() {
        assertFalse(b.f(0, 10));
    }

    @Test
    public void test4242() {
        assertFalse(b.f(4.2, 4.2));
    }
}
