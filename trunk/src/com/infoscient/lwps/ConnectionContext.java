package com.infoscient.lwps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

public class ConnectionContext {
	public final String code;

	private Properties props;

	private PublisherInfo[] publishers;

	private List<SubscriberInfo> subscribers;

	private static class PublisherInfo {
		public final BufferedReader reader;

		public final Properties props;

		public final List<String> messages = new ArrayList<String>();

		public PublisherInfo(BufferedReader reader, Properties props) {
			this.reader = reader;
			this.props = props;
		}
	}

	private static class SubscriberInfo {
		public final PrintWriter writer;

		public final Properties props;

		public SubscriberInfo(PrintWriter writer, Properties props) {
			this.writer = writer;
			this.props = props;
		}
	}

	public ConnectionContext(String code, Properties props) {
		this.code = code;
		this.props = props;
		publishers = new PublisherInfo[Integer.parseInt(props.getProperty(
				"Num-Publishers", "1"))];
		subscribers = new LinkedList<SubscriberInfo>();
	}

	public synchronized int addPublisher(BufferedReader r, Properties props) {
		int index = 0;
		while (publishers[index] != null) {
			index++;
		}
		if (index == publishers.length) {
			return -1;
		}
		publishers[index] = new PublisherInfo(r, props);
		new ReadThread(index, publishers[index]).start();
		return index;
	}

	public synchronized void initPublisher(int id) {
		for (SubscriberInfo si : subscribers) {
			System.out.println("writing to subscriber " + id);
			si.writer.println("Publish:" + id + "\n"
					+ createPropertyStr(publishers[id].props));
			si.writer.flush();
		}
	}

	public synchronized int addSubscriber(PrintWriter w, Properties props) {
		int index = subscribers.indexOf(w);
		if (index < 0) {
			index = subscribers.size();
			subscribers.add(new SubscriberInfo(w, props));
			w.println("Setup\n" + createPropertyStr(this.props));
			w.flush();
		}
		return index;
	}

	public synchronized void initSubscriber(int id) {
		SubscriberInfo subInfo = subscribers.get(id);
		PrintWriter w = subInfo.writer;
		for (int i = 0; i < publishers.length; i++) {
			if (publishers[i] != null) {
				w.println("Publish:" + i + "\n"
						+ createPropertyStr(publishers[i].props));
				for (String s : publishers[i].messages) {
					w.println("Message:" + i + "\n" + createMessageStr(s));
					w.flush();
				}
			}
		}
	}

	protected synchronized void write(int id, String line) {
		publishers[id].messages.add(line);
		for (SubscriberInfo si : subscribers) {
			si.writer.println("Message:" + id + "\n" + createMessageStr(line));
			si.writer.flush();
		}
	}

	private String createPropertyStr(Properties props) {
		StringBuilder sb = new StringBuilder();
		sb.append("Properties-Start\n");
		for (Entry<Object, Object> entry : (Set<Entry<Object, Object>>) props
				.entrySet()) {
			sb.append(entry.getKey() + ":" + entry.getValue() + "\n");
		}
		sb.append("Properties-End");
		return sb.toString();
	}

	private String createMessageStr(String line) {
		StringBuilder sb = new StringBuilder();
		sb.append("Body-Start\n");
		sb.append(line + "\n");
		sb.append("Body-End");
		return sb.toString();
	}

	private class ReadThread extends Thread {
		public final int id;

		private PublisherInfo pi;

		public ReadThread(int id, PublisherInfo pi) {
			this.id = id;
			this.pi = pi;
		}

		public void run() {
			try {
				String line;
				while ((line = pi.reader.readLine()) != null) {
					write(id, line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}