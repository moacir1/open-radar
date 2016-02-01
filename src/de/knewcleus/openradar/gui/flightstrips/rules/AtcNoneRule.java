package de.knewcleus.openradar.gui.flightstrips.rules;

import java.util.ArrayList;

import org.jdom2.Element;

import de.knewcleus.openradar.gui.flightplan.FlightPlanData;
import de.knewcleus.openradar.gui.flightstrips.FlightStrip;
import de.knewcleus.openradar.gui.flightstrips.LogicManager;

public class AtcNoneRule extends AbstractRule {

	private final boolean isAtcNone;
	
	public AtcNoneRule(boolean isAtcNone) {
		this.isAtcNone = isAtcNone;
	}
	
	public AtcNoneRule(Element element, LogicManager logic) {
		this.isAtcNone = Boolean.valueOf(element.getAttributeValue("isatcnone"));
	}
	
	@Override
	public boolean isAppropriate(FlightStrip flightstrip) {
		FlightPlanData flightplan = flightstrip.getContact().getFlightPlan();
		return (flightplan != null) && (flightplan.isOwnedbyNobody() == isAtcNone);
	}

	@Override
	public ArrayList<String> getRuleText() {
		ArrayList<String> result = new ArrayList<String>();
		result.add("contact is " + (isAtcNone ? "" : "not") + " uncontrolled.");
		return result;
	}

	// --- IDomElement ---
	
	@Override
	public void putAttributes(Element element) {
		super.putAttributes(element);
		element.setAttribute("isatcnone", String.valueOf(isAtcNone));
	}

}