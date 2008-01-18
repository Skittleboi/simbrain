/**
 * This is an example of a component, which serves as a DragSource as well as
 * Drop Target. To illustrate the concept, JList has been used as a droppable
 * target and a draggable source. Any component can be used instead of a JList.
 * The code also contains debugging messages which can be used for diagnostics
 * and understanding the flow of events.
 *
 * @version 1.0
 */
// TODO: Add reference to author
package org.simbrain.workspace.gui;

import java.awt.Point;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DragGestureEvent;
import java.awt.dnd.DragSourceDragEvent;
import java.awt.dnd.DragSourceDropEvent;
import java.awt.dnd.DragSourceEvent;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JList;

import org.apache.log4j.Logger;
import org.simbrain.workspace.Consumer;
import org.simbrain.workspace.ConsumingAttribute;
import org.simbrain.workspace.Coupling;
import org.simbrain.workspace.Producer;
import org.simbrain.workspace.ProducingAttribute;

/**
 * Tray which manages binding of couplings.  Most of the code handles drag and drop issues.
 *
 * @author jyoshimi
 *
 */
public class CouplingTray extends JList implements DropTargetListener {

    /** Log4j logger. */
    private Logger LOGGER = Logger.getLogger(CouplingTray.class);
    
    /**
     * Enables this component to be a dropTarget.
     */
    private DropTarget dropTarget = null;

    /**
     * Constructor - initializes the DropTarget and DragSource.
     */
    public CouplingTray() {
        dropTarget = new DropTarget(this, this);
    }

    /**
     * Invoked when you are dragging over the DropSite.
     * @param event drop target drag.
     */
    public void dragEnter(final DropTargetDragEvent event) {
        LOGGER.trace("dragEnter(DropTargetDragEvent) called");
    }

    /**
     * Invoked when you are exit the DropSite without dropping.
     * @param event drop target drag.
     */
    public void dragExit(final DropTargetEvent event) {
        LOGGER.trace("dragExit(DropTargetEvent) called");
    }

    /**
     * Invoked when a drag operation is going on.
     * @param event drop target drag.
     */
    public void dragOver(final DropTargetDragEvent event) {
        LOGGER.trace("dragOver(DropTargetDragEvent) called");
        this.setSelectedIndex(this.locationToIndex(event.getLocation()));

    }

    /**
     * A drop has occurred.
     *  @param event drop target drag.
     */
    public void drop(final DropTargetDropEvent event) {
        LOGGER.trace("We have a drop: " + this.getSelectedIndex());
        try {
            Transferable transferable = event.getTransferable();
            if (transferable.isDataFlavorSupported(ListData.LIST_DATA_FLAVOR)) {
                event.acceptDrop(DnDConstants.ACTION_MOVE);
                TransferrableCouplingList tcl =
                    (TransferrableCouplingList) transferable.getTransferData(ListData.LIST_DATA_FLAVOR);
                ArrayList list = tcl.getList();

                if (tcl.getProducerOrConsumer().equalsIgnoreCase("producers")) {
                    // Create unbound producers
                    int index = this.getSelectedIndex();
                    for (int i = 0; i < list.size(); i++) {
                        Coupling coupling = new Coupling(((Producer) list.get(i)).getDefaultProducingAttribute());
                        if (index > -1) {
                            ((ModifiableListModel<Coupling>) this.getModel()).
                            insertElementAt(coupling, index + 1);
                        } else {
                            ((ModifiableListModel<Coupling>) this.getModel()).addElement((coupling));
                        }
                    }
                } else {
                    // Bind consumers to producers
                    int start = this.getSelectedIndex();
                    int index;
                    if (this.getSelectedIndex() == -1) {
                        return;
                    }
                    for (int i = 0; i < list.size(); i++) {
                        //TODO: Here and above!
                        ConsumingAttribute consumer = (((Consumer) list.get(i)).
                                getDefaultConsumingAttribute());
                        index = start + i;
                        if (index >= this.getModel().getSize()) {
                            break;
                        } else {
                            ((CouplingList) this.getModel()).bindElementAt(consumer, index);
                        }
                    }
                }
                event.getDropTargetContext().dropComplete(true);
            } else {
                event.rejectDrop();
            }
        } catch (IOException exception) {
            exception.printStackTrace();
            System.err.println("Exception" + exception.getMessage());
            event.rejectDrop();
        } catch (UnsupportedFlavorException ufException) {
            ufException.printStackTrace();
            System.err.println("Exception" + ufException.getMessage());
            event.rejectDrop();
        }
    }

    /**
     * Invoked if the usee modifies the current drop gesture.
     * @param event drop target drag.
     */
    public void dropActionChanged(final DropTargetDragEvent event) {
        LOGGER.trace("dropActionChanged(DropTargetDragEvent) called");
    }

    /**
     * A drag gesture has been initiated.
     * @param event drop target drag.
     */
    public void dragGestureRecognized(final DragGestureEvent event) {
        LOGGER.trace("dragGestureRecognized(DragGestureEvent) called");
//        Object selected = getSelectedValue();
//        if (selected != null) {
//            StringSelection text = new StringSelection(selected.toString());
//        } else {
//            System.out.println("nothing was selected");
//        }
    }

    /**
     * This message goes to DragSourceListener, informing it that the dragging
     * has ended.
     * @param event drop target drag.
     */
    public void dragDropEnd(final DragSourceDropEvent event) {
        LOGGER.trace("dragDropEnd(DragSourceDropEvent) called");
    }

    /**
     * This message goes to DragSourceListener, informing it that the dragging
     * has entered the DropSite.
     * @param event drop target drag.
     */
    public void dragEnter(final DragSourceDragEvent event) {
        LOGGER.trace("dragEnter(DragSourceDragEvent) called");
    }

    /**
     * This message goes to DragSourceListener, informing it that the dragging
     * has exited the DropSite.
     * @param event drop target drag.
     */
    public void dragExit(final DragSourceEvent event) {
        LOGGER.trace("dragExit(DragSourceEvent) called");
    }

    /**
     * This message goes to DragSourceListener, informing it that the dragging
     * is currently ocurring over the DropSite.
     * @param event drop target drag.
     */
    public void dragOver(final DragSourceDragEvent event) {
        LOGGER.trace("dragOver(DragSourceDragEvent) called");
    }

    /**
     * Invoked when the user changes the dropAction.
     * @param event drop target drag.
     */
    public void dropActionChanged(final DragSourceDragEvent event) {
        LOGGER.trace("dropActionChanged(DragSourceDragEvent) called");
    }

    static class CouplingList extends ModifiableListModel<Coupling> {
        CouplingList() {
            super();
        }
        
        CouplingList(List<Coupling> couplings) {
            super(couplings);
        }
        
        /**
         * Position to bind a producer.
         * @param producer to be bound
         * @param index of location to bind
         */
        public void bindElementAt(final ProducingAttribute producer, final int index) {
            getElementAt(index).setProducingAttribute(producer);
            this.fireContentsChanged(this, 0, getSize());
        }
        
        /**
         * Position to bind a consumer.
         * @param consumer to be bound
         * @param index of location to bind
         */
        public void bindElementAt(final ConsumingAttribute consumer, final int index) {
            getElementAt(index).setConsumingAttribute(consumer);
            this.fireContentsChanged(this, 0, getSize());
        }
    }
}
