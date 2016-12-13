/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * Piled server interface.
 * 
 * @author zhourenjian
 */
public interface IPiledServer {
	
	/**
	 * Send out data to given socket.
	 * 
	 * @param socket
	 * @param data
	 */
	public void send(SocketChannel socket, byte[] data);
	
	/**
	 * Send out data to given socket.
	 * 
	 * @param socket
	 * @param data
	 * @param offset
	 * @param length
	 */
	public void send(SocketChannel socket, byte[] data, int offset, int length);

	/**
	 * Return server NIO selector.
	 * 
	 * ATTENTION:
	 * 1. Must not do network jobs on returned object.
	 * 2. Only suggested to use this feature for applications of server
	 * management.
	 * 
	 * @return
	 */
	public Selector getSelector();
	
	/**
	 * Return the TCP port on which server is listening on.
	 * 
	 * @return
	 */
	public int getPort();
	
	/**
	 * Return whether server is running on SSL layer.
	 * 
	 * @return
	 */
	public boolean isSSLEnabled();

	/**
	 * Return whether server is running or not.
	 * @return
	 */
	public boolean isRunning();
	
	/**
	 * Stop Piled worker.
	 * As there is only one worker for Piled server, stopping worker
	 * will also stop the whole Piled server.
	 * 
	 * ATTENTION: Only suggested to use this feature for applications of server
	 * management.
	 * 
	 */
	public void stop();	

	/**
	 * Set class loader for dynamically class loading.
	 * @param loader
	 */
	public void setSimpleClassLoader(ClassLoader loader);

	/**
	 * Reload pilet, filter or wrapper for the server.
	 * 
	 * @param clazzName
	 * @param path
	 * @param tag
	 */
	public void reloadClasses(String[] clazzNames, String path, String tag);

	/**
	 * Return how many network operations has been processed.
	 * 
	 * ATTENTION: Only suggested to use this feature for applications of
	 * server management.
	 * 
	 * @return
	 */
	public long getProcessingIOs();

	/**
	 * Return how many HTTP requests has been processed.
	 * 
	 * ATTENTION: Only suggested to use this feature for applications of server
	 * management.
	 * 
	 * @return
	 */
	public long getTotalRequests();
	
	/**
	 * Return how many error request has been processed.
	 * 
	 * ATTENTION: Only suggested to use this feature for applications of server
	 * management.
	 * 
	 * @return
	 */
	public long getErrorRequests();
	
	/**
	 * Return all active requests.
	 * 
	 * ATTENTION: Only suggested to use this feature for applications of server
	 * management.
	 * 
	 * @return
	 */
	public HttpRequest[] getActiveRequests();

	/**
	 * Return all existed servers.
	 * 
	 * @return
	 */
	public IPiledServer[] getAllServers();

}
