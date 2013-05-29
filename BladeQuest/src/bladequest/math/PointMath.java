package bladequest.math;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import bladequest.graphics.BattleAnim;
import bladequest.graphics.BattleAnimObjState;
import bladequest.graphics.BattleAnimObjState.PosTypes;
import bladequest.graphics.BattleAnimObject;
import bladequest.graphics.BattleAnimObject.Types;
import bladequest.graphics.BattleSprite;
import bladequest.graphics.BattleSprite.faces;
import bladequest.world.Global;
import bladequest.world.PlayerCharacter;

public class PointMath {

	public static Point subtract(Point lhs, Point rhs)
	{
	  return new Point(lhs.x - rhs.x, lhs.y - rhs.y);
	}
	public static Point add(Point lhs, Point rhs)
	{
		  return new Point(lhs.x + rhs.x, lhs.y + rhs.y);
	}
	public static Point offset(Point lhs, float x, float y)
	{
		 return new Point(lhs.x + (int)x, lhs.y + (int)y);
	}
	public static float offsetAngle(Point vectorTo, float distance)
	{
	   float xNormalized = -vectorTo.x / distance;
	   if (xNormalized > 1.0f) xNormalized = 1.0f; //floating point stupidity
	   float angle = (float)Math.acos(xNormalized);
	   //check orientation (ghetto det check)
	   if (vectorTo.y < 0)
	   {
		   angle = (float)(Math.PI * 2  - angle);
	   }
	   return angle;
	}
	public static float angleBetween(Point p1, Point p2)
	{
		return (float)Math.atan2(p2.y-p1.y, p2.x-p1.x);
	}
	public static float length(Point p1, Point p2)
	{
		float y = (p2.y-p1.y);
	    float x = (p2.x-p1.x); 
		
	    return (float)Math.sqrt(x*x + y*y);
	}
	public static void toCenter(Point p, PlayerCharacter source)
	{
		BattleSprite battleSpr = source.getBattleSprite();
		p.offset(battleSpr.getWidth()/2, battleSpr.getHeight()/2);
	}
	public static void toTopLeft(Point p, PlayerCharacter source)
	{
		BattleSprite battleSpr = source.getBattleSprite();
		p.offset(-battleSpr.getWidth()/2, -battleSpr.getHeight()/2);
	}	
	public static List<Point> getArc(Point startPoint, Point endPoint, int steps, float arcFactor)
	{
		if (startPoint.x < endPoint.x) 
		{
			List<Point> out = getArc(endPoint, startPoint, steps, arcFactor);
			Collections.reverse(out);
			return out;
		}
		List<Point> out = new ArrayList<Point>();
		Point vectorTo = subtract(endPoint, startPoint);
		Point center = new Point(startPoint.x + (int)(vectorTo.x * 0.5f), 
								 startPoint.y + (int)(vectorTo.y * 0.5f));
		
	    float distance = (float) Math.sqrt((float)(vectorTo.x*vectorTo.x + vectorTo.y*vectorTo.y));
		float height = distance * arcFactor;
	    
	    final float stepSize = (float)(Math.PI / (steps-1));
	    
	    float offsetAngle = offsetAngle(vectorTo, distance);
	    
	    for (int i = 0; i < steps; ++i)
	    {
	    	float angle = stepSize * i;
	    	float x = (float)Math.cos((double)angle) * (distance/2);
	    	float y = (float)Math.sin((double)angle) * (-height/2);
	        Point p = getRotatedPoint(x, y, offsetAngle);
	        out.add(add(p, center));
	    }
	    	    
	    return out;
	}
		
	public static Point getRotatedPoint(float x, float y, float angle)
	{
	   //cos, sin, -sin cos
	   float cosAngle = (float)Math.cos(angle);
	   float sinAngle = (float)Math.sin(angle);
	   
	   float outX = cosAngle  * x + sinAngle * y;
	   float outY = -sinAngle * x + cosAngle * y;
	   
	   return new Point((int)outX, (int)outY);
	}
	
	public static Point getStabPoint(PlayerCharacter attacker, PlayerCharacter target)
	{
		int attackerHeight = attacker.getBattleSprite().getHeight();
		int defenderHeight = target.getBattleSprite().getHeight(); 
		
		Point startPos = attacker.getPosition(false);
		Point offset = PointMath.subtract(attacker.getPosition(true), startPos); 
		Point pointOff = target.getPosition(false);
		pointOff.x += 8 + target.getBattleSprite().getWidth();
		pointOff.y +=  defenderHeight - attackerHeight;
		
		return add(pointOff, offset);
	}
	
	public static BattleAnim buildJumpAnimation(PlayerCharacter attacker, Point from, Point to, float arcFactor, int animationTime)
	{
		//animations are relative to CENTER!
		final int steps = 6;
		//Jump anim.  get arc
		List<Point> path = PointMath.getArc(from, to, steps, arcFactor);
		
		BattleAnim anim = new BattleAnim(60.0f);
		
		BattleSprite playerSprite = attacker.getBattleSprite();
		
		BattleAnimObject baObj = new BattleAnimObject(Types.Bitmap, false, playerSprite.getBmpName());
		
		PointMath.animateAlongPath(playerSprite, playerSprite.getFrameRect(faces.Use, 0), baObj, path, animationTime);		
		
		anim.addObject(baObj);
		
		return anim;
	}
	
	public static void animateAlongPath(BattleSprite spr, Rect sprRect, BattleAnimObject insertObj, List<Point> path, int stepTime)
	{
		int step = 0;
	    for (Point p : path)
	    {
	        BattleAnimObjState state = new BattleAnimObjState(stepTime * step++, PosTypes.Screen);
			
			state.setBmpSrcRect(sprRect.left, sprRect.top, sprRect.right, sprRect.bottom);
			state.argb(255, 255, 255, 255);
			state.pos1 = p;
			state.size = new Point(spr.getWidth(), spr.getHeight());
			insertObj.addState(state);
	    }		
	}
	
	private static void jaggedPathStep(PointF start, PointF end, int iterations, float radius, List<Point> out)
	{
		
		
		float x = (start.x + end.x)/2.0f - radius + Global.rand.nextFloat() * radius * 2.0f;
		float y = (start.y + end.y)/2.0f - radius + Global.rand.nextFloat() * radius * 2.0f;
		

		if (iterations == 0)
		{
			out.add(new Point((int)x, (int)y));
			return;
		}

		PointF midPoint = new PointF(x,y);
		jaggedPathStep(start, midPoint, iterations - 1, radius/2, out);		
		
		out.add(new Point((int)x, (int)y));

		jaggedPathStep(midPoint, end, iterations - 1, radius/2, out);
	}
	public static List<Point> jaggedPath(Point start, Point end, int iterations, float radius)
	{
		List<Point> out = new ArrayList<Point>();
		out.add(start);
		
		jaggedPathStep(new PointF(start.x, start.y), new PointF(end.x, end.y), iterations, radius, out);
		
		out.add(end);
		return out;
	}
	
	public static class ForkingPath
	{
		ForkingPath(Point p)
		{
			this.p = p;
		}
		public Point p;
		public ForkingPath child;
		public ForkingPath next;  //next on this level.
		public ForkingPath last;
		void addPath(ForkingPath path)
		{
			path.next = null;
			if (child != null)
			{
				last.next = path;
				last = path;
			}
			else
			{
				last = child = path;
			}
		}
		void addChildren(ForkingPath path)
		{
			if (path.child == null) return;
			ForkingPath iter = path.child;
			while (iter != null)
			{
				ForkingPath current = iter.next;
				addPath(iter);
				iter = current;
			}
		}
	}
	
	public static ForkingPath getForkingPath(Point start, List<Point> targets, int iterations, float radius)
	{
	   //hit a random target.  Split into above and below.  generate a forking path to each from a random point (0.25 - 0.5?) and done
	   //get all targets outside a tolerance, fork, call getForkingPath on one of them randomly.  get their lists, ignore first point, add them!
		
		if (targets.isEmpty()) return new ForkingPath(start);
		
		int randomPick = Global.rand.nextInt(targets.size());
		Point finalPoint = targets.get(randomPick);
		
		//it's possible to do more binning here so that more spawn from the start line... but eh
		List<Point> above = new ArrayList<Point>();
		List<Point> below = new ArrayList<Point>();
		List<Point> beyondTolerance = new ArrayList<Point>();
		
		float x = finalPoint.x - start.x;
		float y = finalPoint.y - start.y;
		float length = (float)Math.sqrt(x*x + y*y);
		
		if (length == 0.0f) //wut
		{
			for (Point p : targets)
			{
				if (p != finalPoint)
				{
					beyondTolerance.add(p);		
				}
			}
			return getForkingPath(start, beyondTolerance, iterations, radius);
		}
		
		final float tolerance = 0.6f;
		
		for (Point p : targets)
		{
			if (p != finalPoint)
			{
				float x2 = p.x - start.x;
				float y2 = p.y - start.y;
				float l2 = (float)Math.sqrt(x2*x2 + y2*y2);
				if (l2 == 0.0f) //wtf, 0 length?  ignore
				{
					continue;
				}
				//dot
		        if ((x*x2 + y*y2)/(length*l2) < tolerance)      		
		        {
		        	beyondTolerance.add(p);
		        	continue;
		        }
		        
		        //det
		        if (x * y2 - y * x2 > 0.0f)
		        {
		        	above.add(p);
		        }
		        else
		        {
		        	below.add(p);
		        }
			}
		}
		
	
		ForkingPath outputPath = new ForkingPath(start);		
		
		ForkingPath lastPath = outputPath;
				
		List<Point> jaggedPath = jaggedPath(start, finalPoint, iterations, radius);
		
		int aboveSplit = (int)(Global.rand.nextFloat() * (jaggedPath.size()*0.55f) + jaggedPath.size()*0.1f);
		int belowSplit = (int)(Global.rand.nextFloat() * (jaggedPath.size()*0.55f) + jaggedPath.size()*0.1f);
		
		if (aboveSplit == 0) ++aboveSplit;
		if (belowSplit == 0) ++belowSplit;
		
		outputPath.addChildren(getForkingPath(start, beyondTolerance, iterations, radius));
		
		int arg = 0;
		int nextIter = Math.max(1, iterations-1);
		for (Point p : jaggedPath)
		{
			if (arg == 0)  //ignore first point in the path... it's the current position!
			{
				++arg;
				continue;
			}
			ForkingPath currentPath = new ForkingPath(p);
			
			if (arg == aboveSplit)
			{
				currentPath.addChildren(getForkingPath(p, above, nextIter, radius * 0.75f));	
			}
			if (arg == belowSplit)
			{
				currentPath.addChildren(getForkingPath(p, below, nextIter, radius * 0.75f));
			}			
			++arg;
			lastPath.addPath(currentPath);
			lastPath = currentPath;
		}
		
		return outputPath;
	}
}
