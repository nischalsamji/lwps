package com.infoscient.lwps;

import java.awt.Component;
import java.awt.Image;
import java.awt.MediaTracker;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.filechooser.FileFilter;

public class Utils {

	public static Image getScaledInstance(Component component, Image srcImage,
			int maxWidth, int maxHeight) {
		MediaTracker mt = new MediaTracker(component);
		mt.addImage(srcImage, 0);
		try {
			mt.waitForID(0);
			int imageWidth = srcImage.getWidth(null), imageHeight = srcImage
					.getHeight(null);
			if (imageWidth > maxWidth || imageHeight > maxHeight) {
				double s = Math.min((double) maxWidth / imageWidth,
						(double) maxHeight / imageHeight);
				return srcImage.getScaledInstance((int) (imageWidth * s),
						(int) (imageHeight * s), Image.SCALE_SMOOTH);
			}
			return srcImage;
		} catch (InterruptedException e) {
		}
		return null;
	}

	public static void copy(InputStream in, OutputStream out)
			throws IOException {
		int n;
		byte[] buf = new byte[4096];
		while ((n = in.read(buf)) >= 0) {
			out.write(buf, 0, n);
		}
		out.flush();
	}

	public static void copy(Reader r, Writer w) throws IOException {
		int n;
		char[] buf = new char[4096];
		while ((n = r.read(buf)) >= 0) {
			w.write(buf, 0, n);
		}
		w.flush();
	}

	public static void delete(File file) {
		if (file.isDirectory()) {
			for (File f : file.listFiles()) {
				delete(f);
			}
		}
		file.delete();
	}

	public static String randomAlpha(int length, boolean capsOnly) {
		byte[] buf = new byte[length];
		for (int i = 0; i < buf.length; i++) {
			int n = (int) (Math.random() * (capsOnly ? 26 : 52));
			if (n < 26) {
				n -= 10;
				buf[i] = (byte) (0x41 + n);
			} else {
				n -= 26;
				buf[i] = (byte) (0x61 + n);
			}
		}
		return new String(buf);
	}

	public static String randomAlphaNum(int length) {
		byte[] buf = new byte[length];
		for (int i = 0; i < buf.length; i++) {
			int n = (int) (Math.random() * 62);
			if (n < 10) {
				buf[i] = (byte) (0x30 + n);
			} else if (n < 36) {
				n -= 10;
				buf[i] = (byte) (0x41 + n);
			} else {
				n -= 36;
				buf[i] = (byte) (0x61 + n);
			}
		}
		return new String(buf);
	}

	// replaces all occurrences of %ENV_NAME% with System.getenv(ENV_NAME) and
	// {PROP_NAME} with System.getProperty(PROP_NAME) in a file path
	public static File parseFilename(String s) {
		int envs = s.indexOf("%");
		while (envs >= 0) {
			int enve = s.indexOf("%", envs + 1);
			if (enve > envs) {
				String env = s.substring(envs + 1, enve);
				String val = System.getenv(env);
				s = s.substring(0, envs) + (val != null ? val : "")
						+ s.substring(enve + 1);
			}
			envs = s.indexOf("{");
		}
		int props = s.indexOf("{");
		while (props >= 0) {
			int prope = s.indexOf("}", props + 1);
			if (prope > props) {
				String prop = s.substring(props + 1, prope);
				String val = System.getProperty(prop);
				s = s.substring(0, props) + (val != null ? val : "")
						+ s.substring(prope + 1);
			}
			props = s.indexOf("{");
		}
		return new File(s);
	}

	public static FileFilter[] createFilters(String[] filters) {
		List<FileFilter> list = new LinkedList<FileFilter>();
		for (String filter : filters) {
			int start = filter.indexOf("*.") + 2;
			List<String> extList = new ArrayList<String>();
			while (start >= 2 && start < filter.length()) {
				int end = start + 1;
				while (end < filter.length()
						&& Character.isLetterOrDigit(filter.charAt(end))) {
					end++;
				}
				if (start < end) {
					extList.add(filter.substring(start, end));
				}
				start = filter.indexOf("*.", start) + 2;
			}
			if (extList.size() > 0) {
				final String[] exts = extList.toArray(new String[0]);
				final String description = filter;
				list.add(new FileFilter() {
					public boolean accept(File file) {
						if (file.isDirectory()) {
							return true;
						}
						String name = file.getName();
						boolean accept = false;
						for (String ext : exts) {
							if (name.substring(name.lastIndexOf(".") + 1)
									.equals(ext)) {
								accept = true;
								break;
							}
						}
						return accept;
					}

					public String getDescription() {
						return description;
					}
				});
			}
		}
		return list.toArray(new FileFilter[0]);
	}
}
