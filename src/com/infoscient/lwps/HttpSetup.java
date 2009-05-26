package com.infoscient.lwps;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

public class HttpSetup extends LWPSComm {
	public static void setup(String host, String code, Properties props)
			throws Exception {
		StringBuilder sb = new StringBuilder(host
				+ "/lwpshttpproxy/setup/setup?");
		sb.append("code=" + code);
		if (props != null) {
			sb.append("&props=");
			int count = 0;
			for (Entry<Object, Object> entry : (Set<Entry<Object, Object>>) props
					.entrySet()) {
				sb.append((count > 0 ? ";" : "") + entry.getKey() + ":"
						+ entry.getValue());
				count++;
			}
		}
		BufferedReader in = new BufferedReader(new InputStreamReader(new URL(sb
				.toString()).openStream()));
		String line = in.readLine();
		if (!line.equals(OK)) {
			throw new Exception("Error connecting to server");
		}
	}
}
