/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.channels.SocketChannel;
import java.util.Date;

/**
 * HTTP request class.
 * 
 * @author zhourenjian
 *
 */
public class HttpRequest {

	public SocketChannel socket;
	
	/*
	 * Read from request.
	 */
	public String remoteIP;
	public String method; // POST, GET, PUT, ...
	public String url; // /signup/...
	public boolean v11; // HTTP/1.1 or HTTP/1.0
	public String host; // Host: webuzz.im
	public int port;
	public String userAgent; // User-Agent: Mozilla/...
	public String referer; // Referer: http://www.google.com/...
	public String cookies; // Cookie: ...
	public boolean supportGZip; // Accept-Encoding: gzip
	public long keepAliveTimeout = HttpConfig.aliveTimeout; // <= 0: do not keepalive // Keep-Alive: 300, timeout=30
	public long keepAliveMax; // <= 0: do not keepalive // Connection: keep-alive
	public long lastModifiedSince; // If-Modified-Since: ...
	public String eTag; // If-None-Match: ...
	public boolean expecting100;
	
	/**
	 * Server requests for Base64 authorization and browser answers.
	 */
	public String authorization; // Authorization: ...
	public int contentLength; // Content-Length: 324
	public String contentType; // Content-Type: ...

	public String session;

	public Object requestData; // url for static resources or requested SimpleSerialize object
	
	public long created;
	public long lastSent;
	public int sending;
	public long closed;

	public int requestCount; // Will increase for Keep-Alive connection
	
	/*
	 * Parsing status.
	 */
	private boolean firstLine = true; // GET /home.html HTTP/1.1
	private boolean header = true; // XXX: ...
	private boolean searchHeaderTail = false;
	public int dataLength; // Content-Length: ...
	public byte[] pending; // \r\n\r\nXXXX...
	
	/**
	 * Whether this HTTP request is a comet connection or not.
	 */
	public boolean comet;
	/**
	 * Whether this HTTP request has been responded or not
	 */
	public boolean done;
	
	/* Support of HTTP pipeline */
	public boolean fullRequest = false;
	public HttpRequest prev;
	public HttpRequest next;
	public Object mutex; // lock for HTTP pipeline
	
	public HttpQuickResponse response;
	public IRequestMonitor monitor;
	
	public int rangeBeginning;
	public int rangeEnding;
	
	public boolean keepRawData;
	public ByteArrayOutputStream rawData;
	
	public HttpRequest() {
		reset();
	}

	public void debugPrint() {
		System.out.println("keepAliveTimeout: " + keepAliveTimeout);
		System.out.println("keepAliveMax: " + keepAliveMax);
		System.out.println("v11: " + v11);
		System.out.println("created: " + created);
		System.out.println("lastSent: " + lastSent);
		System.out.println("sending: " + sending);
		System.out.println("closed: " + closed);
		System.out.println("method: " + method);
		System.out.println("url: " + url);
		System.out.println("host: " + host);
		System.out.println("remoteIP: " + remoteIP);
		System.out.println("userAgent: " + userAgent);
		System.out.println("referer: " + referer);
		System.out.println("requestData: " + requestData);
		System.out.println("requestCount: " + requestCount);
		System.out.println("comet: " + comet);
		System.out.println("done: " + done);
		System.out.println("supportGZip: " + supportGZip);
		System.out.println("fullRequest: " + fullRequest);
		System.out.println("session: " + session);
		System.out.println("dataLength: " + dataLength);
		System.out.println("pending: " + pending);
		if (pending != null) {
			System.out.println("pending str: " + new String(pending));
		}
		System.out.println("firstLine: " + firstLine);
		System.out.println("header: " + header);
		System.out.println("contentLength: " + contentLength);
		System.out.println("expect: " + expecting100);
	}
	
	/**
	 * Reset request. We may re-use this object for another HTTP request.
	 */
	public void reset(){
		keepAliveMax = 0;
		keepAliveTimeout = HttpConfig.aliveTimeout;
		v11 = false;
		created = System.currentTimeMillis();
		lastSent = 0;
		sending = 0;
		closed = 0;
		method = null;
		url = null;
		host = null;
		port = 0;
		requestData = null;
		comet = false;
		session = null;
		pending = null;
		dataLength = 0;
		lastModifiedSince = 0;
		eTag = null;
		expecting100 = false;
		firstLine = true;
		header = true;
		searchHeaderTail = false;
		contentLength = 0;
		fullRequest = false;
		done = false;
		userAgent = null; // no needs of resetting remoteIP but proxy server may change other properties
		referer = null;
		cookies = null;
		authorization = null;
		contentType = null;
		supportGZip = false;
		response = null;
		next = null;
		rangeBeginning = -1;
		rangeEnding = -1;
		
		keepRawData = false;
		rawData = null;
	}
	
	public HttpRequest clone() {
		HttpRequest r = new HttpRequest();
		cloneTo(r);
		return r;
	}
	
	public void cloneTo(HttpRequest r){
		r.keepAliveMax = r.keepAliveMax;
		r.keepAliveTimeout = keepAliveTimeout;
		r.v11 = v11;
		r.created = created;
		r.lastSent = lastSent;
		r.sending = sending;
		r.closed = closed;
		r.method = method;
		r.url = url;
		r.host = host;
		r.port = port;
		r.requestData = requestData;
		r.comet = comet;
		r.session = session;
		r.pending = pending;
		r.dataLength = dataLength;
		r.lastModifiedSince = lastModifiedSince;
		r.eTag = eTag;
		r.expecting100 = expecting100;
		r.firstLine = firstLine;
		r.header = header;
		r.searchHeaderTail = searchHeaderTail;
		r.contentLength = contentLength;
		r.fullRequest = fullRequest;
		r.done = done;
		r.userAgent = userAgent;
		r.referer = referer;
		r.cookies = cookies;
		r.authorization = authorization;
		r.contentType = contentType;
		r.supportGZip = supportGZip;
		r.response = response;
		r.next = next;
		r.rangeBeginning = rangeBeginning;
		r.rangeEnding = rangeEnding;
		
		r.keepRawData = keepRawData;
		r.rawData = rawData;
	}
	
	protected int checkParseRequest(StringBuilder request) {
		return 0;
	}
	
	/**
	 * Parse given data and update HTTP request status.
	 * 
	 * @param data
	 * @return
	 */
	public HttpQuickResponse parseData(byte[] data) {
		if (keepRawData && data != null) {
			if (rawData == null) {
				rawData = new ByteArrayOutputStream(data.length);
			}
			try {
				rawData.write(data);
			} catch (IOException e) {
				//e.printStackTrace();
			}
		}
		if (!firstLine && !header && data != null) { // already finish reading headers, continue to read data
			ByteArrayOutputStream baos = (ByteArrayOutputStream) requestData;
			int existedDataSize = baos.size();
			if (existedDataSize + data.length < contentLength) { // not completed
				baos.write(data, 0, data.length);
				return new HttpQuickResponse(100);
			} else {
				int length = contentLength - existedDataSize;
				baos.write(data, 0, length);
				requestData = baos.toByteArray();
				if (data.length > length) { // Pipelining support: still more data, next HTTP request?
					dataLength = data.length - length;
					pending = new byte[dataLength];
					System.arraycopy(data, length, pending, 0, dataLength);
				} else {
					pending = null;
					dataLength = 0;
				}
				if (response != null) { // might has format error already!
					return response;
				}
				response = new HttpQuickResponse(200);
				return response;
			}
		}
		int idx = -1;
		int lineBegin = 0;
		int lastSlash = -1;
		int firstColon = -1;
		int firstSemiColon = -1;
		int firstSpace = -1;
		int lastSpace = -1;
		int firstQuestionMark = -1;
		boolean containsPercent = false;
		if (pending != null) {
			if (data == null) {
				data = pending;
			} else if (dataLength + data.length < pending.length) {
				System.arraycopy(data, 0, pending, dataLength, data.length);
				data = pending;
			} else {
				if (data.length + pending.length > 8192 + 8192) { // 2+ packets, considered as attacks?
					pending = null; // clear cached data
					dataLength = 0;
					response = new HttpQuickResponse(400);
					return response;
				}
				byte[] newData = new byte[data.length + pending.length];
				System.arraycopy(pending, 0, newData, 0, dataLength);
				System.arraycopy(data, 0, newData, dataLength, data.length);
				data = newData;
			}
		}
		if (data == null) {
			System.out.println("Error!");
		}
			
		while (idx < data.length - 1) {
			idx++;
			byte b = data[idx];
			if (firstLine) {
				if (idx > 10240) { // Might be attacks, in such case, we can not continue parsing ...
					// Piled server support URL with length < 8k
					pending = null; // clear cached data
					dataLength = 0;
					response = new HttpQuickResponse(414); // url too long
					return response;
				}
				if (b == '/') {
					lastSlash = idx;
				} else if (b == ' ') {
					if (firstSpace == -1) {
						firstSpace = idx;
					} else {
						lastSpace = idx;
					}
				} else if (b == '?') { // /getpost?id=134
					if (firstQuestionMark == -1) {
						firstQuestionMark = idx;
					}
				} else if (b == '#') { // /qina#... make it as request
					if (firstQuestionMark == -1) {
						firstQuestionMark = idx;
					}
				} else if (b == '%') { // %20
					if (firstQuestionMark == -1) {
						containsPercent = true;
					}
				} else if (b == ';') { // ;jsessionid=....
					if (firstQuestionMark == -1) {
						firstSemiColon = idx;
					}
				} else if (b == '\n') {
					if (lastSlash == -1 || firstSpace == -1 || firstSpace >= lastSpace || firstSpace > 32 // method too long!
							|| (firstQuestionMark != -1 && (firstQuestionMark < firstSpace
									|| firstQuestionMark > lastSpace
									|| (firstSemiColon != -1 && firstSemiColon < firstSpace)))) {
						response = new HttpQuickResponse(400); // BAD
						firstLine = false;
						lineBegin = idx + 1;
						firstSpace = -1;
						continue; // error
					}
					v11 = false;
					if (data[lastSlash + 3] == '1' && data[lastSlash + 2] == '.' && data[lastSlash + 1] == '1') {
						v11 = true;
					} else if (data[lastSlash + 3] == '2' && data[lastSlash + 2] == '.' && data[lastSlash + 1] == '0') {
						v11 = true; // Be compatible with HTTP/2.0
					}
					if (v11) { // https://en.wikipedia.org/wiki/HTTP_persistent_connection
						keepAliveMax = HttpConfig.maxKeepAlive;
						keepAliveTimeout = HttpConfig.aliveTimeout;
					}
					method = new String(data, 0, firstSpace);
					if (firstQuestionMark == -1) {
						if (firstSemiColon > 0 && firstSemiColon < lastSpace) { // /post;jsessionid=fa382a...
							url = new String(data, firstSpace + 1, firstSemiColon - firstSpace - 1);
							if (firstSemiColon + 12 < lastSpace) {
								session = new String(data, firstSemiColon + 12, lastSpace - (firstSemiColon + 12)); 
							}
						} else {
							url = new String(data, firstSpace + 1, lastSpace - firstSpace - 1);
						}
						if (containsPercent) { // /page/hello%20world.html
							try {
								url = URLDecoder.decode(url, "UTF-8");
							} catch (UnsupportedEncodingException e) {
								//e.printStackTrace();
								response = new HttpQuickResponse(400);
								firstLine = false;
								lineBegin = idx + 1;
								firstSpace = -1;
								continue; // error
							}
						}
						if (url.indexOf("..") != -1 || url.indexOf("./") != -1) {
							url = fixURL(url);
						}
					} else {
						if (firstSemiColon > 0 && firstSemiColon < firstQuestionMark) { // /post;jsessionid=fa382a...?content=
							url = new String(data, firstSpace + 1, firstSemiColon - firstSpace - 1);
							if (firstSemiColon + 12 < firstQuestionMark) {
								session = new String(data, firstSemiColon + 12, firstQuestionMark - (firstSemiColon + 12)); 
							}
						} else { //
							url = new String(data, firstSpace + 1, firstQuestionMark - firstSpace - 1);
						}
						if (containsPercent) {// /page/hello%20world/get?attrs=time
							try {
								url = URLDecoder.decode(url, "UTF-8");
							} catch (UnsupportedEncodingException e) {
								//e.printStackTrace();
								response = new HttpQuickResponse(400);
								firstLine = false;
								lineBegin = idx + 1;
								firstSpace = -1;
								continue; // error
							}
						}
						if (url.indexOf("..") != -1 || url.indexOf("./") != -1) {
							url = fixURL(url);
						}
						
						StringBuilder request = new StringBuilder(new String(data, firstQuestionMark + 1, lastSpace - firstQuestionMark - 1));
						if (checkParseRequest(request) == -1) {
							firstLine = false;
							lineBegin = idx + 1;
							firstSpace = -1;
							continue;
						}
						requestData = request.toString();
					}
					firstLine = false;
					lineBegin = idx + 1;
					firstSpace = -1;
				}
			} else { // header
				if (b == ':') {
					if (firstColon == -1) {
						firstColon = idx;
					}
				} else if (b == ' ') {
					if (firstSpace == -1) {
						if (firstColon + 1 == idx) {
							firstSpace = idx;
						}
					} else if (firstSpace + 1 == idx) {
						firstSpace++;
					}
				} else if (b == '\n' || (!searchHeaderTail && idx - lineBegin >= 512)) {
					if (b == '\n' && searchHeaderTail) {
						searchHeaderTail = false;
						firstColon = -1;
						firstSpace = -1;
						lineBegin = idx + 1;
						continue;
					}
					if (b != '\n' && idx - lineBegin >= 512) {
						// will ignore those big header fields, e.g. Cookie
						// Host, User-Agent, Referer, ... fields should not be longer than 512
						searchHeaderTail = true;
					}
					if (idx - lineBegin <= 1) { // \r\n\r\n meaning the end of header
						lineBegin = idx + 1;
						header = false;
						
						// begin to read data
						HttpQuickResponse resp = collectData(data, lineBegin);
						if (resp != null) {
							response = resp;
							return resp;
						}
						return new HttpQuickResponse(100); // continue reading data
						// end of reading data
					} else { // start parsing header
						if (firstColon == -1) { // error field
							response = new HttpQuickResponse(400);
							if (b == '\n') {
								firstSpace = -1;
								lineBegin = idx + 1;
							}
							continue; // error
						}
						String headerName = new String(data, lineBegin, firstColon - lineBegin);
						int offset = (firstSpace != -1 ? firstSpace : firstColon) + 1;
						if (idx - 1 - offset > 0) {
							int valueLength = idx - 1 - offset;
							if (data[idx - 1] != '\r') { // HTTP line break must be \r\n. Here is tolerance provision: http://www.w3.org/Protocols/rfc2616/rfc2616-sec19.html#sec19.3 
								valueLength++;
							}
							if ("Host".equals(headerName)) {
								host = new String(data, offset, valueLength).toLowerCase();
								int endIdx = 0;
								if (host.length() > 0 && host.charAt(0) == '[') { // IPv6
									endIdx = host.indexOf(']');
									if (endIdx == -1) {
										endIdx = 0;
									}
								}
								int index = host.indexOf(':', endIdx);
								if (index != -1) {
									try {
										port = Integer.parseInt(host.substring(index + 1));
									} catch (NumberFormatException e) {
										port = 0;
									}
									if (endIdx > 0) {
										host = host.substring(1, endIdx);
									} else {
										host = host.substring(0, index);
									}
								} else {
									port = 0;
									if (endIdx > 0) {
										host = host.substring(1, endIdx);
									}
								}
								if (isMaliciousHost(host)) {
									host = null; // Bad request
									response = new HttpQuickResponse(400);
									return response;
								}
							} else if ("User-Agent".equals(headerName)) {
								userAgent = new String(data, offset, valueLength);
							} else if ("Referer".equals(headerName)) {
								referer = new String(data, offset, valueLength);
							} else if ("Cookie".equals(headerName)) {
								cookies = new String(data, offset, valueLength);
							} else if ("Accept-Encoding".equals(headerName)) {
								String encoding = new String(data, offset, valueLength);
								if (encoding.indexOf("gzip") != -1) {
									supportGZip = true;
								}
							} else if ("Content-Type".equals(headerName)) {
								contentType = new String(data, offset, valueLength);
							} else if ("Content-Length".equals(headerName)) {
								String headerValue = new String(data, offset, valueLength);
								boolean formatError = false;
								try {
									contentLength = Integer.parseInt(headerValue);
								} catch (NumberFormatException e) {
									formatError = true;
								}
								if (formatError || contentLength > HttpConfig.maxPost) {
									response = new HttpQuickResponse(413);
									if (b == '\n') {
										firstColon = -1;
										firstSpace = -1;
										lineBegin = idx + 1;
									}
									continue; // error
								}
							} else if ("Expect".equals(headerName)) {
								expecting100 = "100-continue".equalsIgnoreCase(new String(data, offset, valueLength));
							} else {
								int maxRequests = HttpConfig.maxKeepAlive;
								int timeout = HttpConfig.aliveTimeout;
								if ("Keep-Alive".equals(headerName)) {
									String headerValue = new String(data, offset, valueLength).toLowerCase();
									if (headerValue.indexOf("closed") != -1) {
										keepAliveMax = 0;
										keepAliveTimeout = 0;
									} else {
										String[] split = headerValue.split(", ");
										if (split.length >= 2) {
											for (int i = 0; i < split.length; i++) {
												String p = split[i];
												int index = p.indexOf('=');
												if (index != -1) {
													String name = p.substring(0, index);
													String value = p.substring(index + 1);
													if ("timeout".equals(name)) {
														try {
															keepAliveTimeout = Integer.parseInt(value);
														} catch (NumberFormatException e) {
															keepAliveTimeout = timeout;
														}
														if (keepAliveTimeout > timeout) {
															keepAliveTimeout = timeout;
														}
													} else {
														try {
															keepAliveMax = Integer.parseInt(value);
														} catch (NumberFormatException e) {
															keepAliveMax = maxRequests;
														}
														if (keepAliveMax > maxRequests) {
															keepAliveMax = maxRequests;
														}
													}
												}
											}
										} else {
											keepAliveTimeout = timeout;
											try {
												keepAliveMax = Integer.parseInt(headerValue);
											} catch (NumberFormatException e) {
												keepAliveMax = maxRequests;
											}
											if (keepAliveMax > maxRequests) {
												keepAliveMax = maxRequests;
											}
										}
									}
									// end of Keep-Alive
								} else if ("Connection".equals(headerName)) {
									String headerValue = new String(data, offset, valueLength).toLowerCase();
									if (headerValue.indexOf("close") != -1) {
										keepAliveMax = 0;
										keepAliveTimeout = 0;
									} else if (headerValue.indexOf("keep-alive") != -1) {
										if (keepAliveTimeout <= 0) {
											keepAliveTimeout = timeout; // seconds
										}
										if (keepAliveMax <= 0) {
											keepAliveMax = maxRequests;
										}
									}
								} else if ("If-Modified-Since".equals(headerName)) {
									String headerValue = new String(data, offset, valueLength);
									try {
										Date parsedDate = DateUtils.parseDate(headerValue);
										lastModifiedSince = parsedDate.getTime();
									} catch (DateParseException e) {
										e.printStackTrace();
									}
								} else if ("If-None-Match".equals(headerName)) {
									eTag = new String(data, offset, valueLength).trim();
								} else if ("Authorization".equals(headerName)) {
									authorization = new String(data, offset, valueLength).trim();
								} else if ("Range".equals(headerName)) {
									String rangeStr = new String(data, offset, valueLength).trim();
									String bytesPrefix = "bytes=";
									if (rangeStr.startsWith(bytesPrefix)) {
										rangeStr = rangeStr.substring(bytesPrefix.length()).trim();
										if (rangeStr.length() != 0) {
											String[] segments = rangeStr.split("-");
											if (segments.length == 2) {
												if (segments[0].length() > 0) {
													rangeBeginning = Integer.parseInt(segments[0]);
												}
												if (segments[1].length() > 0) {
													rangeEnding = Integer.parseInt(segments[1]);
													if (rangeEnding < rangeBeginning) { // toggle
														int tmp = rangeEnding;
														rangeEnding = rangeBeginning;
														rangeBeginning = tmp;
													}
												}
											} else if (segments.length == 1 && rangeStr.indexOf('-') != -1) {
												if (segments[0].length() > 0) {
													rangeBeginning = Integer.parseInt(segments[0]);
												}
											}
										}
									}
								} else if (!HttpConfig.useDirectRemoteIP && "X-Real-IP".equals(headerName)) {
									String ip = new String(data, offset, valueLength).toLowerCase().trim();
									int x = 0;
									if (!HttpConfig.ignoreIntranetForwardedIP || (ip.indexOf(':') != -1 && !ip.startsWith("fd") && !ip.startsWith("fe"))
											|| (isValidIPv4Address(ip) && !ip.startsWith("10.") && !ip.startsWith("192.168.")
												&& (!ip.startsWith("172.") || (x = Integer.parseInt(ip.substring(4, ip.indexOf('.', 4)))) < 16 || x >= 32)
												&& !ip.startsWith("127."))) {
										remoteIP = ip;
									}
								} else if (!HttpConfig.useDirectRemoteIP && "X-Forwarded-For".equals(headerName)) {
									String pathStr = new String(data, offset, valueLength).trim();
									if (remoteIP == null || !pathStr.startsWith(remoteIP)) {
										// For normal request, this branch won't be reached.
										int index = 0;
										do {
											int separatorIdx = pathStr.indexOf(',', index);
											String ip = pathStr.substring(index, separatorIdx < 0 ? pathStr.length() : separatorIdx).toLowerCase().trim();
											int x = 0;
											if (!HttpConfig.ignoreIntranetForwardedIP || (ip.indexOf(':') != -1 && !ip.startsWith("fd") && !ip.startsWith("fe"))
													|| (isValidIPv4Address(ip) && !ip.startsWith("10.") && !ip.startsWith("192.168.")
														&& (!ip.startsWith("172.") || (x = Integer.parseInt(ip.substring(4, ip.indexOf('.', 4)))) < 16 || x >= 32)
														&& !ip.startsWith("127."))) {
												remoteIP = ip;
												break;
											}
											if (separatorIdx == -1) {
												break;
											}
											index = separatorIdx + 1;
										} while (true);
									}
								}
							}
						}
						if (b == '\n') {
							firstColon = -1;
							firstSpace = -1;
							lineBegin = idx + 1;
						}
					} // end of parsing header's name-value pair
				}
			} // end of header parsing
		} // end of while
		int length = data.length - lineBegin;
		if (length > 0 && !searchHeaderTail) { // start reading post data
			if (pending == null || lineBegin != 0) {
				pending = new byte[length];
				System.arraycopy(data, lineBegin, pending, 0, length);
				dataLength = length;
			} else {
				if (lineBegin == 0) {
					pending = data;
					dataLength = data.length;
//				} else {
//					pending = new byte[length];
//					System.arraycopy(data, lineBegin, pending, 0, length);
//					dataLength = length;
				}
			}
		} else {
			pending = null;
			dataLength = 0;
		}
		return new HttpQuickResponse(100); // Continue reading...
	}

	public static boolean isMaliciousHost(String host) {
		int length = host.length();
		boolean lastDot = false;
		for (int i = 0; i < length; i++) {
			char c = host.charAt(i);
			if (!('a' <= c && c <= 'z') && c != '.'
					&& !('0' <= c && c <= '9')
					&& !('A' <= c && c <= 'Z')
					&& c != '-' && c != ':') {
				return true;
			} else if (c == '.') {
				if (lastDot) { // ..
					return true;
				}
				lastDot = true;
			} else {
				lastDot = false;
			}
		}
		return false;
	}

    public static boolean isValidIPv4Address(String ip) {
    	int count = 0;
		int cursor = 0;
		do {
			int idx = ip.indexOf('.', cursor);
			try {
				if (Integer.parseInt(ip.substring(cursor, idx < 0 ? ip.length() : idx)) > 255) {
					return false;
				}
			} catch (NumberFormatException e) {
				return false;
			}
			count++;
			if (idx == -1) {
				return count == 4;
			}
			cursor = idx + 1;
		} while (true);
    }

	private HttpQuickResponse collectData(byte[] data, int dataBegin) {
		if (contentLength == 0) { // not tested branch
			// TODO: chunked encoding requests?
			int length = dataBegin + contentLength;
			if (data.length > length) { // Pipelining support: still more data, next HTTP request?
				dataLength = data.length - length;
				pending = new byte[dataLength];
				System.arraycopy(data, length, pending, 0, dataLength);
			} else {
				pending = null; // clear cached data
				dataLength = 0;
			}
			if (response != null) {
				return response;
			}
			return new HttpQuickResponse(200);
		}
		if (data.length - dataBegin >= contentLength) {
			int length = dataBegin + contentLength;
			if (data.length > length) { // Pipelining support: still more data, next HTTP request?
				dataLength = data.length - length;
				pending = new byte[dataLength];
				System.arraycopy(data, length, pending, 0, dataLength);
			} else {
				pending = null; // clear cached data
				dataLength = 0;
			}
			if (response != null) {
				return response;
			}
			//... request is completed. copy data to requestData
			if (contentLength > 3 && data[dataBegin + 3] == '=') {
				int jzzStarted = -1;
				int jzzStopped = -1;
				int dataEnd = dataBegin + contentLength;
				for (int i = dataBegin + 3; i < dataEnd; i++) {
					if (jzzStarted != -1) {
						if (data[i] == '&') {
							// gotcha
							jzzStopped = i;
							break;
						}
						continue;
					}
					if (data[i] == '=' && data[i - 1] == 'z' && data[i - 2] == 'z' && data[i - 3] == 'j') { // jzz=
						jzzStarted = i + 1;
					}
				}
				if (jzzStarted != -1) {
					if (jzzStopped == -1) {
						jzzStopped = dataEnd;
					}
					requestData = new String(data, jzzStarted, jzzStopped);
					return new HttpQuickResponse(200);
				}
			}
			byte[] contentData = new byte[contentLength];
			System.arraycopy(data, dataBegin, contentData, 0, contentLength);
			requestData = contentData;
			return new HttpQuickResponse(200);
		} else {
			ByteArrayOutputStream baos = new HttpDataOutputStream(contentLength, response != null);
			baos.write(data, dataBegin, data.length - dataBegin);
			requestData = baos;
			
			pending = null; // clear cached data
			dataLength = 0;
			return null;
		}
	}
	
	/**
	 * Remove "/../" or "/./" in URL.
	 * 
	 * @param url
	 * @return Fixed URL.
	 */
	public static String fixURL(String url) {
		int length = url.length();
		if (length == 0) {
			return url;
		}
		boolean slashStarted = url.charAt(0) == '/';
		boolean slashEnded = length > 1 && url.charAt(length - 1) == '/';
		
		int idxBegin = slashStarted ? 1 : 0;
		int idxEnd = slashEnded ? length - 1 : length;
		if (idxEnd - idxBegin <= 0) {
			return "/";
		}
		String[] segments = url.substring(idxBegin, idxEnd).split("\\/|\\\\");
		int count = segments.length + 1;
		for (int i = 0; i < segments.length; i++) {
			count--;
			if (count < 0) {
				System.out.println("Error in fixing URL: " + url);
				break;
			}
			String segment = segments[i];
			if (segment == null) {
				break;
			}
			if (segments[i].equals("..")) {
				int shift = 2;
				if (i > 0) {
					segments[i - 1] = null;
					segments[i] = null;
					if (i + 1 > segments.length - 1 || segments[i + 1] == null) {
						slashEnded = true;
					}
				} else {
					segments[i] = null;
					shift = 1;
				}
				for (int j = i - shift + 1; j < segments.length - shift; j++) {
					String s = segments[j + shift];
					segments[j] = s;
					if (j == segments.length - shift - 1 || s == null) {
						if (shift == 1) {
							segments[j + 1] = null;
						} else { // shift == 2
							segments[j + 1] = null;
							segments[j + 2] = null;
						}
					}
				}
				i -= shift;
			} else if (segments[i].equals(".")) {
				segments[i] = null;
				if (i + 1 > segments.length - 1 || segments[i + 1] == null) {
					slashEnded = true;
				}
				for (int j = i; j < segments.length - 1; j++) {
					String s = segments[j + 1];
					segments[j] = s;
					if (j == segments.length - 2) {
						segments[j + 1] = null;
					}
				}
				i--;
			}
		}
		StringBuilder builder = new StringBuilder(length);
		int lastLength = 0;
		boolean needSlash = true;
		for (int i = 0; i < segments.length; i++) {
			String segment = segments[i];
			if (segment == null) {
				break;
			}
			if (needSlash) {
				builder.append("/");
			}
			builder.append(segment);
			lastLength = segment.length();
			needSlash = lastLength > 0;
		}
		if (lastLength == 0 || slashEnded) {
			builder.append("/");
		}
		return builder.toString();
	}
	
}
