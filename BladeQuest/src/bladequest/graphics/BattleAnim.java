package bladequest.graphics;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Point;

public class BattleAnim 
{
	private List<BattleAnimObject> objects;
	//private List<BattleAnimObject> visibleObjects;
	
	private List<List<BattleAnimObject>> timedObjects;
	
	private float framePeriod;
	private int frame, finalFrame;
	private int timingSection; 
	
	private boolean playing, done, loops;
	
	private long startTime, frameTime;
	private AnimationPosition animPos;
	//private Paint text;
	
	void generateTimedObjects()
	{
		int timings = Math.min(256, finalFrame/200);
		timings = Math.max(1, Math.min(timings, objects.size()));
		
		timingSection = finalFrame / timings;
		if (finalFrame % timings != 0) ++timingSection;
		if (timingSection == 0) timingSection = 1;
		
		
		timedObjects = new ArrayList<List<BattleAnimObject>>();
		for (int i = 0; i <= timings; ++i)
		{
			timedObjects.add(new ArrayList<BattleAnimObject>());
		}
		
		for (BattleAnimObject object : objects)
		{
			int firstObj = object.getStartFrame()/timingSection;
			int lastObj = object.getEndFrame()/timingSection + 1;
			for (int i = firstObj; i != lastObj; ++i)
			{
				timedObjects.get(i).add(object);
			}
		}
	}
	
	public BattleAnim(float fps)
	{
		//visibleObjects = new ArrayList<BattleAnimObject>();
		objects = new ArrayList<BattleAnimObject>();
		this.framePeriod = 1000.0f / fps;
		
		playing = false;
		//text = Global.textFactory.getTextPaint(9, Color.WHITE, Align.LEFT);
	}
	
	public BattleAnim(BattleAnim other)
	{
		this.objects = new ArrayList<BattleAnimObject>();
	//	visibleObjects = new ArrayList<BattleAnimObject>();
		this.framePeriod = other.framePeriod;
		this.loops = other.loops;
		for(BattleAnimObject obj : other.objects)
			addObject(new BattleAnimObject(obj));		
		
		playing = false;		
	}
	
	public BattleAnim(BattleAnim other, float speedModifer)
	{
		this.objects = new ArrayList<BattleAnimObject>();
		//visibleObjects = new ArrayList<BattleAnimObject>();
		this.framePeriod = other.framePeriod * speedModifer;
		this.loops = other.loops;
		for(BattleAnimObject obj : other.objects)
			addObject(new BattleAnimObject(obj));		
		
		playing = false;		
	}	
	//take the frame number from an animation and convert it to the frame time of the event
	public int syncToAnimation(int frame)
	{
		int actualFrame = (int)(getFrameLength() * (frame == -1 ? getFinalFrame() : frame));
		
		return actualFrame;
	}
	public int syncToAnimation(float animPercent)
	{
		int actualFrame = (int)(getFrameLength() * (getFinalFrame()*animPercent));
		
		return actualFrame;
	}
	public float getFrameLength(){ return framePeriod;}
	public void loop() { loops = true; }
	public void addObject(BattleAnimObject obj){objects.add(obj);}
	public boolean Done() { return done; }
	public boolean Playing() { return playing; }
	public int getFinalFrame() { findFinalFrame();return finalFrame;}
	
	public void copyLastObject(int count, int frameOffset)
	{
		BattleAnimObject obj = objects.get(objects.size() - 1);
		
		for(int i = 0; i < count; ++i)
		{
			obj = new BattleAnimObject(obj);
			for(BattleAnimObjState state : obj.states)
				state.frame += frameOffset;
			addObject(obj);
		}
	}
	
	public long getDuration()
	{
		return (long)(getFinalFrame() * framePeriod); 
	}
	
	public void play(AnimationPosition pos)
	{		
		this.animPos = pos;
		playing = true;
		done = false;
		
		frame = -1;	
		
		//start all bojects
		for(BattleAnimObject obj : objects)
			obj.start(pos);
		
		//find finalFrame
		findFinalFrame();
		
		//spatial partitioning!
		generateTimedObjects();
		
		//smooth end necessary for looping
		for(BattleAnimObject obj : objects)
			if(obj.getStartFrame() == 0 && obj.getEndFrame() == finalFrame)
				obj.alwaysDraw();
	}
	
	public void play(Point source, Point target)
	{
		
		play(new AnimationPosition() {
			private Point src, tar;
			
			public AnimationPosition init(Point source, Point target)
			{
				this.src = source;
				this.tar = target;
				
				return this;
			}			
			
			@Override
			public Point getTarget() {
				return tar;
			}
			
			@Override
			public Point getSource() {
				return src;
			}
		}.init(source, target));

			
	}
	
	private void findFinalFrame()
	{
		finalFrame = 0;
		for(BattleAnimObject obj : objects)
			finalFrame = Math.max(finalFrame, obj.getEndFrame());
	}
	
	//update currentFrame, update all objects, and end playing if past final frame
	public void update()
	{
		while (playing)
		{
			boolean update = false;
			if(frame == -1)
			{
				frame = 0;
				startTime = System.currentTimeMillis();
				frameTime = 0;
				update = true;
			}
			else
			{
				frameTime = System.currentTimeMillis() - startTime;
				update = frameTime >= framePeriod;
			}
			if (update)
			{
				//update frame, accounting for times when multiple frames were traversed
				int elapsedFrames = (int)(frameTime / framePeriod);
				frame += elapsedFrames;
				
				//update start time to the end of the last completed frame
				startTime += (int)(elapsedFrames * framePeriod);
				
				if(frame <= finalFrame)
					for(BattleAnimObject obj : timedObjects.get(frame/timingSection))
						obj.update(frame);
				else
				{
					//animation is over
					if(loops)
					{
						play(animPos);
						continue;
					}							
					else
					{
						playing = false;
						done = true;
					}
				}
			}
			break;
		}
	}
	
	public void render()
	{
		if(playing)
		{
//			//draw debug text
			
//			BattleAnimObjState workingState = objects.get(0).getworkingState();
//			BattleAnimObjState currentState = objects.get(0).getCurrentState();
//			BattleAnimObjState nextState = objects.get(0).getNextState();
//			
//			Global.renderer.drawText("Frame: "+frame, Global.vpToScreenX(0), Global.vpToScreenY(10), text);
//			Global.renderer.drawText("OBJ State Index: "+objects.get(0).getCurrentStateIndex(), Global.vpToScreenX(0), Global.vpToScreenY(20), text);
//			Global.renderer.drawText("Current Pos - x:"+currentState.pos1.x + " y:" + currentState.pos1.y, Global.vpToScreenX(0), Global.vpToScreenY(30), text);
//			Global.renderer.drawText("Working Pos - x:"+workingState.pos1.x + " y:" + workingState.pos1.y, Global.vpToScreenX(0), Global.vpToScreenY(40), text);
//			if(nextState != null)
//			Global.renderer.drawText("Next    Pos - x:"+nextState.pos1.x + " y:" + nextState.pos1.y, Global.vpToScreenX(0), Global.vpToScreenY(50), text);
//			
//			Global.renderer.drawText("State Progress:"+objects.get(0).progress*100.0f + "%", Global.vpToScreenX(0), Global.vpToScreenY(60), text);
//			
			
			for(BattleAnimObject obj : timedObjects.get(frame/timingSection))
				obj.render(frame);
		}
			
	}
	
	public static Point linearInterpolation(Point p0, Point p1, float mu)
	{
		return new Point(linearInterpolation(p0.x, p1.x, mu),
						 linearInterpolation(p0.y, p1.y, mu));
	}
	public static  int linearInterpolation(int x0, int x1, float mu)
	{
		if(x0 == x1)
			return x0;
	
		return (int)(x0 * (1.0f-mu) + x1*mu);
	}
	public static  float linearInterpolation(float x0, float x1, float mu)
	{
		if(x0 == x1)
			return x0;
		
		return x0 * (1.0f-mu) + x1*mu;
	}
	public static  int cubicInterpolation(int y0, int y1, int y2, int y3, float mu)
	{
		float a0,a1,a2,a3;
		float mu2;
		
		if(mu < 0) mu = 0;
		if(mu > 1) mu = 1;
		
		if((y0 == y1) && (y0 == y2) && (y0 == y3))
			return y0;

		mu2 = mu*mu;
		a0 = (-0.5f*y0 + 1.5f*y1 - 1.5f*y2 + 0.5f*y3);
		a1 = (y0 - 2.5f*y1 + 2.0f*y2 - 0.5f*y3);
		a2 = (-0.5f*y0 + 0.5f*y2);
		a3 = y1;
	
		return (int)(a0*mu*mu2 + a1*mu2 + a2*mu + a3);
	}

	public static float cubicInterpolation(float y0, float y1, float y2, float y3, float mu)
	{
		float a0,a1,a2,a3,mu2;
		
		if((y0 == y1) && (y0 == y2) && (y0 == y3))
			return y0;

		mu2 = mu*mu;
		a0 = -0.5f*y0 + 1.5f*y1 - 1.5f*y2 + 0.5f*y3;
		a1 = y0 - 2.5f*y1 + 2.0f*y2 - 0.5f*y3;
		a2 = -0.5f*y0 + 0.5f*y2;
		a3 = y1;
	
		return a0*mu*mu2 + a1*mu2 + a2*mu + a3 ;
	}
	public static float cosineInterpolator(float t)
	{
		return (1.0f - (float)(Math.cos(t*Math.PI)))/2.0f;
	}
	public static  int cosineInterpolation(int y1, int y2, float t)
	{
		float t2 = cosineInterpolator(t);
		return y1 == y2 ? y1 : (int)(y1*(1-t2)+y2*t2);
	}
	public static float cosineInterpolation(float y1, float y2, float t)
	{
		float t2 = cosineInterpolator(t);
		return y1 == y2 ? y1 : y1*(1-t2)+y2*t2;
	}

}
