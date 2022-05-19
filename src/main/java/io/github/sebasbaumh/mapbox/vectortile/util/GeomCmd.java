package io.github.sebasbaumh.mapbox.vectortile.util;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * MVT draw command types.
 */
@NonNullByDefault
public enum GeomCmd
{
//@formatter:off
    /**
     * Close the path.
     */
    ClosePath(7, 0),
    /**
     * Create a line to this point.
     */
    LineTo(2, 2),
    /**
     * Move to point without creating a line.
     */
    MoveTo(1, 2);
//@formatter:on

	/**
	 * Unique command ID.
	 */
	private final int cmdId;

	/**
	 * Amount of parameters that follow the command.
	 */
	private final int paramCount;

	GeomCmd(int cmdId, int paramCount)
	{
		this.cmdId = cmdId;
		this.paramCount = paramCount;
	}

	/**
	 * Return matching {@link GeomCmd} for the provided cmdId, or null if there is not a matching command.
	 * @param cmdId command id to find match for
	 * @return command with matching id, or null if there is not a matching command
	 */
	@Nullable
	public static GeomCmd fromId(int cmdId)
	{
		final GeomCmd geomCmd;
		switch (cmdId)
		{
			case 1:
				geomCmd = MoveTo;
				break;
			case 2:
				geomCmd = LineTo;
				break;
			case 7:
				geomCmd = ClosePath;
				break;
			default:
				geomCmd = null;
		}
		return geomCmd;
	}

	/**
	 * @return unique command ID.
	 */
	public int getCmdId()
	{
		return cmdId;
	}

	/**
	 * @return amount of parameters that follow the command.
	 */
	public int getParamCount()
	{
		return paramCount;
	}
}
