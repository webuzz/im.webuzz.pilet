/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

/**
 * Piled wrapping is designed to do jobs before Piled server starting up
 * or after server is closed.
 * 
 * Keep in mind that {@link IPiledWrapping#afterClosed(IPiledServer)} might
 * not be invoked if server is not closing down correctly or being killed.
 * 
 * @author zhourenjian
 */
public interface IPiledWrapping {

	/**
	 * Initialize system or start new thread to do interval jobs.
	 * 
	 * @param server
	 */
	public void beforeStartup(IPiledServer server);
	
	/**
	 * Release resources or close opened files or connections.
	 * 
	 * @param server
	 */
	public void afterClosed(IPiledServer server);
	
}
