package de.mineking.musicquiz.quiz;

public class Track {
	public final String url;
	public final long start;
	public long end;
	public final String title;
	public final String author;

	public Track(String url, long start, long end, String title, String author) {
		this.url = url;
		this.start = start;
		this.end = end;
		this.title = title;
		this.author = author;
	}
}
