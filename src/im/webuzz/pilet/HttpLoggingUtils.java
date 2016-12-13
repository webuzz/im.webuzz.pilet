/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Log HTTP requests.
 * 
 * @author zhourenjian
 *
 */
public class HttpLoggingUtils {

	private static Object lock = new Object();
	
	private static boolean loggingStarted = false;
	
	private static boolean running = false;
	
	private static Map<String, StringBuffer> allLogs = new ConcurrentHashMap<String, StringBuffer>();

	private static String lineSeparator = System.getProperty("line.separator");
	
	private static String lastDateStr = null;
	private static long lastLoggedDate = 0;
	
	/*
	 * We use stand-alone thread to log all logs in very 10 seconds.
	 */
	public static void addLogging(String host, HttpRequest req, int responseCode, long responseLength) {
		if (host == null || host.length() == 0) {
			host = "localhost"; // Not knowing the host name from HTTP request
		}
		if (host.indexOf("..") != -1 || host.indexOf('\\') != -1 || host.indexOf('/') != -1) {
			host = "hacker"; // Bad request
		}
		StringBuffer buffer = allLogs.get(host); // each host will has a StringBuffer object.
		if (!loggingStarted || buffer == null) {
			synchronized (lock) {
				if (!loggingStarted) {
					Thread thread = new Thread("Piled Logger") {
						public void run() {
							runLoggingLoop();
						}
					};
					thread.setDaemon(true);
					thread.start();
					loggingStarted = true;
				}
				buffer = allLogs.get(host);
				if (buffer == null) {
					buffer = new StringBuffer(8096);
					allLogs.put(host, buffer);
				}
			} // end of synchronized blocked
		} // end of if
		StringBuilder builder = new StringBuilder(256);
		String dateStr = null;
		if (lastLoggedDate > 0 && Math.abs(req.created - lastLoggedDate) < 500) {
			dateStr = lastDateStr;
		} else {
			// Use DateUtils for thread safe SimpleDateFormat
			dateStr = DateUtils.formatDate(new Date(req.created), "dd/MMM/yyyy:HH:mm:ss Z");
			lastDateStr = dateStr;
			lastLoggedDate = req.created;
		}
		builder.append(req.remoteIP);
		builder.append(" - - [");
		builder.append(dateStr);
		builder.append("] \"");
		builder.append(req.method);
		builder.append(" ");
		builder.append(req.url);
		if ("GET".equals(req.method) && req.requestData != null) {
			String more = null;
			if (req.requestData instanceof String) {
				more = (String) req.requestData;
			} else if (req.requestData instanceof byte[]) {
				more = new String((byte []) req.requestData);
			}
			if (more != null) {
				int idx = more.indexOf("WLL");
				if (idx == -1) {
					builder.append("?");
					builder.append(more);
				} else if (idx == 0) { // Simple RPC/Pipe
					builder.append("?");
					int index = more.indexOf('#', 3);
					if (index != -1) {
						builder.append(more.substring(Math.min(6, more.length()), index));
					} else {
						builder.append(more.substring(0, Math.min(48, more.length())));
					}
					builder.append("...");
				}
			}
		}

		if (req.v11) {
			builder.append(" HTTP/1.1\" ");
		} else {
			builder.append(" HTTP/1.0\" ");
		}
//		builder.append(" HTTP/1.");
//		builder.append(req.v11 ? "1" : "0");
//		builder.append("\" ");
		builder.append(responseCode);
		builder.append(" ");
		builder.append(responseLength);
		builder.append(" \"");
		builder.append(req.referer == null ? "-" : req.referer);
		builder.append("\" \"");
		builder.append(req.userAgent == null ? "-" : req.userAgent);
		builder.append("\"");
		builder.append(lineSeparator);
		//synchronized (buffer) {
			buffer.append(builder);
		//}
		
	}
	
	static void runLoggingLoop() {
		running = true;
		SimpleDateFormat logFileDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Map<String, FileOutputStream> allFilestreams = new HashMap<String, FileOutputStream>();
		while (running) {
			int count = 10;
			while (running && count-- > 0) {
				for (int k = 0; k < 20; k++) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					boolean bufferNeedFlushing = false;
					for (Iterator<StringBuffer> itr = allLogs.values().iterator(); itr.hasNext();) {
						StringBuffer buffer = (StringBuffer) itr.next();
						if (buffer.length() > HttpLoggingConfig.loggingBufferBlock) {
							bufferNeedFlushing = true;
							break;
						}
					}
					if (bufferNeedFlushing) {
						break;
					}
				}
			}
			String aDayAgoSuffix = logFileDateFormat.format(new Date(System.currentTimeMillis() - 3600L * 24 * 1000));
			String suffix = logFileDateFormat.format(new Date());
			for (Iterator<Map.Entry<String, StringBuffer>> itr = allLogs.entrySet().iterator(); itr.hasNext();) {
				Map.Entry<String, StringBuffer> entry = itr.next();
				String host = entry.getKey();
				StringBuffer buffer = entry.getValue();
				if (buffer == null || buffer.length() == 0) {
					continue;
				}
				String toAppend = buffer.toString();
				buffer.delete(0, toAppend.length());
				FileOutputStream fos = null;
				String loggingBase = HttpLoggingConfig.loggingBase;
				String key = loggingBase + "/" + host + "." + suffix + ".log";
				fos = allFilestreams.get(key);
				if (fos != null) {
					try {
						fos.write(toAppend.getBytes());
						fos.flush();
					} catch (IOException e) {
						e.printStackTrace();
						try {
							fos.close();
						} catch (IOException e2) {
							e2.printStackTrace();
						}
						allFilestreams.remove(key);
					}
				} else {
					if (loggingBase == null || loggingBase.length() == 0) {
						System.out.println(toAppend); // no log path is configured, print to console
					} else {
						File loggingBaseFolder = new File(loggingBase);
						if (!loggingBaseFolder.exists()) {
							loggingBaseFolder.mkdirs();
						}
						try {
							fos = new FileOutputStream(new File(loggingBaseFolder, host + "." + suffix + ".log"), true);
							fos.write(toAppend.getBytes());
							fos.flush();
							allFilestreams.put(key, fos);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
				String aDayAgoKey = loggingBase + "/" + host + "." + aDayAgoSuffix + ".log";
				FileOutputStream fos2 = allFilestreams.get(aDayAgoKey);
				if (fos2 != null) {
					allFilestreams.remove(aDayAgoKey);
					try {
						fos2.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
//				try {
//					fos = new FileOutputStream(new File(loggingBase, host + "." + suffix + ".log"), true);
//					fos.write(toAppend.getBytes());
//				} catch (IOException e) {
//					e.printStackTrace();
//				} finally {
//					if (fos != null) {
//						try {
//							fos.close();
//						} catch (IOException e) {
//							e.printStackTrace();
//						}
//					}
//				}

			}
		}
		loggingStarted = false;
	}
	
	public static void stopLogging() {
		running = false;
	}
	
}
