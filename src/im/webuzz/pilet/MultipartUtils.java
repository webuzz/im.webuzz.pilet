/*******************************************************************************
 * Copyright (c) 2010 - 2011 webuzz.im
 *
 * Author:
 *   Zhou Renjian / zhourenjian@gmail.com - initial API and implementation
 *******************************************************************************/

package im.webuzz.pilet;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Multipart posting utilities.
 * 
 * <form action="/upload-file/" method="post" enctype="multipart/form-data">
 * <input type="file" name="localfile" />
 * </form>
 * 
 * @author zhourenjian
 *
 */
public class MultipartUtils {
	
	/**
	 * Parse post data for multiple parts.
	 * @param contentType String, uploaded content type
	 * @param charset
	 * @param contentData
	 * @param map Parsed key-value map
	 */
	public static void parse(String contentType, String charset, byte[] contentData, Map<String, Object> map) {
		parseData(contentType, charset, contentData, 0, contentData.length, map);
	}

	/**
	 * Parse post data for multiple parts.
	 * 
	 * @param contentType String, uploaded content type
	 * @param charset
	 * @param contentData
	 * @param offset
	 * @param length
	 * @param map Parsed key-value map
	 */
	public static void parse(String contentType, String charset, byte[] contentData, int offset, int length, Map<String, Object> map) {
		parseData(contentType, charset, contentData, offset, length, map);
	}

	@SuppressWarnings("unchecked")
	static int parseData(String contentType, String charset, byte[] contentData, int offset, int length, Map<String, Object> map) {
		if (contentType == null) {
			return -1;
		}
		String boundary = null;
		String key = "boundary=";
		int idx = contentType.indexOf(key);
		if (idx != -1) {
			boundary = contentType.substring(idx + key.length()).trim();
		}
		if (boundary == null) {
			return -1;
		}
		
		byte[] bytes = ("--" + boundary).getBytes();
		int keyLength = bytes.length;
		int dataBegin = -1;
		//int dataEnd = -1;
		
		String nameAttribute = null;
		String fileAttribute = null;
		String type = null;

		while (offset + keyLength < length) {
			int i = keyLength;
			do {
				i--;
				if (contentData[offset + i] != bytes[i]) {
					i = -1;
					break;
				}
				if (i == 0) {
					break; // match boundary
				}
			} while (true);
			
			if (i == 0) {
				// found boundary
				if (dataBegin > 0) {
					//dataEnd = offset + keyLength; 
//					System.out.println("++*++ " + (offset - dataBegin));
//					System.out.println(new String(contentData, dataBegin + 1, offset - 2 - dataBegin - 1));
//					System.out.println("+++++");
					int dataLength = offset - 2 - dataBegin - 1;
					byte[] data = new byte[dataLength];
					System.arraycopy(contentData, dataBegin + 1, data, 0, dataLength);
					if (fileAttribute != null) {
						MultipartFile file = new MultipartFile();
						file.name = fileAttribute;
						file.content = data; //data.getBytes();
						Object object = map.get(nameAttribute);
						if (object != null) {
							if (object instanceof MultipartFile) {
								MultipartFile f = (MultipartFile) object;
								List<MultipartFile> list = new ArrayList<MultipartFile>();
								list.add(f);
								list.add(file);
								map.put(nameAttribute, list);
							} else if (object instanceof List<?>) {
								List<MultipartFile> list = (List<MultipartFile>) object;
								list.add(file);
							} else { // error!?
								map.put(nameAttribute, file); // override
							}
						} else {
							map.put(nameAttribute, file);
						}
					} else {
						String value = null;
						if (charset != null) {
							try {
								value = new String(data, charset);
							} catch (UnsupportedEncodingException e) {
								e.printStackTrace();
								value = new String(data);
							}
						} else {
							value = new String(data);
						}
						Object object = map.get(nameAttribute);
						if (object != null) {
							if (object instanceof String) {
								String v = (String) object;
								List<String> list = new ArrayList<String>();
								list.add(v);
								list.add(value);
								map.put(nameAttribute, list);
							} else if (object instanceof List<?>) {
								List<String> list = (List<String>) object;
								list.add(value);
							} else { // error!?
								map.put(nameAttribute, value); // override
							}
						} else {
							map.put(nameAttribute, value);
						}
					}

					nameAttribute = null;
					fileAttribute = null;
					type = null;

					dataBegin = -1;
					//offset += keyLength;
				}

				int k = offset + keyLength;
				if (k < contentData.length - 1) {
					byte b = contentData[k + 1];
					if (b == '-') { // --\r\n end of data
						k += 4;
						return k;
					} else if (b == '\r') { // \r\n // continue
						k += 2;
					}
				} else {
					return k; // error, EOF
				}
				dataBegin = k;
				// parse headers
				do {
					String headerName = null;
					String headerValue = null;
					int headerNameBegin = k;
					while (k < contentData.length - 1) {
						k++;
						if (contentData[k] == ':') {
							headerName = new String(contentData, headerNameBegin, k - headerNameBegin).trim();
							break;
						}
					}
					k++; // :
					int headerValueBegin = k;
					while (k < contentData.length - 1) {
						k++;
						if (contentData[k] == '\r') {
							if (charset != null) {
								try {
									headerValue = new String(contentData, headerValueBegin, k - headerValueBegin, charset).trim();
								} catch (UnsupportedEncodingException e) {
									e.printStackTrace();
									headerValue = new String(contentData, headerValueBegin, k - headerValueBegin).trim();
								}
							} else {
								headerValue = new String(contentData, headerValueBegin, k - headerValueBegin).trim();
							}
							
//							System.out.println("-----");
//							System.out.println(headerValue);
//							for (int j = headerValueBegin; j < k; j++) {
//								System.out.print(contentData[j] + " ");
//							}
//							System.out.println();
							break;
						}
					}
					k++; // \n
					
					if ("Content-Disposition".equals(headerName)) {
						int typeIndex = headerValue.indexOf(';');
						//String type = null;
						if (typeIndex != -1) {
							//type = value.substring(0, typeIndex);
							String left = headerValue.substring(typeIndex + 1);
							String[] attrs = left.split("\"\\s*;");
							
							for (int j = 0; j < attrs.length; j++) {
								String attr = attrs[j];
								int attrIndex = attr.indexOf('=');
								if (attrIndex != -1) {
									String attrName = attr.substring(0, attrIndex).trim();
									String attrValue = attr.substring(attrIndex + 1).trim();
									boolean quoteStarted = attrValue.startsWith("\"");
									boolean quoteEnded = attrValue.endsWith("\"");
									if (quoteStarted || quoteEnded) {
										int attrLength = attrValue.length();
										attrValue = attrValue.substring(quoteStarted ? 1 : 0, quoteEnded ? attrLength - 1 : attrLength);
									}
									if ("name".equalsIgnoreCase(attrName)) {
										nameAttribute = attrValue;
									} else if ("filename".equals(attrName)) {
										fileAttribute = attrValue;
									}
								}
							}
							//int nameIndex = value.indexOf(';', typeIndex + 1);
						}
					} else if ("Content-Type".equals(headerName)) {
						type = headerValue;
					}

//					System.out.println(headerName);
//					System.out.println(headerValue);
//					System.out.println("----");
					if (k < contentData.length - 1) {
						if (contentData[k + 1] == '\r') { // \r\n\r\n
							k += 2;
							dataBegin = k;
							break;
						}
					} else {
						break;
					}
				} while (true);
				offset = k;
				
				if (dataBegin != -1) {
					if (nameAttribute == null) {
						nameAttribute = "+-*";
					}
					if (type != null && type.contains("multipart/mixed")) {
						Map<String, Object> subMap = new HashMap<String, Object>();
						offset = parseData(type, charset, contentData, offset, length, subMap);
						Object value = subMap.get("+-*");
						map.put(nameAttribute, value);
					}
				}
			} else {
				offset++;
				if (offset >= length) {
					break; // reach end
				}
			}
		}
		return offset;
	}

}
