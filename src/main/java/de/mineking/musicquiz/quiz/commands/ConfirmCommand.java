package de.mineking.musicquiz.quiz.commands;

import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.Quiz;
import de.mineking.musicquiz.quiz.Track;
import de.mineking.musicquiz.quiz.commands.types.Command;
import de.mineking.musicquiz.quiz.commands.types.MasterCommand;
import de.mineking.musicquiz.quiz.remote.EventData;

import java.util.Map;

public class ConfirmCommand extends Command implements MasterCommand {
	public ConfirmCommand(MusicQuiz bot) {
		super(bot);
	}

	@Override
	public void performCommand(Quiz quiz, long user, Map<String, Object> args) {
		Track track = quiz.getTracks().get(quiz.getPosition());

		quiz.sendToAll(new EventData(EventData.Action.SOLUTION)
				.put("url", track.url + "?t=" + track.start)
				.put("title", track.title)
				.put("author", track.author)
		);
	}
}
