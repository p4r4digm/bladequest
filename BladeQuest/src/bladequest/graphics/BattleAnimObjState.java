package bladequest.graphics;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Point;
import android.graphics.Rect;

public class BattleAnimObjState 
{

	public float rotation, strokeWidth, colorize;
	public int r, g, b, a;
	
	public Point size, pos1, pos2;
	
	public int frame;
	
	public boolean show, mirrored;
	
	public PosTypes posType;
	
	public Rect bmpSrcRect;
	
	Interpolatable interpObj;
	
	private boolean isSpriteAnimated;
	private int spriteIndex;
	private float frameLength;
	private long spriteStartTime;
	private List<Rect> frames;
	
	public void makeAnimated(float frameLength)
	{
		isSpriteAnimated = true;
		this.frameLength = frameLength;
		spriteIndex = 0;
	}
	
	public void addFrame(Rect frame)
	{
		frames.add(frame);
	}
	
	public void updateSpriteAnimation()
	{
		if(isSpriteAnimated && frames.size() > 0)
		{
			if(System.currentTimeMillis() - spriteStartTime >= frameLength*1000.0f)
			{
				spriteStartTime = System.currentTimeMillis();
				spriteIndex++;
				if(spriteIndex >= frames.size())
					spriteIndex = 0;
				bmpSrcRect = frames.get(spriteIndex);
			}
		}
	}
	
	public void setBmpSrcRect(int left, int top, int right, int bottom){bmpSrcRect = new Rect(left, top, right, bottom);}

	
	public void offset(BattleAnimObject parent)
	{
		if(interpObj != null) return;
		
		Point target = parent.animPos.getTarget();
		Point source = parent.animPos.getSource();
		
		switch(posType)
		{
		case Source:
			if(parent.animPos.getSource() != null)
			{
				pos1.offset(source.x, source.y);
				pos2.offset(source.x, source.y);				
			}			
			break;
		case Target:
			if(parent.animPos.getTarget() != null)
			{
				pos1.offset(target.x, target.y);
				pos2.offset(target.x, target.y);
			}			
			break;
		default:
			break;
		}
	}
	
	public void unOffset(BattleAnimObject parent)
	{
		if(interpObj != null) return;
		
		Point target = parent.animPos.getTarget();
		Point source = parent.animPos.getSource();
		
		switch(posType)
		{
		case Source:
			if(parent.animPos.getSource() != null)
			{
				pos1.offset(-source.x, -source.y);
				pos2.offset(-source.x, -source.y);				
			}			
			break;
		case Target:
			if(parent.animPos.getTarget() != null)
			{
				pos1.offset(-target.x, -target.y);
				pos2.offset(-target.x, -target.y);
			}			
			break;
		default:
			break;
		}
	}
	
	public BattleAnimObjState()
	{
		frames = new ArrayList<Rect>();
	}
	public BattleAnimObjState(int frame, PosTypes posType)
	{
		show = true;
		mirrored = false;
		rotation = 0.0f;
		
		this.frame = frame;
		this.posType = posType;
		
		pos1 = new Point(0,0);
		pos2 = new Point(0,0);
		size = new Point(0,0);
		
		frames = new ArrayList<Rect>();
	}
	public BattleAnimObjState(int frame, PosTypes posType, Interpolatable interpObj)
	{
		show = true;
		this.frame = frame;
		this.posType = posType;		
		this.interpObj = interpObj;
		
		frames = new ArrayList<Rect>();
	}
	
	public void argb(int a, int r, int g, int b)
	{
		this.a = a;
		this.r = r;
		this.g = g;
		this.b = b;
		
	}
	
	public void copyFrom(BattleAnimObjState other)
	{
		this.frame = other.frame;
		this.show = other.show;
		this.posType = other.posType;		
		
		this.interpObj = other.interpObj;
		if (interpObj != null) return;
		
		
		this.isSpriteAnimated = other.isSpriteAnimated;
		this.spriteIndex = other.spriteIndex; 
		this.frameLength = other.frameLength;
		this.frames = new ArrayList<Rect>(other.frames);
		
		
		this.mirrored = other.mirrored;
		this.rotation = other.rotation;
		this.size = new Point(other.size);
		this.strokeWidth = other.strokeWidth;
		this.r = other.r;
		this.g = other.g;
		this.b = other.b;
		this.a = other.a;
		this.pos1 = new Point(other.pos1);
		this.pos2 = new Point(other.pos2);


		this.colorize = other.colorize;
		
		if(other.bmpSrcRect != null)
			this.bmpSrcRect = new Rect(other.bmpSrcRect);
	}
	
	public BattleAnimObjState(BattleAnimObjState other)
	{
		copyFrom(other);
	}
	
	public enum PosTypes
	{
		Source,  //relative to source
		Target, //relative to target
		Screen  //absolute coordinates
	}

}
