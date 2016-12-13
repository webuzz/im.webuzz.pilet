/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

import java.nio.channels.SocketChannel;

/**
 * Server data event for notifying server to send data or do other jobs.
 * 
 * @author zhourenjian
 *
 */
public class HttpResponse {
	public IPiledWorker worker;
	public SocketChannel socket;
}