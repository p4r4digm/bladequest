package bladequest.bladescript.libraries;

import android.graphics.Point;
import bladequest.bladescript.LibraryWriter;
import bladequest.math.PointMath;
import bladequest.world.Global;

public class MathLibrary 
{
	public static void publishLibrary(LibraryWriter library) 
	{
		try {
			library.addAllIn(MathLibrary.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static float distance(Point p1, Point p2)
	{
		return PointMath.length(PointMath.subtract(p1, p2));
	}
	
	public static boolean equals(boolean x, boolean y)
	{
		return x == y;
	}
	
	public static boolean greater(int x, int y)
	{
		return x > y;
	}
	public static boolean greater(float x, float y)
	{
		return x > y;
	}	
	public static boolean less(int x, int y)
	{
		return x < y;
	}
	public static boolean less(float x, float y)
	{
		return x < y;
	}	
	public static int add(int x, int y)
	{
		return x + y;
	}
	public static int divide(int x, int y)
	{
		return x / y;
	}
	public static int multiply(int x, int y)
	{
		return x * y;
	}	
	public static float negate(float f)
	{
		return -f;
	}
	public static Point point(int x, int y)
	{
		return new Point(x, y);
	}

	//NOT INCLUSIVE!
	public static int randomRoll(int max)
	{
		return Global.rand.nextInt(max);
	}
}
