package de.mineking.musicquiz.quiz.remote;

public abstract class RemoteData {
	public enum Type {
		UPDATE, EVENT
	}

	public Type type;

	public RemoteData(Type type) {
		this.type = type;
	}
}
