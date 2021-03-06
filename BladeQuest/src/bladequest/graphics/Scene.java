package bladequest.graphics;

//scenes are viewport-sized images that can be shown and unshown dynamically
//so the memory isn;t always being taken

import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import bladequest.graphics.drawobjects.DrawScaledBmp;
import bladequest.world.Global;

public class Scene 
{
	private boolean loaded;
	private Bitmap bmp;
	private InputStream stream;
	private Rect src, dest;
	private DrawScaledBmp dro;
	
	public Scene(InputStream stream)
	{
		this.stream = stream;
		loaded = false;
		src = new Rect(0, 0, Global.vpGridSize.x * 16, Global.vpGridSize.y * 16);
		
	}
	
	public boolean isLoaded() { return loaded; }	
	public void load()
	{
		bmp = BitmapFactory.decodeStream(stream);
		if (Global.screenFilter.isFiltering())
		{
			Bitmap unfiltered = bmp;
			bmp = Bitmap.createBitmap(unfiltered.getWidth(), unfiltered.getHeight(), Config.ARGB_8888);
			Canvas c = new Canvas(bmp);
			c.drawBitmap(unfiltered, 0.0f, 0.0f, Global.screenFilter.defaultPaint());
			unfiltered.recycle();
		}
		loaded = true;
	}	
	public void unload()
	{
		if(loaded)
		{
			if(dro != null)
				if(dro.drawn)
					bmp.recycle();
				else
					dro.unloadBitmap = true;				
			else
				bmp.recycle();
			
			loaded = false;
		}
		
	}
	
	public void render()
	{
		if(loaded)
		{
			dest = new Rect(Global.vpToScreenX(0),
					Global.vpToScreenY(0),
					Global.vpToScreenX(Global.vpWidth),
					Global.vpToScreenY(Global.vpHeight));
			
			Global.screenFilter.save();
			Global.screenFilter.clear();
			dro = (DrawScaledBmp)Global.renderer.drawBitmap(bmp, src, dest, null);
			Global.screenFilter.restore();
		}
	}
	
	
	

}
