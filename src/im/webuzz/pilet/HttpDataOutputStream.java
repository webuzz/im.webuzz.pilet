/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * An optimized ByteArrayOutputStream for Piled server.
 * 
 * Do not copy buffer on method #toByteArray. In such a way, we reduce the
 * copying times, which will help improving server performance.
 * 
 * Do not increase buffer size by doubling it if buffer size is greater
 * than 64k. Increasing buffer size by adding another 64k. In such a way,
 * we has more memory.
 * 
 * Piled server is not recommended as server for transferring big trunk of
 * data.
 *  
 * @author zhourenjian
 *
 */
class HttpDataOutputStream extends ByteArrayOutputStream {

	private int contentLength;
	
	private boolean error;
	
	public HttpDataOutputStream(int contentLength, boolean error) {
		super(error ? 0 : (contentLength < 8192 ? contentLength : 8192));
		this.error = error;
		this.contentLength = contentLength;
	}
	
	@Override
    public synchronized void write(byte b[], int off, int len) {
		if ((off < 0) || (off > b.length) || (len < 0) ||
	            ((off + len) > b.length) || ((off + len) < 0)) {
		    throw new IndexOutOfBoundsException();
		} else if (len == 0) {
		    return;
		}
        int newcount = count + len;
        if (!error) { // no error!
        	if (newcount > buf.length) {
        		buf = Arrays.copyOf(buf, Math.max(Math.min(buf.length <= 65536 ? (buf.length << 1) : buf.length + 65536, contentLength), newcount));
        	}
        	System.arraycopy(b, off, buf, count, len);
        }
        count = newcount;
    }

	@Override
	public synchronized byte[] toByteArray() {
		return buf;
	}

}
