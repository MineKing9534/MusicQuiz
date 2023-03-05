package de.mineking.musicquiz.quiz;

import de.mineking.musicquiz.quiz.remote.RemoteData;
import io.javalin.websocket.WsContext;

import java.util.concurrent.atomic.AtomicInteger;

public class MemberData {
	public final AtomicInteger points;
	public WsContext remote;

	public MemberData(int points) {
		this.points = new AtomicInteger(points);
	}

	public void send(RemoteData data) {
		if(remote == null) {
			return;
		}

		remote.sendAsClass(data, data.getClass());
	}
}
