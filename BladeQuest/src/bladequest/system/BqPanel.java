package bladequest.system;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import bladequest.graphics.TilePlate;
import bladequest.world.Global;


public class BqPanel extends SurfaceView 
implements SurfaceHolder.Callback
{
	private BqRenderThread renderThread;
	public BqThread updateThread;
	private PlateLoadThread plateLoader;
	private static final String TAG = BqPanel.class.getSimpleName();
	private Paint blackpaint, logText;
	
	public BqPanel(Context c)
	{
		super(c);		
	}
	
	public BqPanel(Activity activity)
	{
		super(activity);
		//register for callbacks
		//SurfaceHolder holder = getHolder();
		getHolder().addCallback(this);
		
		Log.d(TAG, "panel boot...");
		if (renderThread == null)
		{
			renderThread = new BqRenderThread(null);	
			renderThread.start();  //"running" isn't set until the surface is created.
		}
		
		if (updateThread == null)
		{
			updateThread = new BqThread(this);	
			updateThread.start();  //"running" isn't set until the surface is created.
		};

		plateLoader = new PlateLoadThread();
		
		plateLoader.start();

		
		setFocusable(true);	
		blackpaint = new Paint();
		blackpaint.setColor(Color.BLACK);
		
		logText = Global.textFactory.getTextPaint(13, Color.WHITE, Align.LEFT);
	}
	
	//@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    } 

    //@Override
    public void surfaceCreated(SurfaceHolder holder) 
    {
    	Log.d(TAG, "Surface Create");
    	
    	Global.screenWidth = getWidth();
    	Global.screenHeight = getHeight();   
    	
    	scaleImage();
    	
    	if (renderThread == null || renderThread.getState() == Thread.State.TERMINATED)
    	{
    		Log.d(TAG, "Thread terminated, creating new...");
    		renderThread = new BqRenderThread(holder);
    		renderThread.start();
    	}
    	else
    	{
        	renderThread.setHolder(holder);    		
    	}
    	renderThread.setRunning(true);

    	
    	if (updateThread == null || updateThread.getState() == Thread.State.TERMINATED)
    	{
    		Log.d(TAG, "Thread terminated, creating new...");
    		updateThread = new BqThread(this);
        	updateThread.start();    		
    	}    	
    	updateThread.setRunning(true);
    	   	
    }
    
    public void scaleImage()
    {
    	if(Global.stretchScreen)
    	{
        	int deltaX = Global.screenWidth - Global.vpWidth;
        	int deltaY = Global.screenHeight - Global.vpHeight;
        	if(deltaX >= deltaY)
        		Global.baseScale = (float)Global.screenHeight / (float)Global.vpHeight;    		
        	else
        		Global.baseScale = (float)Global.screenWidth / (float)Global.vpWidth;
        	
        	Global.baseScale -= 1.0F;
    	}
    	else
    		Global.baseScale = 0.0f;
    }
    

    //@Override
    public void surfaceDestroyed(SurfaceHolder holder) 
    {
    	destroyContext();
    }
    
    public void resume(){
    	renderThread.setRunning(true);
    	if (updateThread != null)
    	{
        	updateThread.setRunning(true);	
    	}
    	Global.bladeSong.resume();
    }
    
    public void pause()
    {
    	Global.bladeSong.pause();
    	renderThread.setRunning(false);
    	if (updateThread != null)
    	{    	
    		updateThread.setRunning(false);
    	}
    }
    
    public void destroyContext()
    {
    	Log.d(TAG, "Surface Destroy.");
    	boolean retry = true;
    	while(retry)
    	{
    		//try
    		//{
    			renderThread.setRunning(false);
    			renderThread.setHolder(null);
    			//renderThread.join();
    			updateThread.setRunning(false);
    			//updateThread.join();
    			retry = false;
    		//} 
    		//catch (InterruptedException e)
    		//{
    			//Log.d(TAG, "Thread Join FAILED.");
    		//}
    	}
    }       
    
    //@Override
    protected void draw() 
    {   
    	//canvas.clipRect(global.vpRect);
    	drawBackground();
    	    	
    	switch(Global.GameState)
    	{
    	case GS_TITLE:
    		Global.title.render();
    		
    		Global.screenFader.render();
    		break;
    	case GS_MENUTRANSITION:
    	case GS_WORLDMOVEMENT:
    	case GS_BATTLETRANSITION:
    	case GS_PAUSE:
    		if(Global.map != null)
    			drawWorld();
    		
    		
    		Global.screenFilter.save();
    		Global.screenFilter.clear();
    		if(!Global.menuButton.Closed())
    			Global.menuButton.render();
    		Global.contextMenu.render();

    		if(Global.debugButton != null)
    			Global.debugButton.render();
    		Global.screenFilter.restore();
    		
    		break;
    	case GS_BATTLE:
    		Global.battle.render();
    		break;
    	case GS_MAINMENU:
    		Global.screenFilter.save();
    		Global.screenFilter.clear();
    		Global.menu.render();
    		Global.screenFilter.restore();
    		Global.screenFader.render();
    		
    		break;
    	case GS_SAVELOADMENU:
    		Global.screenFilter.save();
    		Global.screenFilter.clear();
    		Global.saveLoadMenu.render();
    		Global.screenFader.render();
    		Global.screenFilter.restore();
			break;
    	case GS_LOADING:
    		Global.loadingScreen.render();
    		break;
    	case GS_NAMESELECT:
    		
    		Global.screenFilter.save();
    		Global.screenFilter.clear();
    		Global.nameSelect.render();
    		Global.screenFader.render();
    		
    		Global.screenFilter.restore();
    		break;
    	case GS_MERCHANT:
    		if(Global.map != null)
    			drawWorld();
    		
    		Global.screenFilter.save();
    		Global.screenFilter.clear();    		
    		Global.merchantScreen.render();
    		
    		Global.screenFilter.restore();
    		break;
    	case GS_DEBUG:
    		if(Global.map != null)
    			drawWorld();
    		
    		Global.screenFilter.save();
    		Global.screenFilter.clear();
    		Global.debugScreen.render();
    		Global.screenFilter.restore();
    		break;
    	case GS_SPUDQUEST:
    		Global.screenFilter.save();
    		Global.screenFilter.clear();
    		Global.spudQuest.render();
    		Global.screenFilter.restore();
    		break;
    		
    	}	
    	
    	Global.screenFilter.save();
		Global.screenFilter.clear();
		
    	drawForeground();
    	drawLog();
    	
    	Global.screenFilter.restore();
    }
    

    
    private void drawBackground()
    {    	
    	Global.renderer.drawColor(Color.BLACK);
    }
    
    private void drawLog()
	{
		int i = 0;
				
		if(Global.gameLog != null)
		{
			List<String> events = new ArrayList<String>(Global.gameLog);
			events = events.subList(events.size()-Math.min(10, events.size()), events.size());
			
			for(String str : events)
				Global.renderer.drawText(str, Global.vpToScreenX(0), Global.vpToScreenY(10+i++*20), logText);
		
		}
	}
    
    private void drawWorld()
    {
    	//genBG();
    	//canvas.drawBitmap(Global.background, bgSrcRect, bgDstRect, null);  
    	
    	List<TilePlate> loadList = new ArrayList<TilePlate>();
    	
    	Global.map.renderBackground(loadList);    	    	
    	Global.target.render();  	
    	Global.map.renderBackgroundObjs();  
    	if(Global.party.getElevation() <= 0)
    	{
    		Global.party.render();      	 
        	Global.map.renderForeground(loadList);
    	}
    	else
    	{    		     	 
        	Global.map.renderForeground(loadList);
        	Global.party.render(); 
    	}
    	
    	//load plates
    	if(loadList.size() >0)
    	{
    		plateLoader.addPlates(loadList);
    	}
    	
    	Global.map.renderForegroundObjs();
    	Global.renderReactionBubbles();    	
    	
    	if(Global.showScene != null)
    		Global.showScene.render();
    	
    	Global.renderAnimations();
    	
    	Global.screenFilter.save();
    	Global.screenFilter.clear();
		
    	Global.map.renderDisplayName(); 
    	
    	Global.screenFader.render();
    	
    	//draw msgbox
    	if(Global.worldMsgBox != null)
    		Global.worldMsgBox.render();
    	
    	Global.screenFilter.restore();

    }
    
    private void drawForeground()
    {   
    	Global.renderer.drawRect(0, 0, ((getWidth() - Global.vpWidth)/2), getHeight(), blackpaint, true);
    	Global.renderer.drawRect(0, 0, getWidth(), ((getHeight() - Global.vpHeight)/2), blackpaint, true);
    	Global.renderer.drawRect(getWidth() - ((getWidth() - Global.vpWidth)/2), 0, getWidth(), getHeight(), blackpaint, true);
    	Global.renderer.drawRect(0, getHeight() - ((getHeight() - Global.vpHeight)/2), getWidth(), getHeight(), blackpaint, true);
    }

}


