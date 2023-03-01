package de.mineking.musicquiz.quiz;

import io.javalin.websocket.WsContext;

import java.util.concurrent.atomic.AtomicInteger;

public class MemberData {
	public final AtomicInteger points = new AtomicInteger();
	public WsContext remote;
}
