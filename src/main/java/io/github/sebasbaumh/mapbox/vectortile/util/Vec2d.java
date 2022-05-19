package io.github.sebasbaumh.mapbox.vectortile.util;

import java.util.Objects;

import javax.annotation.Nullable;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * A mutable vector with x and y coordinates.
 * <p>
 * It uses ints for coordinates as in the MapBox vector tile coordinate system only ints are used.
 */
@NonNullByDefault
public class Vec2d
{
	private int x;
	private int y;

	/**
	 * Construct instance with x = 0, y = 0.
	 */
	public Vec2d()
	{
	}

	/**
	 * Construct instance with (x, y) values set to passed parameters.
	 * @param x value in x
	 * @param y value in y
	 */
	public Vec2d(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	/**
	 * Constructs instance with values from the input vector 'v'.
	 * @param v The vector
	 */
	public Vec2d(Vec2d v)
	{
		this.x = v.x;
		this.y = v.y;
	}

	/**
	 * Adds the given values to this vector. Return this vector for chaining.
	 * @param x value in x
	 * @param y value in y
	 */
	public void add(int x, int y)
	{
		this.x += x;
		this.y += y;
	}

	@Override
	public boolean equals(@Nullable Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (!(obj instanceof Vec2d))
		{
			return false;
		}
		Vec2d other = (Vec2d) obj;
		return x == other.x && y == other.y;
	}

	/**
	 * Gets the x coordinate.
	 * @return x coordinate
	 */
	public int getX()
	{
		return x;
	}

	/**
	 * Gets the y coordinate.
	 * @return y coordinate
	 */
	public int getY()
	{
		return y;
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(x, y);
	}

	/**
	 * Set the x and y values of this vector. Return this vector for chaining.
	 * @param x value in x
	 * @param y value in y
	 */
	public void set(int x, int y)
	{
		this.x = x;
		this.y = y;
	}

	/**
	 * Set the x and y values of this vector to match input vector 'v'. Return this vector for chaining.
	 * @param v contains values to copy
	 */
	public void set(Vec2d v)
	{
		this.x = v.x;
		this.y = v.y;
	}

	/**
	 * Sets the x coordinate.
	 * @param x x coordinate
	 */
	public void setX(int x)
	{
		this.x = x;
	}

	/**
	 * Sets the y coordinate.
	 * @param y y coordinate
	 */
	public void setY(int y)
	{
		this.y = y;
	}

	@Override
	public String toString()
	{
		return "(" + x + "," + y + ")";
	}
}