package com.infoscient.lwps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class LWPSServer implements LWPSConstants, Runnable {
	private Map<String, ConnectionContext> connMap = new HashMap<String, ConnectionContext>();

	private Publisher[] publishers;

	private List<Subscriber> subscribers = new LinkedList<Subscriber>();

	public LWPSServer() {
		this(DEFAULT_PORT, false);
	}

	public LWPSServer(int port, boolean startShell) {
		try {
			server = new ServerSocket(port);
			if (startShell) {
				new Thread(new Shell(null, null)).start();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private ServerSocket server;

	public void run() {
		try {
			while (true) {
				Socket sock = server.accept();
				BufferedReader connReader = new BufferedReader(
						new InputStreamReader(sock.getInputStream()));
				PrintWriter connWriter = new PrintWriter(sock.getOutputStream());
				String line;
				line = connReader.readLine();
				int connectionType = -1;
				String code = null;
				if (line.equals("Header-Start")) { // Read header
					while ((line = connReader.readLine()) != null) {
						String prefix;
						if (line.startsWith(prefix = "ConnectionType:")) {
							connectionType = Integer.parseInt(line.substring(
									prefix.length()).trim());
						} else if (line.startsWith(prefix = "Code:")) {
							code = line.substring(prefix.length()).trim();
						} else if (line.equals("Header-End")) {
							break;
						}
					}
				} else {
					throw new Exception("Protocol error, header expected");
				}
				if (code == null || connectionType == -1) {
					connWriter.println(ERROR);
					connWriter.flush();
					continue;
				}
				connWriter.println(OK);
				connWriter.flush();
				handleConn(connReader, connWriter, connectionType, code);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void close() throws IOException {
		if (server != null) {
			server.close();
		}
	}

	private void handleConn(final BufferedReader connReader,
			final PrintWriter connWriter, final int connectionType,
			final String code) {
		new Thread() {
			public void run() {
				switch (connectionType) {
				case CONN_TYPE_SETUP:
					processSetup(connReader, connWriter, code);
					break;
				case CONN_TYPE_PUBLISH:
					processPublish(connReader, connWriter, code);
					break;
				case CONN_TYPE_SUBSCRIBE:
					processSubscribe(connReader, connWriter, code);
					break;
				}
			}
		}.start();
	}

	private void processSetup(BufferedReader r, PrintWriter w, String code) {
		try {
			String line;
			line = r.readLine();
			Properties props = new Properties();
			if (!line.equals("Properties-Start")) {
				throw new Exception("Protocol error, properties expected");
			}
			while ((line = r.readLine()) != null
					&& !line.equals("Properties-End")) {
				int n = line.indexOf(":");
				if (n < 0) {
					continue;
				}
				String key = line.substring(0, n);
				String value = line.substring(n + 1).trim();
				props.put(key, value);
			}
			connMap.put(code, new ConnectionContext(code, props));
			publishers = new Publisher[Integer.parseInt(props.getProperty(
					"Num-Publishers", "1"))];
			w.println(OK);
			w.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void processPublish(BufferedReader r, PrintWriter w, String code) {
		try {
			String line;
			line = r.readLine();
			Properties props = new Properties();
			if (!line.equals("Properties-Start")) {
				throw new Exception("Protocol error, properties expected");
			}
			while ((line = r.readLine()) != null
					&& !line.equals("Properties-End")) {
				int n = line.indexOf(":");
				if (n < 0) {
					continue;
				}
				String key = line.substring(0, n);
				String value = line.substring(n + 1).trim();
				props.put(key, value);
			}
			ConnectionContext ctx = connMap.get(code);
			if (ctx == null) {
				w.println(NO);
				w.flush();
				return;
			}
			int id = ctx.addPublisher(r, props);
			w.println(OK + " " + id);
			w.flush();
			ctx.initPublisher(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void processSubscribe(BufferedReader r, PrintWriter w, String code) {
		try {
			String line;
			line = r.readLine();
			Properties props = new Properties();
			if (!line.equals("Properties-Start")) {
				throw new Exception("Protocol error, properties expected");
			}
			// Read properties
			while ((line = r.readLine()) != null
					&& !line.equals("Properties-End")) {
				int n = line.indexOf(":");
				if (n < 0) {
					continue;
				}
				String key = line.substring(0, n);
				String value = line.substring(n + 1).trim();
				props.put(key, value);
			}
			ConnectionContext ctx = connMap.get(code);
			if (ctx == null) {
				w.println(NO);
				w.flush();
				return;
			}
			int id = ctx.addSubscriber(w, props);
			w.println(OK);
			w.flush();
			ctx.initSubscriber(id);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private class Shell implements LWPSConstants, Runnable {
		private BufferedReader in;

		private PrintStream out;

		private Map<String, Command> commandMap;

		public Shell(BufferedReader in, PrintStream out) {
			this.in = in;
			this.out = out;
			commandMap = new HashMap<String, Command>();
			putCommand("setup", new SetupCommand());
			putCommand("addpub", new AddPublisherCommand());
			putCommand("pub", new PublishCommand());
			putCommand("addsub", new AddSubscriberCommand());
			putCommand("quit", new QuitCommand());
			putCommand("help", new HelpCommand());
		}

		public Command getCommand(String name) {
			return commandMap.get(name);
		}

		public void putCommand(String name, Command cmd) {
			commandMap.put(name, cmd);
		}

		public void run() {
			if (in == null) {
				in = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("No shell reader specified, using STDIN");
			}
			if (out == null) {
				out = System.out;
				System.out.println("No shell reader specified, using STDOUT");
			}
			String line;
			String msg = "This shell is used to test this server. Type 'help' for "
					+ "a list of available commands";
			out.println(msg);
			out.print(PROMPT);
			try {
				while ((line = in.readLine()) != null) {
					String[] tokens = line.split("[\\s]+");
					Command cmd = commandMap.get(tokens[0]);
					if (cmd != null) {
						String[] args = new String[tokens.length - 1];
						for (int i = 1; i < tokens.length; i++) {
							args[i - 1] = tokens[i];
						}
						cmd.execute(args);
					} else {
						out.println("What? I'm not sure what you mean, sorry.");
					}
					out.print(PROMPT);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		protected class SetupCommand implements Command {
			public void execute(String[] args) {
				if (args.length != 2) {
					out.println("Usage: setup <code> <num-publishers>");
					return;
				}
				String code = args[0];
				int numPublishers = Integer.parseInt(args[1]);
				try {
					Properties props = new Properties();
					props.put("Num-Publishers", numPublishers);
					Setup.setup("127.0.0.1", code, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			public String getDescription() {
				return "Sets up a connection context";
			}
		}

		protected class AddPublisherCommand implements Command {
			public void execute(String[] args) {
				if (args.length != 1) {
					out.println("Usage: addpub <code>");
					return;
				}
				String code = args[0];
				try {
					Publisher p = new Publisher();
					p.connect("127.0.0.1", code, null);
					publishers[p.getID()] = p;
					out.println("publisher [" + p.getID() + "] added");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			public String getDescription() {
				return "Adds a publisher";
			}
		}

		protected class PublishCommand implements Command {
			public void execute(String[] args) {
				if (args.length != 2) {
					out.println("Usage: pub <publisher.id> <msg>");
					return;
				}
				int id = Integer.parseInt(args[0]);
				String msg = args[1];
				try {
					publishers[id].sendMessage(msg);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			public String getDescription() {
				return "Publishes a message";
			}
		}

		protected class AddSubscriberCommand implements Command {
			public void execute(String[] args) {
				if (args.length != 1) {
					out.println("Usage: addsub <code>");
					return;
				}
				String code = args[0];
				try {
					final int id = subscribers.size();
					final Subscriber s = new Subscriber() {
						public void receiveMessage(int pubID, String msg) {
							out.println("subscriber [" + id + "]: " + msg);
						}
					};
					s.connect("127.0.0.1", code, null);
					subscribers.add(s);
					out.println("subscriber added");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			public String getDescription() {
				return "Adds a subscriber";
			}
		}

		protected class QuitCommand implements Command {
			public void execute(String[] args) {
				System.exit(0);
			}

			public String getDescription() {
				return "Quits the shell";
			}
		}

		protected class HelpCommand implements Command {
			public void execute(String[] args) {
				String[] keys = commandMap.keySet().toArray(new String[0]);
				Arrays.sort(keys, new Comparator<String>() {
					public int compare(String o1, String o2) {
						return o1.compareToIgnoreCase(o2);
					}
				});
				String format = "%-15s%s";
				out.println(String.format(format, "COMMAND", "DESCRIPTION"));
				for (String key : keys) {
					Command cmd = getCommand(key);
					out.println(String
							.format(format, key, cmd.getDescription()));
				}
			}

			public String getDescription() {
				return "Shows a list of commands available";
			}
		}
	}

	public static void main(String[] args) {
		if (args.length == 1 && args[0].equals("-help")) {
			System.out
					.println("Usage: java LWPSServer [-port=<port>] [-shell]");
			System.exit(0);
		}
		int port = DEFAULT_PORT;
		boolean startShell = false;
		for (String s : args) {
			String prefix;
			if (s.startsWith((prefix = "-port="))) {
				port = Integer.parseInt(s.substring(prefix.length()));
			} else if (s.equals("-shell")) {
				startShell = true;
			}
		}
		new Thread(new LWPSServer(port, startShell)).start();
	}
}
