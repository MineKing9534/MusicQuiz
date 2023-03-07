package de.mineking.musicquiz.quiz.commands;

import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.Quiz;
import de.mineking.musicquiz.quiz.commands.types.CachingCommand;
import de.mineking.musicquiz.quiz.commands.types.Command;
import de.mineking.musicquiz.quiz.remote.EventData;

import java.time.Duration;
import java.util.Map;

public class GuessCommand extends Command implements CachingCommand {
	public GuessCommand(MusicQuiz bot) {
		super(bot);
	}

	@Override
	public void performCommand(Quiz quiz, long user, Map<String, Object> args) {
		quiz.setGuesser(quiz.getChannel().getGuild().getMemberById(user));
		quiz.sendUpdate();
		quiz.sendToMember(quiz.getGuesser(), new EventData(EventData.Action.GUESS).put("time", Quiz.guessDuration.toMillis()));
	}

	@Override
	public Duration getCacheDuration() {
		return Duration.ofSeconds(1);
	}
}
