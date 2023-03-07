package de.mineking.musicquiz.quiz.commands;

import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.Quiz;
import de.mineking.musicquiz.quiz.commands.types.Command;
import de.mineking.musicquiz.quiz.commands.types.MasterCommand;

import java.util.Map;

public class ScoreCommand extends Command implements MasterCommand {
	public ScoreCommand(MusicQuiz bot) {
		super(bot);
	}

	@Override
	public void performCommand(Quiz quiz, long user, Map<String, Object> args) {
		quiz.getMembers().get(Long.parseLong((String) args.get("user"))).points.addAndGet((int) (double) args.get("delta"));
		quiz.sendUpdate();
	}
}
