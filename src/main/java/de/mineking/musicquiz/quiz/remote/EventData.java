package de.mineking.musicquiz.quiz.remote;

import java.util.HashMap;
import java.util.Map;

public class EventData extends RemoteData {
	public enum Action {
		LOGIN, ERROR, WAIT, GUESS, SOLUTION
	}

	public final Action action;
	public final Map<String, Object> data;

	public EventData(Action action, Map<String, Object> data) {
		super(Type.EVENT);

		this.action = action;
		this.data = data;
	}

	public EventData(Action action) {
		this(action, new HashMap<>());
	}

	public EventData put(String name, Object value) {
		data.put(name, value);

		return this;
	}
}
