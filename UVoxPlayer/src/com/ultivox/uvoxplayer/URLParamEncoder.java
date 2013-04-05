package com.ultivox.uvoxplayer;

import java.io.UnsupportedEncodingException;

public class URLParamEncoder {

	public static String encode(String input)
			throws UnsupportedEncodingException {

		StringBuilder resultStr = new StringBuilder();
		for (byte ch : input.getBytes("UTF-8")) {
			if (isUnsafe(ch)) {
				resultStr.append("%");
				resultStr.append(String.format("%02X", ch));
			} else {
				resultStr.append((char) ch);
			}
		}
		return resultStr.toString();
	}

	private static boolean isUnsafe(byte ch) {
		if (ch > 128 || ch < 0)
			return true;
		return " %$&+,/:;=?@<>#%".indexOf((char) ch) >= 0;
	}

}