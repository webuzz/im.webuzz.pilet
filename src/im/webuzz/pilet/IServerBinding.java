/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

/**
 * Server binding is for a Pilet to get access to Piled server object.
 * 
 * ATTENTION: Only suggested to use this feature for applications of server
 * management.
 * 
 * @author zhourenjian
 *
 */
public interface IServerBinding {

	public void binding(IPiledServer server);
	
}
