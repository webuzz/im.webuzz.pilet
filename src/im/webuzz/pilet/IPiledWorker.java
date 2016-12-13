/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

import java.nio.channels.SocketChannel;

/**
 * Piled worker interface.
 * 
 * @author zhourenjian
 * @see IServerBinding
 */
public interface IPiledWorker {
	
	/**
	 * Get Pile server.
	 * @return
	 */
	public IPiledServer getServer();
	
	/**
	 * Put request in closing waiting pool.
	 * 
	 * @param socket
	 * @param request
	 */
	public void poolingRequest(SocketChannel socket, HttpRequest request);

	/**
	 * Find next request in chain and make response.
	 * 
	 * @param request
	 * @param response
	 */
	public void chainingRequest(HttpRequest request, HttpResponse response);
	
	/**
	 * Remove given socket.
	 * @param socket
	 */
	public void removeSocket(SocketChannel socket);

}
