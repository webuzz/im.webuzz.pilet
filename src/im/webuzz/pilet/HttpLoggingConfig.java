package im.webuzz.pilet;

public class HttpLoggingConfig {

	/**
	 * Server logging base path.
	 */
	public static String loggingBase;
	/**
	 * Flush logging, if buffer block reaches this size between 1s interval.
	 */
	public static int loggingBufferBlock = 65536;

}
