package im.webuzz.pilet;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MIMEConfig {

	/*public */static final String configKeyPrefix = "mime";
	
	public static Map<String, String> types = new ConcurrentHashMap<String, String>();

	static {
		types.put("htm", "text/html");
		types.put("html", "text/html");
		types.put("php", "text/html");
		types.put("txt", "text/plain");
		types.put("css", "text/css");
		types.put("js", "text/javascript");
		types.put("json", "application/json");
		types.put("jpg", "image/jpeg");
		types.put("jpeg", "image/jpeg");
		types.put("gif", "image/gif");
		types.put("png", "image/png");
		types.put("bmp", "image/bmp");
		types.put("ico", "image/x-icon");
		types.put("svg", "image/svg+xml");
		types.put("webm", "video/webm");
		types.put("xml", "application/xml");
		types.put("mp4", "video/mp4");
		types.put("ogg", "audio/ogg");
		types.put("mp3", "audio/mpeg");
		types.put("m4a", "audio/x-m4a");
		types.put("swf", "application/x-shockwave-flash");
		types.put("cfg", "text/plain");
		types.put("ini", "text/plain");
		types.put("properties", "text/plain");
		types.put("props", "text/plain");
	}

}
