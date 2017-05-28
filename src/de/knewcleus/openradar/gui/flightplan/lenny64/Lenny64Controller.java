/**
 * Copyright (C) 2014-2016 Wolfram Wagner
 * 
 * This file is part of OpenRadar.
 * 
 * OpenRadar is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * 
 * OpenRadar is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with OpenRadar. If not, see
 * <http://www.gnu.org/licenses/>.
 * 
 * Diese Datei ist Teil von OpenRadar.
 * 
 * OpenRadar ist Freie Software: Sie können es unter den Bedingungen der GNU General Public License, wie von der Free
 * Software Foundation, Version 3 der Lizenz oder (nach Ihrer Option) jeder späteren veröffentlichten Version,
 * weiterverbreiten und/oder modifizieren.
 * 
 * OpenRadar wird in der Hoffnung, dass es nützlich sein wird, aber OHNE JEDE GEWÄHRLEISTUNG, bereitgestellt; sogar ohne
 * die implizite Gewährleistung der MARKTFÄHIGKEIT oder EIGNUNG FÜR EINEN BESTIMMTEN ZWECK. Siehe die GNU General Public
 * License für weitere Details.
 * 
 * Sie sollten eine Kopie der GNU General Public License zusammen mit diesem Programm erhalten haben. Wenn nicht, siehe
 * <http://www.gnu.org/licenses/>.
 */
package de.knewcleus.openradar.gui.flightplan.lenny64;

import java.awt.event.MouseEvent;
import java.util.List;

import de.knewcleus.openradar.gui.GuiMasterController;
import de.knewcleus.openradar.gui.contacts.FlightPlanDialog;
import de.knewcleus.openradar.gui.contacts.GuiRadarContact;
import de.knewcleus.openradar.gui.flightplan.FlightPlanData;
import de.knewcleus.openradar.gui.flightplan.FlightPlanData.FlightPlanStatus;

public class Lenny64Controller {

    private final GuiMasterController master;
    private final FlightPlanDialog dialog;
    private final Lenny64FpSelectionDialog fpSelectionDialog;

    public Lenny64Controller(GuiMasterController master, FlightPlanDialog dialog) {
        this.master = master;
        this.dialog = dialog;

        fpSelectionDialog = new Lenny64FpSelectionDialog(this, dialog);
    }

    public boolean isDialogOpen() {
        return fpSelectionDialog.isVisible();
    }

    public void closeFpSelectionDialog() {
        fpSelectionDialog.setVisible(false);
    }

    /**
     * Retrieves the list of possibly matching flightplans and displays a dialog to select the one you want to import.
     * 
     * @param callsign
     */
    public synchronized void downloadFlightPlansFor(MouseEvent e, String callsign) {
//        dialog.saveData();
        GuiRadarContact c = master.getRadarContactManager().getContactFor(callsign);
        List<FlightPlanData> existingFPs = Lenny64FlightplanServerConnector.checkForFlightplan(c);
        if (existingFPs.isEmpty()) {
            dialog.setLennyButtonText("none found");
        } else {
            dialog.setLennyButtonText("(please select)");
            fpSelectionDialog.show(callsign, existingFPs);
            fpSelectionDialog.setLocation(e);
        }
    }

    /**
     * Merges the data from lenny64 into the existing flightplan
     */
    public synchronized void mergeFlightplans(FlightPlanData lenny64Flightplan) {
        GuiRadarContact c = master.getRadarContactManager().getContactFor(lenny64Flightplan.getCallsign());
        synchronized (c) {
            c.setFlightPlan(lenny64Flightplan);
            dialog.setLennyButtonText("(loaded)");
            dialog.extUpdateUI(c, true);
        }
    }

    public synchronized void openFlightPlan(GuiRadarContact contact) {
		contact.getFlightPlan().setFpStatus(FlightPlanStatus.OPEN.toString());
		Lenny64FlightplanServerConnector.openFlightPlan(master, contact);
    }

    public synchronized void closeFlightPlan(GuiRadarContact contact) {
    	Lenny64FlightplanServerConnector.closeFlightPlan(master, contact);
    }

}
