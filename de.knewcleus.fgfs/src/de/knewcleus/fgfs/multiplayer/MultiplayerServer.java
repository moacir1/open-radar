package de.knewcleus.fgfs.multiplayer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.knewcleus.fgfs.Units;
import de.knewcleus.fgfs.location.Position;
import de.knewcleus.fgfs.location.Vector3D;
import de.knewcleus.fgfs.multiplayer.protocol.ChatMessage;
import de.knewcleus.fgfs.multiplayer.protocol.MultiplayerPacket;
import de.knewcleus.fgfs.multiplayer.protocol.PositionMessage;

public class MultiplayerServer<T extends Player> extends AbstractMultiplayerEndpoint<T> {
	public static final int STANDARD_PORT=5000;
	public double maxDistance=100.0*Units.NM;
	protected List<MultiplayerPacket> queuedPackets=new ArrayList<MultiplayerPacket>();

	public MultiplayerServer(IPlayerRegistry<T> playerRegistry) throws IOException {
		super(playerRegistry,getStandardPort());
	}
	
	protected static int getStandardPort() {
		return Integer.getInteger("de.knewcleus.fgfs.multiplayer.server.port",STANDARD_PORT);
	}

	public MultiplayerServer(IPlayerRegistry<T> playerRegistry, int port) throws IOException {
		super(playerRegistry, port);
	}

	@Override
	protected void processPacket(T player, MultiplayerPacket mppacket) throws MultiplayerException {
		Position senderPosition;
		if (mppacket.getMessage() instanceof PositionMessage) {
			PositionMessage positionMessage=(PositionMessage)mppacket.getMessage();
			player.updatePosition(System.currentTimeMillis(),positionMessage);
			
			senderPosition=positionMessage.getPosition();
		} else {
			senderPosition=player.getCartesianPosition();
		}
		
		if (!(mppacket.getMessage() instanceof ChatMessage)) {
			/* Do not forward anything but chat messages from observers */
			if (mppacket.getCallsign().startsWith("obs"))
				return;
		}
		
		/* Send back to all local clients */
		synchronized (queuedPackets) {
			synchronized (playerRegistry) {
				for (T otherPlayer: playerRegistry.getPlayers()) {
					if (!player.isLocalPlayer())
						continue;
					
					for (MultiplayerPacket queuedPacket: queuedPackets)
						sendPacket(otherPlayer, queuedPacket);
					
					/* Don't send to sender itself */
					if (player==otherPlayer)
						continue;
					
					/* Don't send to players out of range */
					Position otherPos=otherPlayer.getCartesianPosition();
					Vector3D delta=otherPos.subtract(senderPosition);
					if (delta.getLength()>maxDistance)
						continue;
					
					sendPacket(otherPlayer, mppacket);
				}
			}
			queuedPackets.clear();
		}
		
		// TODO: send to relays
	}
	
	public void broadcastChatMessage(String text) {
		ChatMessage message=new ChatMessage("server:"+text);
		MultiplayerPacket packet=new MultiplayerPacket("*server*",message);
		
		synchronized (queuedPackets) {
			queuedPackets.add(packet);
		}
	}
	
	@Override
	protected void newPlayerLogon(T player) throws MultiplayerException {
		super.newPlayerLogon(player);
		
		ChatMessage message=new ChatMessage("server: Welcome to the FlightGear Multiplayer server at "+datagramSocket.getLocalSocketAddress()+":"+datagramSocket.getLocalPort());
		MultiplayerPacket packet=new MultiplayerPacket("*server*",message);
		sendPacket(player, packet);
		
		broadcastChatMessage("server:"+player.getCallsign()+" came online");
	}
}
