package de.mineking.musicquiz.quiz.commands;

import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.Quiz;
import de.mineking.musicquiz.quiz.commands.types.Command;
import de.mineking.musicquiz.quiz.commands.types.MasterCommand;

import java.util.Map;

public class FreeCommand extends Command implements MasterCommand {
	public FreeCommand(MusicQuiz bot) {
		super(bot);
	}

	@Override
	public void performCommand(Quiz quiz, long user, Map<String, Object> args) {
		if(args.containsKey("user")) {
			quiz.getIgnored().remove(Long.parseLong((String) args.get("user")));
		}

		else {
			quiz.getIgnored().clear();
		}

		quiz.sendUpdate();
	}
}
