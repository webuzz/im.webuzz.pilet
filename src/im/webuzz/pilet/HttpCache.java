/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

/**
 * HTTP caching constants.
 * 
 * @author zhourenjian
 *
 */
public class HttpCache {
	
	public static final long HALF_HOUR = 1800L * 1000;
	
	public static final long ONE_HOUR = 3600L * 1000;
	
	public static final long HALF_DAY = 12L * 3600 * 1000;

	public static final long ONE_DAY = 24L * 3600 * 1000;
	
	public static final long HALF_WEEK = 7L * 12 * 3600 * 1000;
	
	public static final long ONE_WEEK = 7L * 24 * 3600 * 1000;
	
	public static final long NEVER_EXPIRED = 5L * 365 * 24 * 3600 * 1000;

}
