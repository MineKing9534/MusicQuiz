package de.mineking.musicquiz.quiz;

import de.mineking.musicquiz.quiz.remote.RemoteData;
import io.javalin.websocket.WsContext;

import java.util.concurrent.atomic.AtomicInteger;

public class MemberData {
	public final AtomicInteger points = new AtomicInteger();
	public WsContext remote;

	public void send(RemoteData data) {
		if(remote == null) {
			return;
		}

		remote.sendAsClass(data, data.getClass());
	}
}
