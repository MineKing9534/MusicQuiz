package de.mineking.musicquiz.quiz.commands;

import de.mineking.musicquiz.main.MusicQuiz;
import de.mineking.musicquiz.quiz.Quiz;
import de.mineking.musicquiz.quiz.commands.types.Command;
import org.eclipse.jetty.websocket.core.CloseStatus;

import java.util.Map;

public class LeaveCommand extends Command {
	public LeaveCommand(MusicQuiz quiz) {
		super(quiz);
	}

	@Override
	public void performCommand(Quiz quiz, long user, Map<String, Object> args) {
		quiz.getMembers().remove(user);
		quiz.sendUpdate();

		bot.server.gateway.getUserConnections(user).forEach(context -> context.closeSession(CloseStatus.NORMAL, "Left Quiz"));
	}
}
