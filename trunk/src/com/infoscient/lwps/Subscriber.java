package com.infoscient.lwps;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

public class Subscriber extends LWPSComm implements ISubscriber {
	private BufferedReader in;

	private Thread readThread;

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
		System.out.println("sub connecting");
		if (port <= 0) {
			port = DEFAULT_PORT;
		}
		this.host = host;
		this.port = port;
		this.code = code;
		this.props = props;
		Socket s = new Socket(host, port);
		PrintStream out = new PrintStream(s.getOutputStream());
		in = new BufferedReader(new InputStreamReader(s.getInputStream()));
		String line;
		out.println("Header-Start");
		out.println("ConnectionType:" + CONN_TYPE_SUBSCRIBE);
		out.println("Code:" + code);
		out.println("Header-End");
		out.flush();
		line = in.readLine();
		System.out.println(line);
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
		System.out.println(line);
		if (!line.equals("Setup")) {
			throw new Exception("Error connecting to server");
		}
		line = in.readLine();
		Properties setupProps = new Properties();
		if (!line.equals("Properties-Start")) {
			throw new Exception("Protocol error, properties expected");
		}
		// Read properties
		while ((line = in.readLine()) != null && !line.equals("Properties-End")) {
			int n = line.indexOf(":");
			if (n < 0) {
				continue;
			}
			String key = line.substring(0, n);
			String value = line.substring(n + 1).trim();
			setupProps.put(key, value);
		}
		setup(setupProps);
		line = in.readLine();
		System.out.println(line);
		if (!line.equals(OK)) {
			throw new Exception("Error connecting to server");
		}
		readThread = new Thread() {
			public void run() {
				System.out.println("starting read thread");
				try {
					String line, prefix;
					while ((line = in.readLine()) != null) {
						System.out.println(line);
						if (line.startsWith(prefix = "Publish:")) {
							int pubID = Integer.parseInt(line.substring(prefix
									.length()));
							line = in.readLine();
							Properties pubProps = new Properties();
							if (!line.equals("Properties-Start")) {
								throw new Exception(
										"Protocol error, properties expected");
							}
							// Read properties
							while ((line = in.readLine()) != null
									&& !line.equals("Properties-End")) {
								int n = line.indexOf(":");
								if (n < 0) {
									continue;
								}
								String key = line.substring(0, n);
								String value = line.substring(n + 1).trim();
								pubProps.put(key, value);
							}
							newPublisher(pubID, pubProps);
						} else if (line.startsWith(prefix = "Message:")) {
							int pubID = Integer.parseInt(line.substring(prefix
									.length()));
							line = in.readLine();
							StringBuilder msg = new StringBuilder();
							if (!line.equals("Body-Start")) {
								throw new Exception(
										"Protocol error, message body expected");
							}
							// Read (multi-line) message
							int lc = 0;
							while ((line = in.readLine()) != null
									&& !line.equals("Body-End")) {
								msg.append((lc++ > 0 ? "\n" : "") + line);
							}
							receiveMessage(pubID, msg.toString());
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};
		readThread.start();
	}

	public Thread getReadThread() {
		return readThread;
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

	public void setup(Properties setupProps) {
	}

	public void newPublisher(int pubID, Properties pubProps) {
	}

	public void receiveMessage(int pubID, String msg) {
	}
}
