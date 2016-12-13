/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

/**
 * Response for parsing HTTP request.
 * 
 * This response is different from HTTP response. This response is
 * generated in parsing HTTP request.
 * 
 * ATTENTION: This is an internal class.
 * 
 * @author zhourenjian
 *
 */
public class HttpQuickResponse {

	public int code;
	public String contentType;
	public String content;
	
	public HttpQuickResponse(int code) {
		super();
		this.code = code;
	}

	public HttpQuickResponse(String contentType, String content) {
		super();
		this.code = 200;
		this.contentType = contentType;
		this.content = content;
	}

}
