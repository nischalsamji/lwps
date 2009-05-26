package com.infoscient.lwps;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

public class Setup extends LWPSComm {
	public static void setup(String host, String code, Properties props)
			throws Exception {
		setup(host, DEFAULT_PORT, code, props);
	}

	public static void setup(String host, int port, String code,
			Properties props) throws Exception {
		if (port <= 0) {
			port = DEFAULT_PORT;
		}
		Socket s = new Socket(host, port);
		PrintStream out = new PrintStream(s.getOutputStream());
		BufferedReader in = new BufferedReader(new InputStreamReader(s
				.getInputStream()));
		String line;
		out.println("Header-Start");
		out.println("ConnectionType:" + CONN_TYPE_SETUP);
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
		if (!line.equals(OK)) {
			throw new Exception("Error connecting to server");
		}
	}
}
