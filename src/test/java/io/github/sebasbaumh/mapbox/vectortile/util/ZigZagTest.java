package io.github.sebasbaumh.mapbox.vectortile.util;

import io.github.sebasbaumh.mapbox.vectortile.encoding.ZigZag;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test zig zag encoding function.
 */
public final class ZigZagTest {

    @Test
    public void encodeAndDecode() {
        assertEquals(ZigZag.decode(ZigZag.encode(0)), 0);
        assertEquals(ZigZag.decode(ZigZag.encode(10018754)), 10018754);
    }
}
