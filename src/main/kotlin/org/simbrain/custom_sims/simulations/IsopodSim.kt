package org.simbrain.custom_sims.simulations

import kotlinx.coroutines.launch
import org.simbrain.custom_sims.*
import org.simbrain.network.core.connect
import org.simbrain.network.neuron_update_rules.LinearRule
import org.simbrain.network.util.BiasedScalarData
import org.simbrain.util.environment.SmellSource
import org.simbrain.util.piccolo.TileMap
import org.simbrain.util.place
import org.simbrain.util.point
import org.simbrain.util.showSaveDialog
import org.simbrain.util.stats.distributions.NormalDistribution
import org.simbrain.util.stats.distributions.UniformRealDistribution
import org.simbrain.world.odorworld.effectors.Effector
import org.simbrain.world.odorworld.entities.EntityType
import org.simbrain.world.odorworld.entities.OdorWorldEntity
import org.simbrain.world.odorworld.sensors.SmellSensor

/**
 * A simulation of Isopod navigation. With Peter Hinow and Kaiden Schmidt.
 */
val isopodSim = newSim {

    // Adjustable parameters for sim
    var defaultNumTrials = 5
    val maxIterationsPerTrial = 5000
    val hitRadius = 80

    // Other variables
    var log = ""
    var trialNum = 0

    // Clear the workspace
    workspace.clearWorkspace()

    // ----- Network construction ------

    val networkComponent = addNetworkComponent("Network")
    val network = networkComponent.network
    val noiseSource = NormalDistribution(1.0, .9)

    val neuronLeftSensor = network.addNeuron {
        location = point(0, 100)
        upperBound = 100.0
        label = "Left"
        with (updateRule as LinearRule) {
            noiseGenerator = noiseSource
            addNoise = true
        }
    }
    val neuronRightSensor = network.addNeuron {
        location = point(100, 100)
        upperBound = 100.0
        label = "Right"
        with (updateRule as LinearRule) {
            noiseGenerator = noiseSource
            addNoise = true
        }
    }
    val neuronLeftTurning = network.addNeuron {
        location = point(0, 0)
        upperBound = 150.0
        label = "Turn Left"
    }
    val neuronRightTurning = network.addNeuron {
        location = point(100, 0)
        upperBound = 150.0
        label = "Turn Right"
    }
    val neuronStraight = network.addNeuron {
        location = point(50, 0)
        upperBound = 10.0
        label = "Straight"
        (dataHolder as BiasedScalarData).bias = 5.0
    }

    // Create the weights
    connect(neuronLeftSensor, neuronLeftTurning, 10.0, 0.0, 50.0)
    connect(neuronRightSensor, neuronRightTurning, 10.0, 0.0, 50.0)

    // Location of the network in the desktop
    withGui {
        place(networkComponent) {
            location = point(180, 10)
            width = 400
            height = 400
        }
    }

    // ----- Build 2d World ------

    val odorWorldComponent = addOdorWorldComponent()
    val odorWorld = odorWorldComponent.world

    // Object references we'll need later
    val straightMovement: Effector
    val turnLeft: Effector
    val turnRight: Effector
    var leftSensor: SmellSensor
    var rightSensor: SmellSensor
    val isopod: OdorWorldEntity

    var collision = false

    odorWorld.apply {

        wrapAround = false
        isObjectsBlockMovement = true

        tileMap = TileMap(25, 25)
        tileMap.fill(2)

        // Body could be represented by a triangle or rhombus
        isopod = addEntity(centerLocation.x, centerLocation.y, EntityType.ISOPOD).apply {
            name = "isopod"
            heading = 90.0
            addDefaultEffectors()
            straightMovement = effectors[0]
            turnLeft = effectors[1]
            turnRight = effectors[2]
            isShowSensorsAndEffectors = false

            // Can add more smell sensors here
            // Options: new sensor in back; triangular array
            leftSensor = SmellSensor().apply {
                radius = 40.0
                theta = 45.0
                addSensor(this)
            }
            rightSensor = SmellSensor().apply {
                radius = 40.0
                theta = -45.0
                addSensor(this)
            }
            events.onCollided {
                if (it is OdorWorldEntity) {
                    log += "# Collided with ${it.name}\n"
                } else {
                    log += "# Collided with wall\n"
                }
                collision = true
            }
            manualMovement.manualStraightMovementIncrement = 2.0
            manualMovement.manualMotionTurnIncrement = 2.0
        }

        fun addFish(x: Double, y: Double) {
            odorWorld.addEntity(x, y, EntityType.FISH).apply {
                name = "Fish"
                smellSource = SmellSource.createScalarSource(1).apply {
                    dispersion = 350.0
                }
            }
        }

        // adding fish to four corners of the world
        val fishHalfWidth = EntityType.FISH.imageWidth / 2
        val fishHalfHeight = EntityType.FISH.imageHeight / 2

        addFish(odorWorld.width - fishHalfWidth, odorWorld.height - fishHalfHeight)
        addFish(fishHalfWidth, odorWorld.height - fishHalfHeight)
        addFish(fishHalfWidth, fishHalfHeight)
        addFish(odorWorld.width - fishHalfWidth, fishHalfHeight)

    }

    withGui {
        place(odorWorldComponent) {
            location = point(590, 10)
            width = 600
            height = 600
        }
    }

    // ----- Make Couplings ------

    with(couplingManager) {
        neuronStraight couple straightMovement
        neuronLeftTurning couple turnLeft
        neuronRightTurning couple turnRight
        leftSensor couple neuronLeftSensor
        rightSensor couple neuronRightSensor
    }

    fun resetIsopod() {
        isopod.location = odorWorld.centerLocation
        isopod.heading = UniformRealDistribution(0.0,360.0).sampleDouble()
    }

    workspace.addUpdateAction(updateAction("Found fish") {
        val foundFish = odorWorld.entityList
            .filter { it.entityType == EntityType.FISH }
            .any { fish -> fish.location.distance(isopod.location) < hitRadius }
        if (foundFish) {
            collision = true
        }
    })

    withGui {
        createControlPanel("Control Panel", 5, 10) {

            addButton("Run one trial") {
                resetIsopod()
                log = ""
                var iteration = 0
                workspace.coroutineScope.launch {
                    log += "# Heading: ${isopod.heading}\n"
                    while (++iteration < maxIterationsPerTrial) {
                        workspace.iterateSuspend(1)
                        if (collision) {
                            break
                        } else {
                            log +=  "${isopod.x}, ${isopod.y}\n"
                        }
                    }
                    collision = false
                    showSaveDialog("", "singleTrial.csv") {
                        writeText(log)
                    }
                }
            }

            val numTrialsTF = addTextField("Number of trials", "" + defaultNumTrials)

            addButton("Run trials") {
                workspace.coroutineScope.launch {
                    log = ""
                    var iteration = 0
                    defaultNumTrials = Integer.parseInt(numTrialsTF.text)
                    while(trialNum < defaultNumTrials) {
                        log += "# Trial: ${trialNum + 1}\n"
                        resetIsopod()
                        log += "# Heading: ${isopod.heading}\n"
                        while (++iteration < maxIterationsPerTrial) {
                            workspace.iterateSuspend(1)
                            if (collision) {
                                break
                            } else {
                                log +=  "${isopod.x}, ${isopod.y}\n"
                            }
                        }
                        trialNum++
                        collision = false
                    }
                    trialNum = 0
                    showSaveDialog("", "multipleTrials.csv") {
                        writeText(log)
                    }
                }
            }
        }

    }

    // addDocViewer("Test", "Braitenberg.html").apply {
    //     place(this) {
    //         location = point(145, 421)
    //         width = 400
    //         height = 330
    //     }
    // }

}
