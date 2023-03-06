package de.mineking.musicquiz.quiz.commands;

import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.Quiz;
import de.mineking.musicquiz.quiz.commands.types.Command;
import de.mineking.musicquiz.quiz.commands.types.MasterCommand;

import java.util.Map;

public class ExtendCommand extends Command implements MasterCommand {
	public ExtendCommand(MusicQuiz bot) {
		super(bot);
	}

	@Override
	public void performCommand(Quiz quiz, long user, Map<String, Object> args) {
		if(args.containsKey("delta")) {
			quiz.getTracks().get(0).end += (int) args.get("delta");
		}

		else if(args.containsKey("value")) {
			quiz.getTracks().get(0).end = (int) args.get("value");
		}

		quiz.sendUpdate();
	}
}
