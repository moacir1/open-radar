package de.knewcleus.radar.ui.rpvd;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import de.knewcleus.radar.autolabel.Label;
import de.knewcleus.radar.autolabel.LabeledObject;
import de.knewcleus.radar.ui.Palette;
import de.knewcleus.radar.ui.labels.LabelElementContainer;

public abstract class AbstractVehicleLabel extends LabelElementContainer implements Label, IVehicleLabel { 
	protected final LabelElementContainer labelLines[];
	protected static final double minLabelDist=10;
	protected static final double maxLabelDist=100;
	protected static final double meanLabelDist=(minLabelDist+maxLabelDist)/2.0;
	protected static final double labelDistRange=(maxLabelDist-minLabelDist);

	public AbstractVehicleLabel(int maxLabelLines) {
		labelLines=new LabelElementContainer[maxLabelLines];
	}
	
	protected void clearAllLines() {
		for (int i=0;i<labelLines.length;i++) {
			labelLines[i].removeAll();
		}
	}
	
	@Override
	public boolean isOpaque() {
		return getVehicleSymbol().getVehicle().isSelected();
	}
	
	@Override
	public Color getForegroundColor() {
		return (getVehicleSymbol().getVehicle().isSelected()?
				getSelectedTextColor():getNormalTextColor());
	}
	
	@Override
	public Color getBackgroundColor() {
		return (getVehicleSymbol().getVehicle().isSelected()?
				getSelectedBackgroundColor():Palette.TRANSPARENT);
	}

	@Override
	public LabeledObject getAssociatedObject() {
		return getVehicleSymbol();
	}
	
	@Override
	public Point2D getHookPosition() {
		final Rectangle2D labelBounds=getBounds2D();
		
		// TODO: properly calculate the hook position
		return new Point2D.Double(labelBounds.getCenterX(), labelBounds.getCenterY());
	}
	
	@Override
	public void setCentroidPosition(double x, double y) {
		final Rectangle2D symbolBounds=getAssociatedObject().getBounds2D();
		
		double dx=x-symbolBounds.getCenterX();
		double dy=y-symbolBounds.getCenterY();
		
		final double len=Math.sqrt(dx*dx+dy*dy);
		
		if (len<1) {
			return;
		} else if (len<minLabelDist) {
			dx*=minLabelDist/len;
			dy*=minLabelDist/len;
		} else if (len>maxLabelDist) {
			dx*=maxLabelDist/len;
			dy*=maxLabelDist/len;
		}

		x=symbolBounds.getCenterX()+dx;
		y=symbolBounds.getCenterY()+dy;
		
		final Rectangle2D labelBounds=getBounds2D();
		super.setPosition(x-labelBounds.getWidth()/2.0, y-labelBounds.getHeight()/2.0);
	}
	
	public abstract Color getNormalTextColor();
	public abstract Color getSelectedTextColor();
	public abstract Color getSelectedBackgroundColor();

	public abstract IVehicleSymbol getVehicleSymbol();

	public abstract void updateLabelContents();
}