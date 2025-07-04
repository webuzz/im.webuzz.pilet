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
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
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


	public static void addLogging(String key, String loggingStr) {
		addLogging(key, loggingStr, 0);
	}
	
	public static void addLogging(String key, String loggingStr, long created) {
		if (loggingStr == null || loggingStr.length() == 0) {
			return;
		}
		if (key == null || key.length() == 0) {
			key = "default"; // Not knowing the host name from HTTP request
		}
		if (key.indexOf("..") != -1 || key.indexOf('\\') != -1 || key.indexOf('/') != -1) {
			key = "hacker"; // Bad request
		}
		Map<String, String> mappings = HttpLoggingConfig.hostMappings;
		if (mappings != null) {
			String newHost = mappings.get(key);
			if (newHost != null && newHost.length() > 0) {
				key = newHost;
			}
		}
		Set<String> ignorings = HttpLoggingConfig.ignoringHosts;
		if (ignorings != null && ignorings.contains(key)) {
			return;
		}
		Set<String> knowns = HttpLoggingConfig.knownHosts;
		if (knowns != null && !knowns.contains(key)) {
			key = HttpLoggingConfig.unknowHostLogPrefix;
			if (key == null || key.length() == 0) {
				key = "unknown.hosts";
			}
		}
		StringBuffer buffer = allLogs.get(key); // each host will has a StringBuffer object.
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
				buffer = allLogs.get(key);
				if (buffer == null) {
					buffer = new StringBuffer(8096);
					allLogs.put(key, buffer);
				}
			} // end of synchronized blocked
		} // end of if
		if (created >= 0) {
			if (created == 0) {
				created = System.currentTimeMillis();
			}
			String dateStr = getDateString(created);
			//synchronized (buffer) {
				buffer.append(dateStr).append(" - ").append(loggingStr).append(lineSeparator);
			//}
		} else {
			//synchronized (buffer) {
				buffer.append(loggingStr);
			//}
		}
	}

	/*
	 * We use stand-alone thread to log all logs in very 10 seconds.
	 */
	@Deprecated
	public static void addLogging(String host, HttpRequest req, int responseCode, long responseLength) {
		// addLogging(host, req, responseCode, responseLength);
		addLogging(host, req, null, null, responseCode, responseLength);
	}
	public static void addLogging(String host, HttpRequest req, HttpResponse resp, String requestContent, int responseCode, long responseLength) {
		Set<String> ignorings = HttpLoggingConfig.ignoringHosts;
		if (ignorings != null && host != null && ignorings.contains(host)) {
			return;
		}
		StringBuilder builder = new StringBuilder(256);
		Object[] segs = HttpLoggingConfig.loggingSegments;
		for (int i = 0; i < segs.length; i++) {
			Object o = segs[i];
			if (o instanceof String) {
				builder.append(o);
			} else {
				ServerVariable v = (ServerVariable) o;
				switch (v) {
				case remote_addr:
					builder.append(req.remoteIP);
					break;
				case remote_port:
					Socket socket = resp.socket.socket();
					if (socket != null) {
						builder.append(socket.getPort());
					} else {
						builder.append("-");
					}
					break;
				case request:
					if (requestContent != null && requestContent.length() > 0) {
						builder.append(requestContent);
					} else {
						builder.append(req.method);
						builder.append(" ");
						builder.append(req.url);
						if (/*"GET".equals(req.method) && */req.requestQuery != null) {
							String more = req.requestQuery;
							builder.append("?");
							builder.append(more);
						}
						if (req.v11) {
							builder.append(" HTTP/1.1");
						} else {
							builder.append(" HTTP/1.0");
						}
					}
					break;
				case server_protocol:
					if (req.v11) {
						builder.append("HTTP/1.1");
					} else {
						builder.append("HTTP/1.0");
					}
					break;
				case time_local:
					builder.append(getDateString(req.created));
					break;
				case status:
					builder.append(responseCode);
					break;
				case body_bytes_sent:
				case bytes_sent:
					builder.append(responseLength);
					break;
				case http_referer:
					builder.append(req.referer == null ? "-" : req.referer);
					break;
				case http_user_agent:
					builder.append(req.userAgent == null ? "-" : req.userAgent);
					break;
				case https:
					if (resp != null && resp.worker.getServer().isSSLEnabled()) {
						builder.append("on");
					}
					break;
				case scheme:
					if (resp != null) {
						if (resp.worker.getServer().isSSLEnabled()) {
							builder.append("https");
						} else {
							builder.append("http");
						}
					} else {
						builder.append("-");
					}
					break;
				case host:
				case hostname:
					builder.append(req.host != null && req.host.length() > 0 ? req.host : "-");
					break;
				case content_type:
					builder.append(req.contentType != null && req.contentType.length() > 0 ? req.contentType : "-");
					break;
				case content_length:
					builder.append(req.contentLength);
					break;
				case request_method:
					builder.append(req.method);
					break;
				case request_length:
					builder.append(req.contentLength);
					break;
				case is_args:
					builder.append(req.requestQuery != null && req.requestQuery.length() > 0 ? "?" : "");
					break;
				case args:
				case query_string:
					builder.append(req.requestQuery);
					break;
				case uri:
				case document_uri:
				case request_uri:
					builder.append(req.url);
					break;
				case request_time:
					appendTime(builder, System.currentTimeMillis() - req.created);
					break;
				case msec:
					appendTime(builder, System.currentTimeMillis());
					break;

				default:
					builder.append("-");
					break;
				}
			}
		}
		builder.append(lineSeparator);
		addLogging(host, builder.toString(), -1); // ignore time
	}

	private static void appendTime(StringBuilder builder, long interval) {
		builder.append(interval / 1000);
		builder.append('.');
		long ms = interval % 1000;
		if (ms < 10) {
			builder.append("00");
		} else if (ms < 100) {
			builder.append('0');
		}
		builder.append(ms);
	}

	public static String getDateString(long created) {
		String dateStr = null;
		if (lastLoggedDate > 0 && Math.abs(created - lastLoggedDate) < 500) {
			dateStr = lastDateStr;
		} else {
			// Use DateUtils for thread safe SimpleDateFormat
			dateStr = DateUtils.formatDate(new Date(created), "dd/MMM/yyyy:HH:mm:ss Z");
			lastDateStr = dateStr;
			lastLoggedDate = created;
		}
		return dateStr;
	}

	static void runLoggingLoop() {
		running = true;
		SimpleDateFormat logFileDateFormat = new SimpleDateFormat("yyyy-MM-dd");
		Map<String, FileOutputStream> allFilestreams = new HashMap<String, FileOutputStream>();
		while (running) {
			int count = 10;
			while (running && count-- > 0) {
				boolean bufferNeedFlushing = false;
				for (int k = 0; k < 20; k++) {
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					for (Iterator<StringBuffer> itr = allLogs.values().iterator(); itr.hasNext();) {
						StringBuffer buffer = (StringBuffer) itr.next();
						if (buffer.length() > HttpLoggingConfig.bufferBlock) {
							bufferNeedFlushing = true;
							break;
						}
					}
					if (bufferNeedFlushing) {
						break;
					}
				}
				if (bufferNeedFlushing) {
					break;
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
				String loggingBase = HttpLoggingConfig.basePath;
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
