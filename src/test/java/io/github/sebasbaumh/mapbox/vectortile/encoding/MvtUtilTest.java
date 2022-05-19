package io.github.sebasbaumh.mapbox.vectortile.encoding;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.github.sebasbaumh.mapbox.vectortile.util.GeomCmd;
import io.github.sebasbaumh.mapbox.vectortile.util.MvtUtil;

/**
 * Test MVT utility functions.
 */
@SuppressWarnings({ "javadoc", "static-method" })
public final class MvtUtilTest {

    @Test
    public void testHeaders() {
        assertEquals(MvtUtil.geomCmdHdr(GeomCmd.MoveTo, 1), 9);
        assertEquals(MvtUtil.geomCmdHdr(GeomCmd.MoveTo, 1) >> 3, 1);

        assertEquals(MvtUtil.getGeomCmdId(MvtUtil.geomCmdHdr(GeomCmd.MoveTo, 1)), GeomCmd.MoveTo.getCmdId());
        assertEquals(MvtUtil.getGeomCmdLength(MvtUtil.geomCmdHdr(GeomCmd.MoveTo, 1)), 1);

        for (GeomCmd c : GeomCmd.values()) {
            assertEquals(MvtUtil.geomCmdHdr(c, 1) & 0x7, c.getCmdId());
        }
    }
}
