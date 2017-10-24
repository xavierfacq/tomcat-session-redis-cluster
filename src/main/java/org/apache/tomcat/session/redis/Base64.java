package org.apache.tomcat.session.redis;

public class Base64 {

	// Mapping table from 6-bit nibbles to Base64 characters.
	private static final char[] MAP1 = new char[64];
	static {
		int i = 0;
		for (char c = 'A'; c <= 'Z'; c++) {
			MAP1[i++] = c;
		}
		for (char c = 'a'; c <= 'z'; c++) {
			MAP1[i++] = c;
		}
		for (char c = '0'; c <= '9'; c++) {
			MAP1[i++] = c;
		}
		MAP1[i++] = '+';
		MAP1[i++] = '/';
	}

	// Mapping table from Base64 characters to 6-bit nibbles.
	private static final byte[] MAP2 = new byte[128];
	static {
		for (int i = 0; i < MAP2.length; i++) {
			MAP2[i] = -1;
		}
		for (int i = 0; i < 64; i++) {
			MAP2[MAP1[i]] = (byte) i;
		}
	}

	// Dummy constructor to prevent instanciation
	private Base64() {
	}

	/**
	 * Encodes a string into Base64 format. No blanks or line breaks are
	 * inserted.
	 * 
	 * @param s
	 *            a String to be encoded.
	 * @return A String with the Base64 encoded data.
	 */
	public static String encodeString(String s) {
		return new String(encode(s.getBytes()));
	}

	/**
	 * Encodes a byte array into Base64 format. No blanks or line breaks are
	 * inserted.
	 * 
	 * @param in
	 *            an array containing the data bytes to be encoded.
	 * @return A character array with the Base64 encoded data.
	 */
	public static char[] encode(byte[] in) {
		return encode(in, in.length);
	}

	/**
	 * Encodes a byte array into Base64 format. No blanks or line breaks are
	 * inserted.
	 * 
	 * @param in
	 *            an array containing the data bytes to be encoded.
	 * @param iLen
	 *            number of bytes to process in <code>in</code>.
	 * @return A character array with the Base64 encoded data.
	 */
	public static char[] encode(byte[] in, int iLen) {
		int oDataLen = (iLen * 4 + 2) / 3; // output length without padding
		int oLen = (iLen + 2) / 3 * 4; // output length including padding
		char[] out = new char[oLen];
		int ip = 0;
		int op = 0;
		while (ip < iLen) {
			int i0 = in[ip++] & 0xff;
			int i1 = ip < iLen ? in[ip++] & 0xff : 0;
			int i2 = ip < iLen ? in[ip++] & 0xff : 0;
			int o0 = i0 >>> 2;
			int o1 = (i0 & 3) << 4 | i1 >>> 4;
			int o2 = (i1 & 0xf) << 2 | i2 >>> 6;
			int o3 = i2 & 0x3F;
			out[op++] = MAP1[o0];
			out[op++] = MAP1[o1];
			out[op] = op < oDataLen ? MAP1[o2] : '=';
			op++;
			out[op] = op < oDataLen ? MAP1[o3] : '=';
			op++;
		}
		return out;
	}

	/**
	 * Decodes a string from Base64 format.
	 * 
	 * @param s
	 *            a Base64 String to be decoded.
	 * @return A String containing the decoded data.
	 * @throws IllegalArgumentException
	 *             if the input is not valid Base64 encoded data.
	 */
	public static String decodeString(String s) {
		return new String(decode(s));
	}

	/**
	 * Decodes a byte array from Base64 format.
	 * 
	 * @param s
	 *            a Base64 String to be decoded.
	 * @return An array containing the decoded data bytes.
	 * @throws IllegalArgumentException
	 *             if the input is not valid Base64 encoded data.
	 */
	public static byte[] decode(String s) {
		return decode(s.toCharArray());
	}

	/**
	 * Decodes a byte array from Base64 format. No blanks or line breaks are
	 * allowed within the Base64 encoded data.
	 * 
	 * @param in
	 *            a character array containing the Base64 encoded data.
	 * @return An array containing the decoded data bytes.
	 * @throws IllegalArgumentException
	 *             if the input is not valid Base64 encoded data.
	 */
	public static byte[] decode(char[] in) {
		int iLen = in.length;
		if (iLen % 4 != 0) {
			throw new IllegalArgumentException("Length of Base64 encoded input string is not a " + "multiple of 4.");
		}
		while (iLen > 0 && in[iLen - 1] == '=') {
			iLen--;
		}
		int oLen = iLen * 3 / 4;
		byte[] out = new byte[oLen];
		int ip = 0;
		int op = 0;
		while (ip < iLen) {
			int i0 = in[ip++];
			int i1 = in[ip++];
			int i2 = ip < iLen ? in[ip++] : 'A';
			int i3 = ip < iLen ? in[ip++] : 'A';
			if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127) {
				throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
			}
			int b0 = MAP2[i0];
			int b1 = MAP2[i1];
			int b2 = MAP2[i2];
			int b3 = MAP2[i3];
			if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0) {
				throw new IllegalArgumentException("Illegal character in Base64 encoded data.");
			}
			int o0 = b0 << 2 | b1 >>> 4;
			int o1 = (b1 & 0xf) << 4 | b2 >>> 2;
			int o2 = (b2 & 3) << 6 | b3;
			out[op++] = (byte) o0;
			if (op < oLen) {
				out[op++] = (byte) o1;
			}
			if (op < oLen) {
				out[op++] = (byte) o2;
			}
		}
		return out;
	}
}
