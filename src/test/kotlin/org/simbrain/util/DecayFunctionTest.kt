package org.simbrain.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.simbrain.util.decayfunctions.ExponentialDecayFunction
import org.simbrain.util.decayfunctions.LinearDecayFunction
import org.simbrain.util.decayfunctions.StepDecayFunction

/**
 * Test decay functions
 */
class DecayFunctionTest {

    @Test
    fun `test linear`() {
        val decay = LinearDecayFunction(10.0)
        assertEquals(1.0, decay.getScalingFactor(0.0))
        assertEquals(0.0, decay.getScalingFactor(11.0))
    }

    @Test
    fun `test linear with peak`() {
        val decay = LinearDecayFunction(10.0)
        decay.dispersion = 9.0
        decay.peakDistance = 10.0
        assertEquals(1.0, decay.getScalingFactor(10.0))
        assertEquals(0.0, decay.getScalingFactor(1.0))
        assertEquals(0.0, decay.getScalingFactor(19.0))
        assertTrue(decay.getScalingFactor(9.0) > decay.getScalingFactor(8.0))
    }

    @Test
    fun `test step`() {
        val decay = StepDecayFunction()
        decay.dispersion = 5.0
        assertEquals(1.0, decay.getScalingFactor(0.0))
        assertEquals(0.0, decay.getScalingFactor(6.0))
    }

    @Test
    fun `test step with peak`() {
        val decay = StepDecayFunction()
        decay.dispersion = 2.0
        decay.peakDistance = 5.0
        assertEquals(0.0, decay.getScalingFactor(1.0))
        assertEquals(0.0, decay.getScalingFactor(2.0))
        assertEquals(1.0, decay.getScalingFactor(3.1))
        assertEquals(1.0, decay.getScalingFactor(5.0))
        assertEquals(0.0, decay.getScalingFactor(8.0))
        // println(makeStringArray(0.0, 10.0, decay::getScalingFactor))
    }

    @Test
    fun `test exponential`() {
        val decay = ExponentialDecayFunction()
        decay.dispersion = 1.0
        assertEquals(1.0, decay.getScalingFactor(0.0))
        assertEquals(1/Math.E, decay.getScalingFactor(1.0))
        // println(makeStringArray(0.0, 10.0, decay::getScalingFactor))
    }

    //TODO
    @Test
    fun `test exponential with peak`() {
        val decay = ExponentialDecayFunction()
        decay.dispersion = 2.0
        decay.peakDistance = 10.0
        assertEquals(1/decay.dispersion, decay.getScalingFactor(10.0))
        // println(makeStringArray(0.0,20.0, decay::getScalingFactor))
    }

}