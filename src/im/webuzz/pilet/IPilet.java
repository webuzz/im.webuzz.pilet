/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

public interface IPilet {
	
	/**
	 * Check request to see if it is for this pilet or not and generate a
	 * reponse if it is true or just return false.
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
	 * Keep in mind that if true is returned and no response is sent, or
	 * false is returned with response is already sent. The server might
	 * failed to deal following requests from the affected users.
	 * 
	 * @param req HTTP request object
	 * @param resp HTTP response object
	 * @return If response is already sent, return true, otherwise
	 * return false.
	 */
	public boolean service(HttpRequest req, HttpResponse resp);

}
