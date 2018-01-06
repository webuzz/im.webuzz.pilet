package im.webuzz.pilet;

import java.util.Map;

public class HttpLoggingConfig {

	public static final String configKeyPrefix = "logging";
	
	/**
	 * Server logging base path.
	 */
	public static String basePath;
	/**
	 * Flush logging, if buffer block reaches this size between 1s interval.
	 */
	public static int bufferBlock = 65536;

	/**
	 * Use different domain, basing given host mappings.
	 */
	public static Map<String, String> hostMappings = null;
	
}
