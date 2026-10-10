package dev.mobile.rlhd.opengl;

import static dev.mobile.rlhd.mobile.MobileGL.*;

public class GLFence {
	public long handle;

	public void sync() {
		sync(GL_TIMEOUT_IGNORED);
	}

	public synchronized void sync(long nanoseconds) {
		if (handle == 0)
			return;

		glClientWaitSync(handle, GL_SYNC_FLUSH_COMMANDS_BIT, nanoseconds);
		glDeleteSync(handle);
		handle = 0;
	}
}
