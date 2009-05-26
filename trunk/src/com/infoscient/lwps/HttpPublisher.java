package com.infoscient.lwps;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

public class HttpPublisher extends LWPSComm implements IPublisher {
	private int id = -1;

	private String host;

	private String code;

	private Properties props;

	public void connect(String host, String code, Properties props)
			throws Exception {
		connect(host, 0, code, props);
	}

	public void connect(String host, int port /* ignored */, String code,
			Properties props) throws Exception {
		if (!host.startsWith("http://")) {
			host = "http://" + host;
		}
		this.host = host;
		this.code = code;
		this.props = props;
		StringBuilder sb = new StringBuilder(host
				+ "/lwpshttpproxy/publish/connect?");
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
		if (!line.startsWith(OK)) {
			throw new Exception("Error connecting to server");
		}
		id = Integer.parseInt(line.substring(OK.length() + 1));
	}

	public int getID() {
		return id;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return 0;
	}

	public String getCode() {
		return code;
	}

	public Properties getProps() {
		return props;
	}

	public void sendMessage(String message) throws Exception {
		StringBuilder sb = new StringBuilder(host
				+ "/lwpshttpproxy/publish/sendMessage?");
		sb.append("code=" + code);
		sb.append("&pubID=" + id);
		sb.append("&msg=" + message);
		BufferedReader in = new BufferedReader(new InputStreamReader(new URL(sb
				.toString()).openStream()));
		String line = in.readLine();
		if (!line.equals(OK)) {
			throw new Exception("Error connecting to server");
		}
		in.close();
	}
}
