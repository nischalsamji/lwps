package com.infoscient.lwps;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

public class Publisher extends LWPSComm implements IPublisher {
	private PrintStream out;

	private int id = -1;

	private String host;

	private int port;

	private String code;

	private Properties props;

	public void connect(String host, String code, Properties props)
			throws Exception {
		connect(host, DEFAULT_PORT, code, props);
	}

	public void connect(String host, int port, String code, Properties props)
			throws Exception {
		if (port <= 0) {
			port = DEFAULT_PORT;
		}
		this.host = host;
		this.port = port;
		this.code = code;
		this.props = props;
		Socket s = new Socket(host, port);
		out = new PrintStream(s.getOutputStream());
		BufferedReader in = new BufferedReader(new InputStreamReader(s
				.getInputStream()));
		String line;
		out.println("Header-Start");
		out.println("ConnectionType:" + CONN_TYPE_PUBLISH);
		out.println("Code:" + code);
		out.println("Header-End");
		out.flush();
		line = in.readLine();
		if (!line.equals(OK)) {
			throw new Exception("Error connecting to server");
		}
		out.println("Properties-Start");
		if (props != null) {
			for (Entry<Object, Object> entry : (Set<Entry<Object, Object>>) props
					.entrySet()) {
				out.println(entry.getKey() + ":" + entry.getValue());
			}
		}
		out.println("Properties-End");
		out.flush();
		line = in.readLine();
		if (!line.startsWith(OK)) {
			throw new Exception("Error connecting to server");
		}
		id = Integer.parseInt(line.substring(LWPSConstants.OK.length() + 1));
	}

	public int getID() {
		return id;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getCode() {
		return code;
	}

	public Properties getProps() {
		return props;
	}

	public void sendMessage(String message) throws Exception {
		out.println(message);
		out.flush();
	}
}
