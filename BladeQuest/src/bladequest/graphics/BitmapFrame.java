package bladequest.graphics;

import android.graphics.Bitmap;
import android.graphics.Rect;

public class BitmapFrame {
	Bitmap bitmap;
	Rect srcRect;
	BitmapFrame(Bitmap bitmap, Rect srcRect)
	{
		this.bitmap = bitmap;
		this.srcRect = srcRect;
	}
	BitmapFrame(BitmapFrame rhs)
	{
		this.bitmap = rhs.bitmap;
		this.srcRect = rhs.srcRect;
	}
	BitmapFrame()
	{
		
	}
}
