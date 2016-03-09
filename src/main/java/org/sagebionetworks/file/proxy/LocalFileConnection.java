package org.sagebionetworks.file.proxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

/**
 * An implementation of FileConnection that reads from local files.
 *
 */
public class LocalFileConnection implements FileConnection {

	final String pathPrefix;
	
	
	public LocalFileConnection(String pathPrefix) throws NotFoundException {
		if(pathPrefix == null || "".equals(pathPrefix.trim())){
			throw new IllegalArgumentException("pathPrefix cannot be null or empty");
		}
		if(!pathPrefix.endsWith(File.separator)){
			pathPrefix += File.separator;
		}
		File dir = new File(pathPrefix);
		if(!dir.exists()){
			throw new NotFoundException("The path prefix does not exist: "+pathPrefix);
		}
		this.pathPrefix = pathPrefix;
	}

	@Override
	public void getFile(String relativePath, OutputStream stream)
			throws NotFoundException, IOException {
		// resolve path to a file.
		File fileToGet = getFileForPath(relativePath);
		FileUtils.copyFile(fileToGet, stream);
	}

	@Override
	public boolean getFileRange(String relativePath, OutputStream stream,
			long startByteIndex, long endByteIndex) throws NotFoundException, IOException {
		File fileToGet = getFileForPath(relativePath);
        try {
			final FileInputStream fis = new FileInputStream(fileToGet);
			try {
				long length = endByteIndex-startByteIndex;
			    long copiedBytes = IOUtils.copyLarge(fis, stream, startByteIndex, length);
			    return copiedBytes > 0;
			} finally {
			    fis.close();
			}
		} catch (FileNotFoundException e) {
			throw new NotFoundException("File does not exist: "+fileToGet.getAbsolutePath());
		}
	}

	@Override
	public long getFileSize(String relativePath) throws NotFoundException {
		File fileToGet = getFileForPath(relativePath);
		return fileToGet.length();
	}
	
	/**
	 * Attempt to resolve the given relative path to an actual file.
	 * 
	 * @param relativePath relative path to the file.
	 * @return
	 * @throws NotFoundException If the file cannot be found.
	 */
	public File getFileForPath(String relativePath) throws NotFoundException{
		String absolutePath = this.pathPrefix+relativePath;
		File fileToGet = new File(absolutePath);
		if(!fileToGet.exists()){
			throw new NotFoundException("File does not exist: "+absolutePath);
		}
		return fileToGet;
	}

}
