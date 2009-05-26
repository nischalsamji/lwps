package com.infoscient.lwps;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

public class HttpSubscriber extends LWPSComm implements ISubscriber {
	private int id = -1;

	private String host;

	private String code;

	private Properties props;

	private Thread readThread;

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
				+ "/lwpshttpproxy/subscribe/connect?");
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
		in.close();

		sb = new StringBuilder(host + "/lwpshttpproxy/subscribe/recvMessage?");
		sb.append("code=" + code);
		sb.append("&subID=" + id);
		in = new BufferedReader(new InputStreamReader(new URL(sb.toString())
				.openStream()));
		line = in.readLine();
		if (!line.equals(OK)) {
			throw new Exception("Error connecting to server");
		}
		line = in.readLine();
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
		in.close();
		readThread = new Thread() {
			public void run() {
				System.out.println("starting read thread");
				try {
					while (true) {
						String line, prefix;
						StringBuilder sb = new StringBuilder(getHost()
								+ "/lwpshttpproxy/subscribe/recvMessage?");
						sb.append("code=" + getCode());
						sb.append("&subID=" + id);
						String url = sb.toString();
						BufferedReader in = new BufferedReader(
								new InputStreamReader(new URL(url).openStream()));
						if ((line = in.readLine()) != null && (line.equals(OK))) {
							line = in.readLine();
							if (line.startsWith(prefix = "Publish:")) {
								int pubID = Integer.parseInt(line
										.substring(prefix.length()));
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
								int pubID = Integer.parseInt(line
										.substring(prefix.length()));
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
							continue;
						}
						Thread.sleep(3000);
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

	public void setup(Properties setupProps) {
	}

	public void newPublisher(int pubID, Properties pubProps) {
	}

	public void receiveMessage(int pubID, String msg) {
	}
}
