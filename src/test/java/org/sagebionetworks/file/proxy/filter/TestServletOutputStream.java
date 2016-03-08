package org.sagebionetworks.file.proxy.filter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

public class TestServletOutputStream extends ServletOutputStream {
	
	ByteArrayOutputStream out = new ByteArrayOutputStream();

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setWriteListener(WriteListener writeListener) {
	}

	@Override
	public void write(int b) throws IOException {
		out.write(b);
	}
	
    public byte[] toByteArray() {
        return out.toByteArray();
    }

}
