/*
 * Part of Simbrain--a java-based neural network kit
 * Copyright (C) 2005,2007 The Authors.  See http://www.simbrain.net/credits
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.simbrain.network.spikeresponders

import org.simbrain.network.core.Connector
import org.simbrain.network.core.Synapse
import org.simbrain.network.matrix.NeuronArray
import org.simbrain.network.matrix.WeightMatrix
import org.simbrain.network.synapse_update_rules.spikeresponders.SpikeResponder
import org.simbrain.network.util.MatrixDataHolder
import org.simbrain.network.util.ScalarDataHolder
import org.simbrain.network.util.SpikingMatrixData
import org.simbrain.util.UserParameter
import smile.math.matrix.Matrix

/**
 * TODO
 */
class RiseAndDecay : SpikeResponder() {

    /**
     * Maximum response value.
     */
    @UserParameter(
        label = "Maximum Response",
        description = "Maximum response value.",
        increment = .1,
        order = 1)
    var maximumResponse = 1.0

    /**
     * The time constant of decay and recovery (ms).
     */
    @UserParameter(
        label = "Time constant",
        description = "Time constant for rising decay (ms). Roughly the time it takes to rise to max value then decay" +
                "to near-baseline. Larger time constants produce slower changes.",
        increment = .1,
        order = 2
    )
    var timeConstant = 3.0

    override fun deepCopy(): RiseAndDecay {
        val rad = RiseAndDecay()
        rad.maximumResponse = maximumResponse
        rad.timeConstant = timeConstant
        return rad
    }

    override fun apply(conn: Connector, data: MatrixDataHolder) {
        val wm = conn.let { if (it is WeightMatrix) it else return }
        val na = conn.source.let { if (it is NeuronArray) it else return }
        val responseData = data.let { if (it is RiseAndDecayMatrixData) it else return }
        val spikeData = na.dataHolder.let { if (it is SpikingMatrixData) it else return }
        if (na.updateRule.isSpikingRule) {
            for (i in 0 until wm.weightMatrix.nrows()) {
                for (j in 0 until wm.weightMatrix.ncols()) {
                    val (psr, recovery) = riseAndDecay(
                        spikeData.spikes[j],
                        wm.psrMatrix[i, j],
                        responseData.recoveryMatrix[i,j],
                        wm.weightMatrix[i, j],
                        na.network.timeStep
                    )
                    wm.psrMatrix.set(i, j, psr)
                    responseData.recoveryMatrix.set(i,j, recovery)
                }
            }
        }
    }

    override fun createMatrixData(rows: Int, cols: Int): MatrixDataHolder {
        return RiseAndDecayMatrixData(rows, cols)
    }

    override fun apply(s: Synapse, responderData: ScalarDataHolder) {
        val data = responderData as RiseAndDecayData
        val (psr, recovery) = riseAndDecay(
            s.source.isSpike,
            s.psr,
            data.recovery,
            s.strength,
            s.parentNetwork.timeStep )
        s.psr = psr
        data.recovery = recovery
    }

    private fun riseAndDecay(spiked: Boolean,
                             initPsr: Double,
                             initRecovery: Double,
                             strength: Double,
                             timeStep: Double): Pair<Double, Double> {
        var recovery = initRecovery
        var psr = initPsr
        if (spiked) {
            recovery = 1.0
        }
        recovery += timeStep / timeConstant * -recovery
        psr += (timeStep / timeConstant
                * (Math.E * maximumResponse * recovery * (1 - psr) - psr))
        psr *= strength
        return Pair(psr, recovery)
    }

    override fun createResponderData(): ScalarDataHolder {
        return RiseAndDecayData()
    }

    override fun getDescription(): String {
        return "Rise and Decay"
    }

    override val name: String
        get() = "Rise and Decay"
}


class RiseAndDecayData(
    @UserParameter(
        label = "Recovery"
    )
    var recovery: Double = 0.0,
) : ScalarDataHolder {
    override fun copy(): RiseAndDecayData {
        return RiseAndDecayData(recovery)
    }
}


class RiseAndDecayMatrixData(val rows: Int, val cols: Int) : MatrixDataHolder {
    var recoveryMatrix = Matrix(rows, cols)
    override fun copy() = RiseAndDecayMatrixData(rows, cols).also {
        it.recoveryMatrix = recoveryMatrix.clone()
    }
}