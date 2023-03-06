package de.mineking.musicquiz.quiz.commands.types;

import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.Quiz;

import java.util.Map;

public abstract class Command {
	protected final MusicQuiz bot;

	public Command(MusicQuiz bot) {
		this.bot = bot;
	}

	public abstract void performCommand(Quiz quiz, long user, Map<String, Object> args);
}
