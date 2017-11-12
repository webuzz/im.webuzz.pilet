/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

/**
 * Configuration for HTTP server.
 * 
 * @author zhourenjian
 *
 */
public class HttpConfig {

	/**
	 * Only start to serve gzip encoding for content larger that this size.
	 */
	public static int gzipStartingSize = 1024;
	
	/**
	 * Piled server do not accept post with data larger than given limit.
	 * Piled server is not optimized for uploading a large file.
	 */
	public static long maxPost = 0x1000000L; // 1M
	
	/**
	 * Default request buffer size for upload HTTP request.
	 */
	public static int defaultBufferSize = 8192; // 8k
	
	/**
	 * Incremental buffer size for upload HTTP request.
	 */
	public static int incrementalBufferSize = 65535; // 64k
	
	/**
	 * How many requests on one keep-alive connection before being closed.
	 */
	public static int maxKeepAlive = 300;
	/**
	 * Timeout for keep-alive HTTP requests.
	 */
	public static int aliveTimeout = 30; // in second

	/**
	 * Whether send server signature "Server: ####" or not (null, empty)
	 */
	public static String serverSignature = "piled";
	
	/**
	 * Use direct remote IP, no proxy IP for remoteIP field in HttpRequest object.
	 * Ignoring X-Real-IP and X-Forwarded-For header added by proxy server.
	 */
	public static boolean useDirectRemoteIP = false;

	/**
	 * Ignore private forwarded IP, like 10.x.x.x.
	 */
	public static boolean ignoreIntranetForwardedIP = true;
	
}
