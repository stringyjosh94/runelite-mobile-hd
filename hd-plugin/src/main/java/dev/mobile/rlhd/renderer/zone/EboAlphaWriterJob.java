package dev.mobile.rlhd.renderer.zone;

import java.nio.IntBuffer;
import java.util.ArrayDeque;
import lombok.extern.slf4j.Slf4j;
import dev.mobile.rlhd.utils.buffer.GLMappedBufferIntWriter;
import dev.mobile.rlhd.utils.jobs.Job;

@Slf4j
public final class EboAlphaWriterJob extends Job {
	public final ArrayDeque<Zone.AlphaModel> alphaModels = new ArrayDeque<>();
	public GLMappedBufferIntWriter.ReservedView eboAlphaView;

	@Override
	protected void onRun() {
		try {
			final IntBuffer eboAlphaBuffer = eboAlphaView.getBuffer();
			if (eboAlphaBuffer == null)
				return;

			Zone.AlphaModel m;
			while ((m = alphaModels.poll()) != null) {
				if (m.sortedFacesLen <= 0 || m.tempSortedFaces == null)
					continue;

				if (eboAlphaBuffer.remaining() < m.sortedFacesLen) {
					log.warn("Not enough space in eboAlphaBuffer for alpha faces");
					break;
				}
				eboAlphaBuffer.put(m.tempSortedFaces, 0, m.sortedFacesLen);
			}
		} finally {
			eboAlphaView = null;
		}
	}
}
