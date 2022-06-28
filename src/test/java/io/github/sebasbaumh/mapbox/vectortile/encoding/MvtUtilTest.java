package io.github.sebasbaumh.mapbox.vectortile.encoding;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.github.sebasbaumh.mapbox.vectortile.util.GeomCmd;
import io.github.sebasbaumh.mapbox.vectortile.util.MvtUtil;

/**
 * Test MVT utility functions.
 */
@SuppressWarnings({ "javadoc", "static-method" })
public final class MvtUtilTest
{
	@Test
	public void testHeaders()
	{
		assertEquals(9, MvtUtil.geomCmdHdr(GeomCmd.MOVE_TO, 1));
		assertEquals(1, MvtUtil.geomCmdHdr(GeomCmd.MOVE_TO, 1) >> 3);

		assertEquals(GeomCmd.MOVE_TO.getCmdId(), MvtUtil.getGeomCmdId(MvtUtil.geomCmdHdr(GeomCmd.MOVE_TO, 1)));
		assertEquals(1, MvtUtil.getGeomCmdLength(MvtUtil.geomCmdHdr(GeomCmd.MOVE_TO, 1)));

		for (GeomCmd c : GeomCmd.values())
		{
			assertEquals(c.getCmdId(), MvtUtil.geomCmdHdr(c, 1) & 0x7);
		}
	}
}
