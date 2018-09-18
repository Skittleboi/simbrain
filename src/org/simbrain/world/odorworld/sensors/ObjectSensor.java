package org.simbrain.world.odorworld.sensors;

import org.simbrain.util.UserParameter;
import org.simbrain.util.environment.ScalarSmellSource;
import org.simbrain.util.math.SimbrainMath;
import org.simbrain.util.propertyeditor2.EditableObject;
import org.simbrain.workspace.Producible;
import org.simbrain.world.odorworld.entities.OdorWorldEntity;

/**
 * Sensor that reacts when an object of a given type is near it.
 */
public class ObjectSensor extends Sensor {

    /**
     * Current value of the sensor.
     */
    private double value = 0;

    /**
     * The type of the object represented, e.g. Swiss.gif.
     */
    @UserParameter(label = "Object Type",
            description = "What type of object this sensor responds to",
            order = 5)
    private OdorWorldEntity.EntityType objectType = OdorWorldEntity.EntityType.SWISS;

    /**
     * Instantiate an object sensor.
     *
     * @param parent parent entity
     * @param objectType the type (e.g. Swiss.gif)
     */
    public ObjectSensor(OdorWorldEntity parent, OdorWorldEntity.EntityType objectType) {
        super(parent, objectType.toString());
        this.objectType = objectType;
    }

    /**
     * Instantiate an object sensor.
     *
     * @param parent parent entity
     */
    public ObjectSensor(OdorWorldEntity parent) {
        super(parent, "Object Sensor");
    }

    @Override
    public void update() {
        value = 0;
        for (OdorWorldEntity entity : parent.getParentWorld().getEntityList()) {
            ScalarSmellSource smell = entity.getScalarSmell();
            if(entity.getEntityType() == objectType) {
                double distance = SimbrainMath.distance(parent.getCenterLocation(), entity.getCenterLocation());
                value += smell.getValue(distance);
            }
        }
    }

    @Producible(idMethod = "getId", customDescriptionMethod = "getSensorDescription")
    public double getCurrentValue() {
        return value;
    }

    @Override
    public String getTypeDescription() {
        return objectType.toString();
    }

    /**
     * Called by reflection to return a custom description for the {@link org.simbrain.workspace.gui.couplingmanager.AttributePanel.ProducerOrConsumer}
     * corresponding to object sensors.
     */
    public String getSensorDescription() {
        return getParent().getName() + ":" + getTypeDescription() + " sensor";
    }

    @Override
    public EditableObject copy() {
        return new ObjectSensor(parent, objectType);
    }

    @Override
    public String getName() {
        return "ObjectSensor";
    }
}
