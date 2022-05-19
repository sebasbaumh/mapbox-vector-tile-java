package io.github.sebasbaumh.mapbox.vectortile.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test zig zag encoding function.
 */
@SuppressWarnings({ "javadoc", "static-method" })
public final class ZigZagTest {

    @Test
    public void encodeAndDecode() {
        assertEquals(MvtUtil.decodeZigZag(MvtUtil.encodeZigZag(0)), 0);
        assertEquals(MvtUtil.decodeZigZag(MvtUtil.encodeZigZag(10018754)), 10018754);
    }
}
