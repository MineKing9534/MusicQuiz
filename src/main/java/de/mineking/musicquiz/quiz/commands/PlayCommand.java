package de.mineking.musicquiz.quiz.commands;

import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.Quiz;
import de.mineking.musicquiz.quiz.commands.types.Command;
import de.mineking.musicquiz.quiz.commands.types.MasterCommand;

import java.util.Map;

public class PlayCommand extends Command implements MasterCommand {
	public PlayCommand(MusicQuiz bot) {
		super(bot);
	}

	@Override
	public void performCommand(Quiz quiz, long user, Map<String, Object> args) {
		quiz.playTrack((int) (double) args.getOrDefault("delta", 0.0));
	}
}
