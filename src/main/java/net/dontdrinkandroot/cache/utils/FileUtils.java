package net.dontdrinkandroot.cache.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * Required methods from Apache Commons IO FileUtils.
 * 
 * @author Apache Commons IO
 */
public class FileUtils {

	private static final char WINDOWS_SEPARATOR = '\\';

	private static final char SYSTEM_SEPARATOR = File.separatorChar;

	public static final long ONE_KB = 1024;

	public static final long ONE_MB = FileUtils.ONE_KB * FileUtils.ONE_KB;

	private static final long FILE_COPY_BUFFER_SIZE = FileUtils.ONE_MB * 30;


	public static void deleteDirectory(File directory) throws IOException {

		if (!directory.exists()) {
			return;
		}

		if (!FileUtils.isSymlink(directory)) {
			FileUtils.cleanDirectory(directory);
		}

		if (!directory.delete()) {
			String message = "Unable to delete directory " + directory + ".";
			throw new IOException(message);
		}
	}


	public static boolean isSymlink(File file) throws IOException {

		if (file == null) {
			throw new NullPointerException("File must not be null");
		}
		if (FileUtils.isSystemWindows()) {
			return false;
		}
		File fileInCanonicalDir = null;
		if (file.getParent() == null) {
			fileInCanonicalDir = file;
		} else {
			File canonicalDir = file.getParentFile().getCanonicalFile();
			fileInCanonicalDir = new File(canonicalDir, file.getName());
		}

		if (fileInCanonicalDir.getCanonicalFile().equals(fileInCanonicalDir.getAbsoluteFile())) {
			return false;
		} else {
			return true;
		}
	}


	public static boolean isSystemWindows() {

		return FileUtils.SYSTEM_SEPARATOR == FileUtils.WINDOWS_SEPARATOR;
	}


	public static void cleanDirectory(File directory) throws IOException {

		if (!directory.exists()) {
			String message = directory + " does not exist";
			throw new IllegalArgumentException(message);
		}

		if (!directory.isDirectory()) {
			String message = directory + " is not a directory";
			throw new IllegalArgumentException(message);
		}

		File[] files = directory.listFiles();
		if (files == null) { // null if security restricted
			throw new IOException("Failed to list contents of " + directory);
		}

		IOException exception = null;
		for (File file : files) {
			try {
				FileUtils.forceDelete(file);
			} catch (IOException ioe) {
				exception = ioe;
			}
		}

		if (null != exception) {
			throw exception;
		}
	}


	public static void forceDelete(File file) throws IOException {

		if (file.isDirectory()) {
			FileUtils.deleteDirectory(file);
		} else {
			boolean filePresent = file.exists();
			if (!file.delete()) {
				if (!filePresent) {
					throw new FileNotFoundException("File does not exist: " + file);
				}
				String message = "Unable to delete file: " + file;
				throw new IOException(message);
			}
		}
	}


	public static void copyFile(File srcFile, File destFile) throws IOException {

		FileUtils.copyFile(srcFile, destFile, true);
	}


	public static void copyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {

		if (srcFile == null) {
			throw new NullPointerException("Source must not be null");
		}
		if (destFile == null) {
			throw new NullPointerException("Destination must not be null");
		}
		if (srcFile.exists() == false) {
			throw new FileNotFoundException("Source '" + srcFile + "' does not exist");
		}
		if (srcFile.isDirectory()) {
			throw new IOException("Source '" + srcFile + "' exists but is a directory");
		}
		if (srcFile.getCanonicalPath().equals(destFile.getCanonicalPath())) {
			throw new IOException("Source '" + srcFile + "' and destination '" + destFile + "' are the same");
		}
		File parentFile = destFile.getParentFile();
		if (parentFile != null) {
			if (!parentFile.mkdirs() && !parentFile.isDirectory()) {
				throw new IOException("Destination '" + parentFile + "' directory cannot be created");
			}
		}
		if (destFile.exists() && destFile.canWrite() == false) {
			throw new IOException("Destination '" + destFile + "' exists but is read-only");
		}
		FileUtils.doCopyFile(srcFile, destFile, preserveFileDate);
	}


	private static void doCopyFile(File srcFile, File destFile, boolean preserveFileDate) throws IOException {

		if (destFile.exists() && destFile.isDirectory()) {
			throw new IOException("Destination '" + destFile + "' exists but is a directory");
		}

		FileInputStream fis = null;
		FileOutputStream fos = null;
		FileChannel input = null;
		FileChannel output = null;
		try {
			fis = new FileInputStream(srcFile);
			fos = new FileOutputStream(destFile);
			input = fis.getChannel();
			output = fos.getChannel();
			long size = input.size();
			long pos = 0;
			long count = 0;
			while (pos < size) {
				count = size - pos > FileUtils.FILE_COPY_BUFFER_SIZE ? FileUtils.FILE_COPY_BUFFER_SIZE : size - pos;
				pos += output.transferFrom(input, pos, count);
			}
		} finally {
			FileUtils.closeQuietly(output);
			FileUtils.closeQuietly(fos);
			FileUtils.closeQuietly(input);
			FileUtils.closeQuietly(fis);
		}

		if (srcFile.length() != destFile.length()) {
			throw new IOException("Failed to copy full contents from '" + srcFile + "' to '" + destFile + "'");
		}
		if (preserveFileDate) {
			destFile.setLastModified(srcFile.lastModified());
		}
	}


	public static void closeQuietly(Closeable closeable) {

		try {
			if (closeable != null) {
				closeable.close();
			}
		} catch (IOException ioe) {
			/* Swallow */
		}
	}


	public static File getTempDirectory() {

		return new File(FileUtils.getTempDirectoryPath());
	}


	public static String getTempDirectoryPath() {

		return System.getProperty("java.io.tmpdir");
	}


	public static Collection<File> listFilesRecursive(File directory) {

		List<File> files = new ArrayList<File>();

		File[] listFiles = directory.listFiles();
		for (File listFile : listFiles) {
			if (listFile.isDirectory()) {
				FileUtils.listFilesRecursive(listFile, files);
			} else {
				files.add(listFile);
			}
		}

		return files;
	}


	private static void listFilesRecursive(File directory, List<File> files) {

		File[] listFiles = directory.listFiles();
		for (File listFile : listFiles) {
			if (listFile.isDirectory()) {
				FileUtils.listFilesRecursive(listFile, files);
			} else {
				files.add(listFile);
			}
		}
	}

}
