package de.knewcleus.radar.vessels;

import java.util.Set;

public interface ITrackUpdateListener {
	public abstract void tracksUpdated(Set<Track> targets);
	public abstract void trackLost(Track target);
}
