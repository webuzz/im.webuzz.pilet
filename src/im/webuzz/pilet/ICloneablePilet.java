/*******************************************************************************
 * Copyright (c) 2010 - 2013 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

public interface ICloneablePilet extends IPilet {

	/**
	 * Clone itself to a new IPilet instance.
	 * 
	 * For normal IPilet, only one IPilet instance is used to serve all
	 * requests. In case each IPilet instance needs its own environment,
	 * clone itself to a new IPilet instance and server the given request.
	 * 
	 * @return Cloned IPilet instance
	 */
	public IPilet clonePilet();
	
}
