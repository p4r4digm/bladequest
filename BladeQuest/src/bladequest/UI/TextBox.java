package bladequest.UI;

import android.graphics.Paint;
import bladequest.world.Global;

public class TextBox 
{	
	public String text;
	public int x, y;
	
	
	public Paint textPaint;
	
	public TextBox(String text, int x, int y, Paint textPaint)
	{
		this.text = text;
		this.x = x;
		this.y = y;
		this.textPaint = new Paint(textPaint);
		

	}
	
	public void render(int x, int y)
	{
		Global.renderer.drawText(text, x + this.x, y + this.y, new Paint(textPaint));
	}
	
	

}
