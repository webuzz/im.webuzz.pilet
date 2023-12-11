/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class HttpWorkerUtils {

	public static Charset UTF_8 = Charset.forName("UTF-8");

	public static Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

	private final static String[] WEEK_DAYS_ABBREV = new String[] {
		"Sun", "Mon", "Tue", "Wed", "Thu",  "Fri", "Sat"
	};

	@SuppressWarnings("deprecation")
	public static String getHTTPDateString(long time) {
		if (time < 0) {
			time = System.currentTimeMillis();
		}
		Date date = new Date(time);
		return WEEK_DAYS_ABBREV[date.getDay()] + ", " + date.toGMTString();
	}

	@SuppressWarnings("deprecation")
	public static String getStaticResourceExpiredDate(long expiredPlus) {
		if (expiredPlus < 0) {
			expiredPlus = 5L * 365 * 24 * 3600 * 1000;
		}
		Date date = new Date(System.currentTimeMillis() + expiredPlus);
		return WEEK_DAYS_ABBREV[date.getDay()] + ", " + date.toGMTString();
	}
	
	public static boolean checkKeepAliveHeader(HttpRequest req, StringBuffer responseBuffer) {
		if ((req.keepAliveMax > 1 && req.requestCount + 1 < req.keepAliveMax) || req.next != null) {
			responseBuffer.append("Keep-Alive: timeout=");
			responseBuffer.append(req.keepAliveTimeout);
			responseBuffer.append(", max=");
			responseBuffer.append(req.keepAliveMax);
			responseBuffer.append("\r\nConnection: keep-alive\r\n");
			return false;
		} else {
			responseBuffer.append("Connection: close\r\n");
			return true;
		}
	}
	
	public static boolean checkKeepAliveHeader(HttpRequest req, StringBuilder responseBuilder) {
		if ((req.keepAliveMax > 1 && req.requestCount + 1 < req.keepAliveMax) || req.next != null) {
			responseBuilder.append("Keep-Alive: timeout=");
			responseBuilder.append(req.keepAliveTimeout);
			responseBuilder.append(", max=");
			responseBuilder.append(req.keepAliveMax);
			responseBuilder.append("\r\nConnection: keep-alive\r\n");
			return false;
		} else {
			responseBuilder.append("Connection: close\r\n");
			return true;
		}
	}
	
	public static boolean checkKeepAliveHeader(HttpRequest req, StringBuilder responseBuilder, boolean ssl) {
		long hstsAge = HttpConfig.hstsMaxAge;
		if (ssl && hstsAge > 0) {
			responseBuilder.append("Strict-Transport-Security: max-age=").append(hstsAge).append("\r\n");
		}
		if ((req.keepAliveMax > 1 && req.requestCount + 1 < req.keepAliveMax) || req.next != null) {
			responseBuilder.append("Keep-Alive: timeout=");
			responseBuilder.append(req.keepAliveTimeout);
			responseBuilder.append(", max=");
			responseBuilder.append(req.keepAliveMax);
			responseBuilder.append("\r\nConnection: keep-alive\r\n");
			return false;
		} else {
			responseBuilder.append("Connection: close\r\n");
			return true;
		}
	}

	public static boolean sendHeaders(String contentType, int contentLength, HttpRequest req, HttpResponse resp, boolean noExpired) {
		StringBuilder responseBuilder = new StringBuilder(512);
		responseBuilder.append("HTTP/1.");
		responseBuilder.append(req.v11 ? '1' : '0');
		responseBuilder.append(" 200 OK\r\n");
		String serverName = HttpConfig.serverSignature;
		if (req.requestCount < 1 && serverName != null && serverName.length() > 0) {
			responseBuilder.append("Server: ").append(serverName).append("\r\n");
		}
		boolean closeSocket = checkKeepAliveHeader(req, responseBuilder, resp.worker.getServer().isSSLEnabled());
		if (noExpired) {
			responseBuilder.append("Expires: ");
			responseBuilder.append(getStaticResourceExpiredDate(HttpCache.NEVER_EXPIRED));
			responseBuilder.append("\r\n");
		}
		responseBuilder.append("Content-Type: ");
		responseBuilder.append(contentType);
		responseBuilder.append("\r\n");
		if (contentLength > 0) {
			responseBuilder.append("Content-Length: ");
			responseBuilder.append(contentLength);
			responseBuilder.append("\r\n");
		}
		responseBuilder.append("\r\n");
		byte[] data = responseBuilder.toString().getBytes();
		if (contentLength > 0) {
			req.sending = data.length +contentLength;
		}
		resp.worker.getServer().send(resp.socket, data);
		return closeSocket;
		/*
		if (closeSocket) {
			//de.server.send(de.socket, new byte[0]);
			req.created = System.currentTimeMillis();
			dataEvent.worker.poolingRequest(de.socket, req);
		}
		// */
	}

	// Send "### Xxx Xxxxx" response with empty content body
	public static void sendXXXResponse(HttpRequest request, HttpResponse response, String responseCodeText, StringBuilder extraHeaderBuilder, boolean markContentZero) {
		StringBuilder responseBuilder = new StringBuilder(256);
		responseBuilder.append(request.v11 ? "HTTP/1.1 " : "HTTP/1.0 ").append(responseCodeText).append("\r\n");
		String serverName = HttpConfig.serverSignature;
		if (request.requestCount < 1 && serverName != null && serverName.length() > 0) {
			responseBuilder.append("Server: ").append(serverName).append("\r\n");
		}
		boolean closeSocket = checkKeepAliveHeader(request, responseBuilder, response.worker.getServer().isSSLEnabled());
		if (extraHeaderBuilder != null) {
			responseBuilder.append(extraHeaderBuilder);
		}
		if (markContentZero) {
			responseBuilder.append("Content-Length: 0\r\n\r\n");
		} else {
			responseBuilder.append("\r\n");
		}
		byte[] data = responseBuilder.toString().getBytes();
		request.sending = data.length;
		response.worker.getServer().send(response.socket, data);
		if (closeSocket) {
			response.worker.poolingRequest(response.socket, request);
		}
	}

	public static void send500Response(HttpRequest request, HttpResponse response) {
		sendXXXResponse(request, response, "500 Internal Server Error", null, true);
	}

	public static void send503Response(HttpRequest request, HttpResponse response) {
		sendXXXResponse(request, response, "503 Service Unavailable", null, true);
	}

	public static void send400Response(HttpRequest request, HttpResponse response) {
		sendXXXResponse(request, response, "400 Bad Request", null, true);
	}

	public static void send404NotFound(HttpRequest req, HttpResponse resp) {
		sendXXXResponse(req, resp, "404 Not Found", null, true);
	}

	public static void send408RequestTimeout(HttpRequest req, HttpResponse resp) {
		sendXXXResponse(req, resp, "408 Request Timeout", null, true);
	}
	
	public static void send403Forbidden(HttpRequest req, HttpResponse resp) {
		sendXXXResponse(req, resp, "403 Forbidden", null, true);
	}
	
	public static void send401AuthorizationRequired(String realm, HttpRequest req, HttpResponse resp) {
		sendXXXResponse(req, resp, "401 Authorization Required",
				new StringBuilder(64).append("WWW-Authenticate: Basic realm=\"").append(realm == null ? "-" : realm).append("\"\r\n"), true);
	}

	public static void redirect(String url, HttpRequest req, HttpResponse resp) {
		redirect(url, null, req, resp);
	}
	
	public static void redirect(String url, String cookies, HttpRequest req, HttpResponse resp) {
		StringBuilder headerBuilder = new StringBuilder(128 + (cookies != null ? cookies.length() + 64 : 0));
		headerBuilder.append("Location: ").append(url).append("\r\n");
		if (cookies != null) {
			String[] lines = cookies.split("\r\n");
			for (int i = 0; i < lines.length; i++) {
				String line = lines[i];
				if (line.length() != 0) {
					headerBuilder.append("Set-Cookie: ");
					headerBuilder.append(line);
					headerBuilder.append("\r\n");
				}
			}
		}
		sendXXXResponse(req, resp, "301 Moved Permanently", headerBuilder, true);
	}

	/**
	 * Respond 302 Found redirect page without cookies.
	 * 
	 * @param url Redirect to this URL.
	 * @param req
	 * @param resp
	 */
	public static void found(String url, HttpRequest req, HttpResponse resp) {
		found(url, null, req, resp);
	}
	
	/**
	 * Respond 302 Found redirect page with cookies.
	 * 
	 * @param url Redirect to this URL.
	 * @param cookies
	 * @param req
	 * @param resp
	 */
	public static void found(String url, String cookies, HttpRequest req, HttpResponse resp) {
		StringBuilder headerBuilder = new StringBuilder(128 + (cookies != null ? cookies.length() + 64 : 0));
		headerBuilder.append("Location: ").append(url).append("\r\n");
		if (cookies != null) {
			String[] lines = cookies.split("\r\n");
			for (int i = 0; i < lines.length; i++) {
				String line = lines[i];
				if (line.length() != 0) {
					headerBuilder.append("Set-Cookie: ");
					headerBuilder.append(line);
					headerBuilder.append("\r\n");
				}
			}
		}
		sendXXXResponse(req, resp, "302 Found", headerBuilder, true);
	}

	public static void send200OK(HttpRequest req, HttpResponse resp) {
		sendXXXResponse(req, resp, "200 OK", null, true);
	}

	public static void send304NotModified(HttpRequest req, HttpResponse resp) {
		sendXXXResponse(req, resp, "304 Not Modified", null, false);
	}

	public static void send304NeverExpired(HttpRequest req, HttpResponse resp) {
		sendXXXResponse(req, resp, "304 Not Modified",
				new StringBuilder(64).append("Expires: ").append(getStaticResourceExpiredDate(HttpCache.NEVER_EXPIRED)).append("\r\n"), false);
	}

	public static void sendOutRawBytes(HttpRequest req, HttpResponse resp, byte[] rawBytes, boolean closeSocket) {
		resp.worker.getServer().send(resp.socket, rawBytes);
		if (closeSocket) {
			resp.worker.poolingRequest(resp.socket, req);
		}
	}

	public static void pipeOutBytes(HttpRequest request, HttpResponse response, String type, String encoding, byte[] content, boolean cachable) {
		pipeOutBytes(request, null, response, type, encoding, content, cachable ? 0 : -1);
	}

	public static void pipeOutBytes(HttpRequest request, String cookies, HttpResponse response, String type, String encoding, byte[] content, boolean cachable) {
		pipeOutBytes(request, cookies, response, type, encoding, content, cachable ? 0 : -1);
	}

	public static byte[] pipeOutBytes(HttpRequest request, String cookies, HttpResponse response, String type, String encoding, byte[] content, long expired) {
		return pipeOutBytes(request, cookies, response, type, encoding, content, null, -1, expired);
	}

	public static void pipeOutBytes(HttpRequest request, HttpResponse response, String type, String encoding, byte[] content, String eTag) {
		pipeOutBytes(request, null, response, type, encoding, content, eTag);
	}

	public static void pipeOutBytes(HttpRequest request, String cookies, HttpResponse response, String type, String encoding, byte[] content, String eTag) {
		pipeOutBytes(request, cookies, response, type, encoding, content, eTag, -1, eTag != null ? 0 : -1);
	}

	public static byte[] pipeOutBytes(HttpRequest request, String cookies, HttpResponse response, String type, String encoding, byte[] content, String eTag, long expired) {
		return pipeOutBytes(request, cookies, response, type, encoding, content, eTag, -1, expired);
	}
	
	@SuppressWarnings("deprecation")
	public static void appendDateAsHTTPHeaderValue(StringBuilder responseBuilder, Date date) {
		responseBuilder.append(WEEK_DAYS_ABBREV[date.getDay()]);
		responseBuilder.append(", ");
		responseBuilder.append(date.toGMTString());
	}

	public static byte[] pipeOutBytes(HttpRequest request, String cookies, HttpResponse response, String type, String encoding, byte[] content, String eTag, long modified, long expired) {
		StringBuilder responseBuilder = new StringBuilder(512);
		responseBuilder.append("HTTP/1.");
		responseBuilder.append(request.v11 ? '1' : '0');
		responseBuilder.append((request.rangeBeginning >= 0 || request.rangeEnding >= 0) ? " 206 Partial Content\r\n" : " 200 OK\r\n");
		String serverName = HttpConfig.serverSignature;
		if (request.requestCount < 1 && serverName != null && serverName.length() > 0) {
			responseBuilder.append("Server: ").append(serverName).append("\r\n");
		}
		boolean closeSocket = checkKeepAliveHeader(request, responseBuilder, response.worker.getServer().isSSLEnabled());
//		resp.setHeader("Pragma", "no-cache");
//		resp.setHeader("Cache-Control", "no-cache");
//		resp.setDateHeader("Expires", 0);
		boolean dateHeaderAppended = false;
		if (modified > 0) {
			responseBuilder.append("Date: ");
			appendDateAsHTTPHeaderValue(responseBuilder, new Date());
			responseBuilder.append("\r\nLast-Modified: ");
			appendDateAsHTTPHeaderValue(responseBuilder, new Date(modified));
			responseBuilder.append("\r\n");
			dateHeaderAppended = true;
		}

		if (expired < 0) {
			Date date = new Date(System.currentTimeMillis() - 24L * 3600 * 1000);
			responseBuilder.append("Pragma: no-cache\r\nCache-Control: no-cache\r\nExpires: ");
			appendDateAsHTTPHeaderValue(responseBuilder, date);
			responseBuilder.append("\r\n");
		} else {
			if (dateHeaderAppended) {
				if (eTag != null && eTag.length() > 0) {
					responseBuilder.append("ETag: ").append(eTag).append("\r\n");
				}
			} else {
				Date now = new Date();
				responseBuilder.append("Date: ");
				appendDateAsHTTPHeaderValue(responseBuilder, now);
				responseBuilder.append("\r\nLast-Modified: ");
				appendDateAsHTTPHeaderValue(responseBuilder, now);
				if (eTag != null && eTag.length() > 0) {
					responseBuilder.append("\r\nETag: ").append(eTag);
				}
				responseBuilder.append("\r\n");
			}
			if (expired > 0) {
				responseBuilder.append("Expires: ").append(getStaticResourceExpiredDate(expired)).append("\r\n");
			}			
		}
		String charset = encoding == null ? "UTF-8" : encoding;
		int length = content.length;
		boolean toGZip = length > HttpConfig.gzipStartingSize && request.supportGZip
				&& (type.startsWith("text/") || "application/json".equals(type))
				&& isUserAgentSupportGZip(request.userAgent);
		if (toGZip) {
			content = gZipCompress(content);
			length = content.length;
			responseBuilder.append("Content-Encoding: gzip\r\n");
		}
		if (cookies != null) {
			String[] lines = cookies.split("\r\n");
			for (int i = 0; i < lines.length; i++) {
				String line = lines[i];
				if (line.length() != 0) {
					responseBuilder.append("Set-Cookie: ").append(line).append("\r\n");
				}
			}
		}
		responseBuilder.append("Content-Type: ").append(type);
		if (type.startsWith("text/")) {
			responseBuilder.append("; charset=").append(charset);
		}
		responseBuilder.append("\r\n");
		if (!checkContentLength(request, response, length, responseBuilder)) {
			responseBuilder.append("\r\n");
			byte[] data = responseBuilder.toString().getBytes();
			request.sending = data.length;
			if (response.worker != null) response.worker.getServer().send(response.socket, data);
			if (closeSocket) {
				if (response.worker != null) response.worker.poolingRequest(response.socket, request);
			}
			return null;
		}
		responseBuilder.append("\r\n");

		byte[] outBytes;
		byte[] responseBytes = responseBuilder.toString().getBytes();
		if (request.rangeBeginning >= 0 || request.rangeEnding >= 0) {
			int rangedSize = request.rangeEnding - request.rangeBeginning + 1;
			outBytes = new byte[responseBytes.length + rangedSize];
			System.arraycopy(content, request.rangeBeginning, outBytes, responseBytes.length, rangedSize);
		} else {
			outBytes = new byte[responseBytes.length + length];
			System.arraycopy(content, 0, outBytes, responseBytes.length, length);
		}
		System.arraycopy(responseBytes, 0, outBytes, 0, responseBytes.length);
		request.sending = outBytes.length;
		if (response.worker != null) response.worker.getServer().send(response.socket, outBytes);
		if (closeSocket) {
			if (response.worker != null) response.worker.poolingRequest(response.socket, request);
		}
		return outBytes;
	}

	public static void pipeOut(HttpRequest request, HttpResponse response, String type, String encoding, String content, boolean cachable) {
		StringBuilder responseBuilder = new StringBuilder(512);
		responseBuilder.append("HTTP/1.").append(request.v11 ? '1' : '0');
		responseBuilder.append((request.rangeBeginning >= 0 || request.rangeEnding >= 0) ? " 206 Partial Content\r\n" : " 200 OK\r\n");
		String serverName = HttpConfig.serverSignature;
		if (request.requestCount < 1 && serverName != null && serverName.length() > 0) {
			responseBuilder.append("Server: ").append(serverName).append("\r\n");
		}
		boolean closeSocket = checkKeepAliveHeader(request, responseBuilder, response.worker.getServer().isSSLEnabled());
//		resp.setHeader("Pragma", "no-cache");
//		resp.setHeader("Cache-Control", "no-cache");
//		resp.setDateHeader("Expires", 0);

		if (!cachable) {
			Date date = new Date(System.currentTimeMillis() - 24L * 3600 * 1000);
			responseBuilder.append("Pragma: no-cache\r\nCache-Control: no-cache\r\nExpires: ");
			appendDateAsHTTPHeaderValue(responseBuilder, date);
			responseBuilder.append("\r\n");
		}
		String charset = encoding == null ? "UTF-8" : encoding;
		int length = 0;
		byte[] bytes = null;
		try {
			bytes = content.getBytes(charset);
			length = bytes.length;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			bytes = content.getBytes();
			length = content.length();
		}
		boolean toGZip = length > HttpConfig.gzipStartingSize && request.supportGZip && type.startsWith("text/") && isUserAgentSupportGZip(request.userAgent);
		if (toGZip) {
			bytes = gZipCompress(bytes);
			length = bytes.length;
			responseBuilder.append("Content-Encoding: gzip\r\n");
		}
		responseBuilder.append("Content-Type: ").append(type);
		if (type.startsWith("text/")) {
			responseBuilder.append("; charset=").append(charset);
		}
		responseBuilder.append("\r\n");
		if (!checkContentLength(request, response, length, responseBuilder)) {
			responseBuilder.append("\r\n");
			byte[] data = responseBuilder.toString().getBytes();
			request.sending = data.length;
			if (response.worker != null) response.worker.getServer().send(response.socket, data);
			if (closeSocket) {
				if (response.worker != null) response.worker.poolingRequest(response.socket, request);
			}
			return;
		}
		responseBuilder.append("\r\n");

		byte[] outBytes;
		byte[] responseBytes = responseBuilder.toString().getBytes();
		if (request.rangeBeginning >= 0 || request.rangeEnding >= 0) {
			int rangedSize = request.rangeEnding - request.rangeBeginning + 1;
			outBytes = new byte[responseBytes.length + rangedSize];
			System.arraycopy(bytes, request.rangeBeginning, outBytes, responseBytes.length, rangedSize);
		} else {
			outBytes = new byte[responseBytes.length + bytes.length];
			System.arraycopy(bytes, 0, outBytes, responseBytes.length, bytes.length);
		}
		System.arraycopy(responseBytes, 0, outBytes, 0, responseBytes.length);
		request.sending = outBytes.length;
		response.worker.getServer().send(response.socket, outBytes);
		if (closeSocket) {
			response.worker.poolingRequest(response.socket, request);
		}
	}

	public static void pipeOutCached(HttpRequest request, HttpResponse response, String type, String encoding, String content, long expired) {
		StringBuilder responseBuilder = new StringBuilder(512);
		responseBuilder.append("HTTP/1.").append(request.v11 ? '1' : '0');
		responseBuilder.append((request.rangeBeginning >= 0 || request.rangeEnding >= 0) ? " 206 Partial Content\r\n" : " 200 OK\r\n");
		String serverName = HttpConfig.serverSignature;
		if (request.requestCount < 1 && serverName != null && serverName.length() > 0) {
			responseBuilder.append("Server: ").append(serverName).append("\r\n");
		}
		boolean closeSocket = checkKeepAliveHeader(request, responseBuilder, response.worker.getServer().isSSLEnabled());
//		resp.setHeader("Pragma", "no-cache");
//		resp.setHeader("Cache-Control", "no-cache");
//		resp.setDateHeader("Expires", 0);

		Date now = new Date();
		responseBuilder.append("Date: ");
		appendDateAsHTTPHeaderValue(responseBuilder, now);
		responseBuilder.append("\r\nLast-Modified: ");
		appendDateAsHTTPHeaderValue(responseBuilder, now);
		responseBuilder.append("\r\n");
		if (expired > 0) {
			responseBuilder.append("Expires: ").append(getStaticResourceExpiredDate(expired)).append("\r\n");
		}
		String charset = encoding == null ? "UTF-8" : encoding;
		int length = 0;
		byte[] bytes = null;
		try {
			bytes = content.getBytes(charset);
			length = bytes.length;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			bytes = content.getBytes();
			length = content.length();
		}
		boolean toGZip = length > HttpConfig.gzipStartingSize && request.supportGZip && type.startsWith("text/") && isUserAgentSupportGZip(request.userAgent);
		if (toGZip) {
			bytes = gZipCompress(bytes);
			length = bytes.length;
			responseBuilder.append("Content-Encoding: gzip\r\n");
		}
		responseBuilder.append("Content-Type: ").append(type);
		if (type.startsWith("text/")) {
			responseBuilder.append("; charset=").append(charset);
		}
		responseBuilder.append("\r\n");
		if (!checkContentLength(request, response, length, responseBuilder)) {
			responseBuilder.append("\r\n");
			byte[] data = responseBuilder.toString().getBytes();
			request.sending = data.length;
			if (response.worker != null) response.worker.getServer().send(response.socket, data);
			if (closeSocket) {
				if (response.worker != null) response.worker.poolingRequest(response.socket, request);
			}
			return;
		}
		responseBuilder.append("\r\n");

		byte[] outBytes;
		byte[] responseBytes = responseBuilder.toString().getBytes();
		if (request.rangeBeginning >= 0 || request.rangeEnding >= 0) {
			int rangedSize = request.rangeEnding - request.rangeBeginning + 1;
			outBytes = new byte[responseBytes.length + rangedSize];
			System.arraycopy(bytes, request.rangeBeginning, outBytes, responseBytes.length, rangedSize);
		} else {		
			outBytes = new byte[responseBytes.length + bytes.length];
			System.arraycopy(bytes, 0, outBytes, responseBytes.length, bytes.length);
		}
		System.arraycopy(responseBytes, 0, outBytes, 0, responseBytes.length);
		request.sending = outBytes.length;
		response.worker.getServer().send(response.socket, outBytes);
		if (closeSocket) {
			response.worker.poolingRequest(response.socket, request);
		}
	}


	public static void pipeOutCached(HttpRequest request, HttpResponse response, String type, byte[] bytes, long expired) {
		StringBuilder responseBuilder = new StringBuilder(512);
		responseBuilder.append("HTTP/1.").append(request.v11 ? '1' : '0');
		responseBuilder.append((request.rangeBeginning >= 0 || request.rangeEnding >= 0) ? " 206 Partial Content\r\n" : " 200 OK\r\n");
		String serverName = HttpConfig.serverSignature;
		if (request.requestCount < 1 && serverName != null && serverName.length() > 0) {
			responseBuilder.append("Server: ").append(serverName).append("\r\n");
		}
		boolean closeSocket = checkKeepAliveHeader(request, responseBuilder, response.worker.getServer().isSSLEnabled());
//		resp.setHeader("Pragma", "no-cache");
//		resp.setHeader("Cache-Control", "no-cache");
//		resp.setDateHeader("Expires", 0);

		Date now = new Date();
		responseBuilder.append("Date: ");
		appendDateAsHTTPHeaderValue(responseBuilder, now);
		responseBuilder.append("\r\nLast-Modified: ");
		appendDateAsHTTPHeaderValue(responseBuilder, now);
		responseBuilder.append("\r\n");
		if (expired > 0) {
			responseBuilder.append("Expires: ").append(getStaticResourceExpiredDate(expired)).append("\r\n");
		}
		
		responseBuilder.append("Content-Type: ").append(type);
//		if (type.startsWith("text/")) {
//			responseBuilder.append("; charset=");
//			responseBuilder.append(charset);
//		}
		responseBuilder.append("\r\n");
		if (!checkContentLength(request, response, bytes.length, responseBuilder)) {
			responseBuilder.append("\r\n");
			byte[] data = responseBuilder.toString().getBytes();
			request.sending = data.length;
			if (response.worker != null) response.worker.getServer().send(response.socket, data);
			if (closeSocket) {
				if (response.worker != null) response.worker.poolingRequest(response.socket, request);
			}
			return;
		}
		responseBuilder.append("\r\n");

		byte[] outBytes;
		byte[] responseBytes = responseBuilder.toString().getBytes();
		if (request.rangeBeginning >= 0 || request.rangeEnding >= 0) {
			int rangedSize = request.rangeEnding - request.rangeBeginning + 1;
			outBytes = new byte[responseBytes.length + rangedSize];
			System.arraycopy(bytes, request.rangeBeginning, outBytes, responseBytes.length, rangedSize);
		} else {
			outBytes = new byte[responseBytes.length + bytes.length];
			System.arraycopy(bytes, 0, outBytes, responseBytes.length, bytes.length);
		}
		System.arraycopy(responseBytes, 0, outBytes, 0, responseBytes.length);
		request.sending = outBytes.length;
		response.worker.getServer().send(response.socket, outBytes);
		if (closeSocket) {
			response.worker.poolingRequest(response.socket, request);
		}
	}

	public static boolean pipeChunkedHeader(HttpRequest request, HttpResponse response, String type, boolean cachable) {
		StringBuilder responseBuilder = new StringBuilder(512);
		responseBuilder.append("HTTP/1.").append(request.v11 ? '1' : '0').append(" 200 OK\r\n");
		String serverName = HttpConfig.serverSignature;
		if (request.requestCount < 1 && serverName != null && serverName.length() > 0) {
			responseBuilder.append("Server: ").append(serverName).append("\r\n");
		}
		boolean closeSocket = checkKeepAliveHeader(request, responseBuilder, response.worker.getServer().isSSLEnabled());

		if (!cachable) {
			Date date = new Date(System.currentTimeMillis() - 24L * 3600 * 1000);
			responseBuilder.append("Pragma: no-cache\r\nCache-Control: no-cache, no-transform\r\nExpires: ");
			appendDateAsHTTPHeaderValue(responseBuilder, date);
			responseBuilder.append("\r\n");
		}
		responseBuilder.append("Content-Type: ").append(type);
		if (type.startsWith("text/")) {
			responseBuilder.append("; charset=UTF-8");
		}
		if (request.rangeBeginning >= 0 && request.rangeEnding >= 0) {
			responseBuilder.append("\r\nRange: bytes=0-");
		}
		responseBuilder.append("\r\nTransfer-Encoding: chunked\r\n\r\n");
		
		response.worker.getServer().send(response.socket, responseBuilder.toString().getBytes());
		return closeSocket;
	}
	
	public static boolean pipeChunkedDataWithHeader(HttpRequest request, HttpResponse response, String type, boolean cachable, String content) {
		boolean closeSocket = false;
		StringBuilder responseBuilder = new StringBuilder(512);
		responseBuilder.append("HTTP/1.").append(request.v11 ? '1' : '0').append(" 200 OK\r\n");
		String serverName = HttpConfig.serverSignature;
		if (request.requestCount < 1 && serverName != null && serverName.length() > 0) {
			responseBuilder.append("Server: ").append(serverName).append("\r\n");
		}
		closeSocket = checkKeepAliveHeader(request, responseBuilder, response.worker.getServer().isSSLEnabled());

		if (!cachable) {
			Date date = new Date(System.currentTimeMillis() - 24L * 3600 * 1000);
			responseBuilder.append("Pragma: no-cache\r\nCache-Control: no-cache, no-transform\r\nExpires: ");
			appendDateAsHTTPHeaderValue(responseBuilder, date);
			responseBuilder.append("\r\n");
		}
		responseBuilder.append("Content-Type: ").append(type);
		if (type.startsWith("text/")) {
			responseBuilder.append("; charset=UTF-8");
		}
		responseBuilder.append("\r\nTransfer-Encoding: chunked\r\n\r\n");
		IPiledServer server = response.worker.getServer();
		if (content == null || content.length() == 0) {
			responseBuilder.append("0\r\n\r\n");
			server.send(response.socket, responseBuilder.toString().getBytes());
			return closeSocket;
		}
		String hexStr = Integer.toHexString(content.length());
		responseBuilder.append(hexStr).append("\r\n");
		responseBuilder.append(content).append("\r\n");
		server.send(response.socket, responseBuilder.toString().getBytes(ISO_8859_1));
		return closeSocket;
	}
	
	public static void pipeChunkedData(HttpRequest request, HttpResponse response, String content) {
		IPiledServer server = response.worker.getServer();
		if (content == null || content.length() == 0) {
			server.send(response.socket, "0\r\n\r\n".getBytes());
			return;
		}
		String hexStr = Integer.toHexString(content.length());
		String output = hexStr + "\r\n" + content + "\r\n";
		server.send(response.socket, output.getBytes(ISO_8859_1));
		return;
	}
	
	public static void pipeChunkedDataWithEnding(HttpRequest request, HttpResponse response, String content) {
		IPiledServer server = response.worker.getServer();
		if (content == null || content.length() == 0) {
			server.send(response.socket, "0\r\n\r\n".getBytes());
			return;
		}
		String hexStr = Integer.toHexString(content.length());
		String output = hexStr + "\r\n" + content + "\r\n0\r\n\r\n";
		server.send(response.socket, output.getBytes(ISO_8859_1));
	}

	public static boolean isUserAgentSupportGZip(String userAgent) {
		boolean supportGZip = true; // by default, modern browser support GZip
		if (userAgent != null) {
			if (userAgent.indexOf("MSIE 5") != -1) {
				supportGZip = false;
			} else if (userAgent.indexOf("MSIE 6") != -1) {
				supportGZip = (userAgent.indexOf("SV1") != -1);
			}
		}
		return supportGZip;
	}
	
	public static boolean checkContentLength(HttpRequest req, HttpResponse resp, int fileSize, StringBuilder responseBuilder) {
		if (req.rangeBeginning >= 0 || req.rangeEnding >= 0) {
			if (req.rangeBeginning < 0) {
				req.rangeBeginning = fileSize - req.rangeEnding;
				req.rangeEnding = fileSize - 1;
			}
			if (req.rangeEnding < 0 || req.rangeEnding >= fileSize) {
				req.rangeEnding = fileSize - 1;
			}
			if (req.rangeBeginning >= fileSize || req.rangeBeginning > req.rangeEnding) {
				responseBuilder.append("Content-Length: 0\r\nContent-Range: bytes ");// and content range
				responseBuilder.append(req.rangeBeginning).append('-').append(req.rangeEnding).append('/').append(fileSize).append("\r\n");
				// skip
				return false;
			}
			responseBuilder.append("Content-Length: ").append(req.rangeEnding - req.rangeBeginning + 1);
			responseBuilder.append("\r\nContent-Range: bytes ");
			responseBuilder.append(req.rangeBeginning).append('-').append(req.rangeEnding).append('/').append(fileSize).append("\r\n");
		} else {
			responseBuilder.append("Content-Length: ").append(fileSize).append("\r\n");
		}
		return true;
	}

	public static byte[] gZipCompress(byte[] src) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		GZIPOutputStream gZipOut = null;
		try {
			gZipOut = new GZIPOutputStream(out);
			gZipOut.write(src);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (gZipOut != null) {
				try {
					gZipOut.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		src = out.toByteArray();
		return src;
	}
	
    public static byte[] gZipUncompress(byte[] compressed) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        GZIPInputStream gZipIn = null;
        try {
        	gZipIn = new GZIPInputStream(new ByteArrayInputStream(compressed));
            byte[] buffer = new byte[1024];
            int len;
            while ((len = gZipIn.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return out.toByteArray();
    }

	/**
	 * Return common content type for given extension name.
	 * 
	 * @param ext
	 * @return content type
	 */
	public static String getContentType(String ext) {
		Map<String, String> mappings = MIMEConfig.types;
		if (mappings != null) {
			String type = mappings.get(ext.toLowerCase());
			if (type != null && type.length() > 0) {
				return type;
			}
		}
		return "application/octet-stream";
	}

	static void parseProperties(Map<String, String> properties, String requestData) {
		if (requestData == null) return;
		String[] datas = requestData.split("&");
		try {
			for(String prop : datas) {
				String[] propArray = prop.split("=");
				if(propArray.length != 2) continue;
				String value = URLDecoder.decode(propArray[1], "UTF-8");
				properties.put(propArray[0], value);
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	public static Map<String, String> getProperties(String requestData) {
		Map<String, String> properties = new HashMap<String, String>();
		if(requestData == null) return properties;
		parseProperties(properties, requestData);
		return properties;
	}

	public static Map<String, String> getProperties(HttpRequest req) {
		if (req == null) return null;
		HashMap<String, String> props = new HashMap<String, String>();
		if (req.requestQuery != null) {
			parseProperties(props, req.requestQuery);
		}
		Object body = req.requestBody;
		if (body != null) {
			try {
				parseProperties(props, (body instanceof String ? (String) body
						: new String((byte[]) body, "UTF-8")));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
		return props;
	}
	
	public static boolean needAuthorization(String authStr, String check) {
		if (authStr == null || check == null) {
			return true;
		}
		int idx = authStr.indexOf(" ");
		if (idx == -1) {
			return true;
		}
		String method = authStr.substring(0, idx);
		String value = authStr.substring(idx + 1);
		return !method.equals("Basic") || !check.equals(value);
	}


}
