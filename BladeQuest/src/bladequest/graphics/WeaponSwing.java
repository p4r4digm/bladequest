package bladequest.graphics;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import bladequest.world.Global;

public class WeaponSwing 
{
	private static final Point frameSize = new Point(42, 38);
	private static final Point animFrameSize = new Point(32, 32);
	//private String id;
	
	private Rect srcRect, animSrcRect;
	private int fileX, fileY;
	
	public WeaponSwing(String id, int fileX, int fileY)
	{
		this.fileX = fileX;
		this.fileY = fileY;
		//this.id = id;
		srcRect = new Rect(
				fileX * (frameSize.x * 3), 
				fileY * frameSize.y,
				fileX * (frameSize.x * 3)+(frameSize.x * 3),
				fileY * frameSize.y + frameSize.y);		
		
		animSrcRect = new Rect(
				fileX * (animFrameSize.x * 3), 
				fileY * animFrameSize.y,
				fileX * (animFrameSize.x * 3)+(animFrameSize.x * 3),
				fileY * animFrameSize.y + animFrameSize.y);	
		
	}
	
	//gets the "anim" anim.
	public AnimatedBitmap toAnimatedBitmap()
	{
		return new AnimatedBitmap()
		{
			@Override
			public BitmapFrame[] getFrames() {
				Bitmap animBmp = Global.bitmaps.get("misc/animsprites");
				BitmapFrame[] frames = new BitmapFrame[3];
				
				for (int i = 0; i < 3; ++i)
				{
					frames[i] = new BitmapFrame(
							animBmp,
							new Rect(
							(fileX * 3 + i) * (animFrameSize.x), 
							fileY * animFrameSize.y,
							(fileX * 3 + 1 + i) * (animFrameSize.x),
							fileY * animFrameSize.y + animFrameSize.y)
							);
				}
				return frames;
			}
		};
	}
	//private int[] baseColors, swingColors;
	
	public WeaponSwingDrawable genSwingDrawable(int[] base, int[] swing)
	{
		Bitmap newBmp = Bitmap.createBitmap(frameSize.x*3, frameSize.y, Config.ARGB_8888);
		Bitmap swingBmp = Global.bitmaps.get("misc/weaponswing");
				
		//baseColors = base;
		//swingColors = swing;
		
		
		for(int y = srcRect.top; y < srcRect.bottom; ++y)
			for(int x = srcRect.left; x < srcRect.right; ++x)
			{
				int c = swingBmp.getPixel(x, y);
				
				if(Color.alpha(c) == 255)
				{
					Point relativePos = new Point(x - srcRect.left, y - srcRect.top);
					boolean drawSlash = relativePos.x / frameSize.x == 1;
					int cIndex = Color.red(c) / 32;
					
					newBmp.setPixel(
							relativePos.x, 
							relativePos.y, 
							(drawSlash ? swing[cIndex] : base[cIndex]));					
				}				
			}
		
		Bitmap newAnimBmp = Bitmap.createBitmap(animFrameSize.x*3, animFrameSize.y, Config.ARGB_8888);
		Bitmap animBmp = Global.bitmaps.get("misc/animsprites");
		
		for(int y = animSrcRect.top; y < animSrcRect.bottom; ++y)
			for(int x = animSrcRect.left; x < animSrcRect.right; ++x)
			{
				int c = animBmp.getPixel(x, y);
				
				if(Color.alpha(c) == 255)
				{
					Point relativePos = new Point(x - animSrcRect.left, y - animSrcRect.top);
					int cIndex = Color.red(c) / 32;
					
					newAnimBmp.setPixel(
							relativePos.x, 
							relativePos.y, 
							swing[cIndex]);					
				}				
			}
		
		return new WeaponSwingDrawable(newBmp, frameSize, newAnimBmp, animFrameSize);
	}

}
