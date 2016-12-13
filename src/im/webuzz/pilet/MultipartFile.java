/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

/**
 * Uploaded file object from posted HTTP data.
 * 
 * @author zhourenjian
 *
 */
public class MultipartFile {

	/**
	 * File name.
	 */
	public String name;
	
	/**
	 * File content.
	 */
	public byte[] content;
	
}
