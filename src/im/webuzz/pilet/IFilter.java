/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

/**
 * Filter is designed for returning quick response without requiring threads.
 * 
 * If request is responded, server will not put request into thread pool. In
 * such a way, server may be much more responsible.
 * 
 * Keep in mind that filter is not suitable for do CPU-heavy or IO-heavy jobs.
 * Or it will blocked all other connecting sockets.
 * 
 * @author zhourenjian
 * @see IPilet
 */
public interface IFilter {
	
	/**
	 * Check request to see if it is for this filter or not and generate a
	 * response if it is true or just return false.
	 * 
	 * Usually, HTTP request's URL will be used to check whether this
	 * request is for this pilet or not.
	 * 
	 * If you just want to monitor all requests, try configure this pilet
	 * as the first pilet in the server's pilet list and try to keep a
	 * record of passing requests and return false.
	 * 
	 * You can mark your request as comet request and return true directly
	 * and output your response by other threads. You need to mark request
	 * as non-comet request before you closing it.
	 * 
	 * Keep in mind that filter is not suitable for do CPU-heavy or IO-heavy
	 * jobs. Or it will blocked all other connecting sockets.
	 * 
	 * Also keep in mind that if true is returned and no response is sent, or
	 * false is returned with response is already sent. The server might
	 * failed to deal following requests from the affected users.
	 * 
	 * @param req HTTP request object
	 * @param resp HTTP response object
	 * @return If request is already dealt, return true, otherwise
	 * return false
	 */
	public boolean filter(HttpRequest req, HttpResponse resp);

}
