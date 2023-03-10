package de.mineking.musicquiz.main;

import com.google.gson.Gson;

import java.io.FileReader;

public class Config {
	public String token;
	public String url;

	public String spotifyClientId;
	public String spotifyClientSecret;

	public static Config readFromFile(String path) throws Exception {
		try(FileReader r = new FileReader(path)) {
			return new Gson().fromJson(r, Config.class);
		} catch(Exception e) {
			throw new Exception("Failed to read config file from '" + path + "' - Please check for correct syntax", e);
		}
	}
}
