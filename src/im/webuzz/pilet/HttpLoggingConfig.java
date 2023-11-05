package im.webuzz.pilet;

import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
	
	/**
	 * Ignore logging for given hosts.
	 */
	public static Set<String> ignoringHosts = null;

	/**
	 * If hosts are not known, requests will be logged into an 
	 * "unknown.host" log file.
	 * 
	 * If knownHosts is null, requests will be logged into their
	 * host name prefixed files. 
	 */
	public static Set<String> knownHosts = null;

	public static String unknowHostLogPrefix = "unknown.hosts";

	/**
	 * Will be updated by "logFormat".
	 * By default, it is nginx's combined log format.
	 */
	public static Object[] loggingSegments = new Object[] {
			ServerVariable.remote_addr, " - ", ServerVariable.remote_user,
			" [", ServerVariable.time_local, "] \"", ServerVariable.request, "\" ",
			ServerVariable.status, " ", ServerVariable.bytes_sent,
			" \"", ServerVariable.http_referer, "\" \"", ServerVariable.http_user_agent, "\""
	};
	
	public static void update(Properties prop) {
		String p = prop.getProperty("logFormat");
		if (p != null && p.length() > 0) {
			loggingSegments = parseLogFormat(p);
		}
	}
	
	public static Object[] parseLogFormat(String format) {
		ArrayList<Object> list = new ArrayList<Object>();
		int length = format.length();
		boolean varNameStarted = false;
		StringBuilder varBuilder = new StringBuilder();
		StringBuilder separatorBuilder = new StringBuilder();
		for (int i = 0; i < length; i++) {
			char c = format.charAt(i);
			if (c == '$') {
				varNameStarted = true;
				checkSeparator(list, separatorBuilder);
				continue;
			}
			if (varNameStarted && (c == '_'
					|| '0' <= c && c <= '9'
					|| 'A' <= c && c <= 'Z'
					|| 'a' <= c && c <= 'z')) {
				varBuilder.append(c);
				continue;
			}
			varNameStarted = false;
			checkVariable(list, varBuilder, separatorBuilder);
			separatorBuilder.append(c);
		}
		checkVariable(list, varBuilder, separatorBuilder);
		checkSeparator(list, separatorBuilder);
		return list.toArray(new Object[list.size()]);
	}

	private static void checkVariable(ArrayList<Object> list, StringBuilder varBuilder, StringBuilder separatorBuilder) {
		int varLen = varBuilder.length();
		if (varLen > 0) {
			try {
				ServerVariable v = ServerVariable.valueOf(varBuilder.toString());
				list.add(v);
			} catch (Exception e) {
				e.printStackTrace();
				separatorBuilder.append('-');
			}
			varBuilder.delete(0, varLen);
		}
	}

	private static void checkSeparator(ArrayList<Object> list, StringBuilder separatorBuilder) {
		int sepLen = separatorBuilder.length();
		if (sepLen > 0) {
			list.add(separatorBuilder.toString());
			separatorBuilder.delete(0, sepLen);
		}
	}
}
