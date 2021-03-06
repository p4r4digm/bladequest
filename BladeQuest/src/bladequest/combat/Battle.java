package bladequest.combat;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Cap;
import android.graphics.Point;
import android.graphics.Rect;
import bladequest.UI.ListBox;
import bladequest.UI.ListBox.LBStates;
import bladequest.UI.MenuPanel;
import bladequest.UI.MenuPanel.Anchors;
import bladequest.UI.MsgBox.MsgBox;
import bladequest.UI.MsgBox.MsgBox.Options;
import bladequest.UI.MainMenu.MainMenu;
import bladequest.battleactions.BattleAction;
import bladequest.battleactions.BattleActionRunner;
import bladequest.combat.BattleEvent.ActionType;
import bladequest.combat.triggers.Condition;
import bladequest.combat.triggers.Event;
import bladequest.combatactions.CombatActionBuilder;
import bladequest.enemy.Enemy;
import bladequest.graphics.BattleAnim;
import bladequest.graphics.BattleSprite.faces;
import bladequest.observer.ObserverUpdatePool;
import bladequest.statuseffects.StatusEffect;
import bladequest.world.Ability;
import bladequest.world.Encounter;
import bladequest.world.Global;
import bladequest.world.Item;
import bladequest.world.Item.UseTypes;
import bladequest.world.PlayerCharacter;
import bladequest.world.PlayerCharacter.Action;
import bladequest.world.States;
import bladequest.world.TargetTypes;

public class Battle 
{	
	public interface BattleEndHandler
	{
		void onBattleEnd();
	};
	
	
	BattleEndHandler battleEndHandler = new BattleEndHandler() {
		public void onBattleEnd()
		{
			Global.screenFader.fadeIn(1.0f);
		}
	};
	
	private final int frameMaxHeight = 96;
	private final int frameMinHeight = 32;
	private final int partyFrameBuffer = 32;
	private final int charMoveSpeed = 7;
	private final int statFrameBuffer = 8;
	private final int charYSpacing = 48;
	private final int charPanelYSpacing = 48;
	private final int charXSpacing = 12;
	private final int mpWindowHeight = 32;
	private final int mpWindowWidth = 128;
	
	public static final int advanceDistance = 32;
	public static final Point partyPos = new Point(Global.vpWidth - 160, 32);
	
	private final String txtStart = "Tap screen to start!";
	private final String txtTargetSingle = "Select target...";
	private final String txtTargetSingleAlly = "Select ally...";
	private final String txtTargetSingleDeadAlly = "Select a fallen ally...";
	private final String txtTargetSingleEnemy = "Select enemy...";
	private final String txtTargetEnemies = "Targeting all enemies...";
	private final String txtTargetAllies = "Targeting all allies...";
	private final String txtTargetSelf = "Targeting self...";
	private final String txtTargetEverybody = "Targeting everybody!";
	private final String txtDefeat = "Annihilated...";
	
	private int selCharX;
	private boolean selCharOpening;
	private boolean selCharOpened;
	private boolean selCharClosed;
	private boolean nextChar;
	private boolean prevChar;
	private boolean nextActor;
	private boolean allowGameOver;
	private boolean battleEnding;
	
	private PlayerCharacter currentChar;
	private int currentCharIndex;
	private List<PlayerCharacter> targets;
	private List<BattleEvent> battleEvents;
	private List<DamageMarker> markers;
	private List<String> messageQueue;
	
	private Encounter encounter;

	private List<PlayerCharacter> partyList;
	
	private Rect enemyArea;
	
	private BattleMusicListener musicListener;
	
	//gfx
	private Paint selectPaint;
	private Paint statsText, smallNameText, nameText, battleText, grayBattleText, enemyBattleText,nameDisplayPaint;
	
	//menu panels
	private MenuPanel infoPanel, displayNamePanel, mpWindow, charStatusPanel;
	private MenuPanel characterPanes[];
	private ListBox mainMenu, backButton;
	
	private BattleStateMachine stateMachine;
	
	public MsgBox msgBox;
	
	private BattleActionRunner graphicalBattleActionRunner;
	
	//event stuff
	public ObserverUpdatePool<Condition> updatePool;
	private Event startTurn, onSelectStart, damageDealt;
	
	
	
	private long startTime;
	private int currentFrame; 
	
	private Outcome outcome;
	
	void updateCurrentFrame()
	{
		currentFrame = (int)(System.currentTimeMillis()-startTime);
	}
	
	
	public Battle(BattleMusicListener musicListener)
	{
		stateMachine = new BattleStateMachine();
		enemyArea = Global.vpToScreen(new Rect(0,0,partyPos.x-partyFrameBuffer, Global.vpHeight-frameMinHeight));
		targets = new ArrayList<PlayerCharacter>();
		battleEvents = new ArrayList<BattleEvent>();
		markers = new ArrayList<DamageMarker>();
		messageQueue = new ArrayList<String>();
		
		msgBox = new MsgBox();
		
		Global.screenFader.clear();
		
		this.musicListener = musicListener;
		
		updatePool = new ObserverUpdatePool<Condition>();		
		
		graphicalBattleActionRunner = new BattleActionRunner();
		
		startTime = System.currentTimeMillis();
		updateCurrentFrame();
	}
	
	private BattleState getStartState()
	{
		return new BattleState()
		{
			@Override
			public void touchActionUp(int x, int y) 
			{
				for(MenuPanel mp : characterPanes)
					if(mp.contains(x, y) && mp.obj != null)
					{
						MainMenu.populateCharStatusPanel(charStatusPanel, (PlayerCharacter)mp.obj);
						stateMachine.setState(getCharStatusState());
						return;
					}
				selectFirstChar();
				if (!getCharacterBattleAction()) nextChar = true;
			}
		};
	}
	
	
	private BattleState getSlideInState()
	{
		return new BattleState()
		{
			final static int slideTime = 300;
			final static int enemyWait = 150;
			final static int slideDistance = 400;
			final static int startWait = 250;
			
			boolean called = false;
			long startTime;
			List<Integer> xPos;
			@Override
			public void onSwitchedTo(BattleState prevState)
			{

			}			
			public void update() 
			{
				if (!called)
				{
					startTime = System.currentTimeMillis();
					xPos = new ArrayList<Integer>();
					for (Enemy e : encounter.Enemies())
					{
						xPos.add(e.getPosition().x);
					}
					called = true;
				}
				
				int time = (int)(System.currentTimeMillis()-startTime);
				for (int i = 0; i < encounter.Enemies().size();  ++i)
				{
					float t = (time - (startWait+enemyWait *(encounter.Enemies().size()-i)))/ ((float)slideTime);
					if (t > 1.0f) t = 1.0f;
					if (t < 0.0f) t = 0.0f;
					encounter.Enemies().get(i).setPosition(new Point(
							BattleAnim.linearInterpolation(xPos.get(i)-slideDistance, xPos.get(i), t), 
							encounter.Enemies().get(i).getPosition().y));
				}
				if (time > (encounter.Enemies().size()+1)*enemyWait + slideTime + startWait)
				{
					stateMachine.setState(getStartState());
				}
			}
		};
	}	
	private BattleState getSelectState()
	{
		return new BattleState()
		{
			@Override
			public void menuUpdate()
			{
				mainMenu.clearObjects();
				mainMenu.addItem("Attack", "atk", false);
				mainMenu.addItem("Item", "itm", Global.party.getUsableInventory(UseTypes.Battle).isEmpty());
				mainMenu.addItem(currentChar.getActionName(), "act", false);
				mainMenu.addItem("Guard", "grd", false);
				mainMenu.addItem(currentChar.getAbilitiesName(), "ab", currentChar.getAbilities().isEmpty());
				mainMenu.addItem("Run", "run", false);		
				mainMenu.update();
			}
			@Override
			public void onSwitchedTo(BattleState prevState)
			{
				menuUpdate();
				mainMenu.open();
				advanceChar();
				targets.clear();
				currentChar.setFace(faces.Idle);
			}
			@Override
			public void backButtonPressed()
			{
				previousCharacter();
			}
			
			@Override		
			public void onLongPress(int x, int y)
			{
				if(mainMenu.getCurrentSelectedEntry() != null)
				{
					String opt = ((String)mainMenu.getCurrentSelectedEntry().obj);
					boolean disabled = mainMenu.getCurrentSelectedEntry().Disabled();
					mainMenu.touchActionUp(x, y);

					if(opt.equals("atk"))
					{
						showMessage("Attack with currently equipped weapon.");
					}
					else if(opt.equals("itm"))
					{
						if(disabled)
							showMessage("You have no items!");
						else
							showMessage("Choose an item from your inventory to use.");
					}
					else if(opt.equals("act"))
					{
						showMessage(currentChar.getCombatAction().getDescription());
					}
					else if(opt.equals("grd"))
					{
						showMessage("Increase your defense by 50% for one turn.");
					}
					else if(opt.equals("ab"))
					{
						if(disabled)
							showMessage("You don't know any abilities!");
						else
							showMessage("Choose an ability you know to use.");
					}
					else if(opt.equals("run"))
					{
						showMessage("Attempt to run from battle.");
						showMessage("Everyone must successfully run in order to escape!");
					}					
				}
					
			}
			
			@Override			
			public void touchActionUp(int x, int y) 
			{				
				for(MenuPanel mp : characterPanes)
					if(mp.contains(x, y) && mp.obj != null)
					{
						MainMenu.populateCharStatusPanel(charStatusPanel, (PlayerCharacter)mp.obj);
						stateMachine.setState(getCharStatusState());
						return;
					}						
			
				switch(mainMenu.touchActionUp(x, y))
				{
				case Selected:
					if (mainMenu.getSelectedEntry().Disabled()) return;
					handleMenuOption((String)(mainMenu.getSelectedEntry().obj));
					break;
				case Close:
					previousCharacter();
					break;
				default:
					break;
				}
			}
			@Override
			public void touchActionDown(int x, int y)
			{
				mainMenu.touchActionDown(x, y);
			}
			@Override
			public void touchActionMove(int x, int y)
			{
				mainMenu.touchActionMove(x, y);
			}			
		};
	}
	private BattleState getSelectItemState()
	{
		return new BattleMenuState(getCombatActionBuilder())
		{
			@Override
			public void drawSelectedObject(Object obj)
			{
				mpWindow.getTextAt(0).text = "Qty: "+((Item)obj).getCount();
				mpWindow.render();
			}
			@Override
			public void onSelected(Object obj) {
				Item itm = (Item)(mainMenu.getSelectedEntry().obj);
				currentChar.setFace(faces.Ready);
				currentChar.setItemToUse(itm);
				stateMachine.setState(getTargetState(itm.getTargetType()));
			}

			@Override		
			public void onLongPress(int x, int y)
			{
				if(mainMenu.getCurrentSelectedEntry() != null)
				{
					String desc = ((Item)mainMenu.getCurrentSelectedEntry().obj).getDescription();
					mainMenu.touchActionUp(x, y);
					showMessage(desc);
					
				}
					
			}
			
			@Override
			public void addMenuItems() {
				for(Item i : Global.party.getUsableInventory(UseTypes.Battle))
					mainMenu.addItem(i.getShortName(), i, false);
			}
		};
	}
	private BattleState getSelectAbilityState()
	{
		return new BattleMenuState(getCombatActionBuilder())
		{
			@Override
			public void drawSelectedObject(Object obj)
			{
				mpWindow.getTextAt(0).text = "Cost: "+((Ability)obj).MPCost();
				mpWindow.render();
			}
			@Override
			public void onSelected(Object obj) {
				Ability ab = (Ability)(mainMenu.getSelectedEntry().obj);
				currentChar.setFace(faces.Casting);
				currentChar.setAbilityToUse(ab);
				stateMachine.setState(getTargetState(ab.TargetType()));
			}

			@Override		
			public void onLongPress(int x, int y)
			{
				if(mainMenu.getCurrentSelectedEntry() != null)
				{
					String desc = ((Ability)mainMenu.getCurrentSelectedEntry().obj).getDescription();
					mainMenu.touchActionUp(x, y);
					showMessage(desc);					
				}
					
			}
			
			@Override
			public void addMenuItems() {
				for(Ability a : currentChar.getAbilities())
					mainMenu.addItem(a.getShortName(), a, !a.isEnabled() || a.MPCost() > currentChar.getMP());
			}
		};
	}
	public BattleState getTargetState(TargetTypes targetType)
	{
		return new BattleState() {
			private TargetTypes targetType;
			BattleState initialize(TargetTypes targetType)
			{
				this.targetType = targetType;
				return this;
			}
			@Override
			public void onSwitchedTo(BattleState PrevState)
			{
				switch(targetType)
				{
				case AllAllies:
					setInfoBarText(txtTargetAllies);
					getTouchTargets(-1, -1, targetType);
					break;
				case AllEnemies:
					setInfoBarText(txtTargetEnemies);
					getTouchTargets(-1, -1, targetType);
					break;
				case Self:
					setInfoBarText(txtTargetSelf);
					getTouchTargets(-1, -1, targetType);
					break;
				case Single:
					setInfoBarText(txtTargetSingle);
					break;
				case SingleAlly:
					setInfoBarText(txtTargetSingleAlly);
					break;
				case SingleDeadAlly:
					setInfoBarText(txtTargetSingleDeadAlly);
					break;					
				case SingleEnemy:
					setInfoBarText(txtTargetSingleEnemy);
					break;
				case Everybody:
					setInfoBarText(txtTargetEverybody);
					getTouchTargets(-1, -1, targetType);
					break;
				}
				mainMenu.close();
			}

			@Override
			public void backButtonPressed()
			{
				cancelToPrevState();
			}
			@Override
			public void touchActionUp(int x, int y)
			{
				if(infoPanelContains(x, y))
				{
					cancelToPrevState();
					targets.clear();
					currentChar.setFace(faces.Idle);
				}
				else
				{
					if(targets.size() > 0)
					{
						//targets were selected
						currentChar.setTargets(new ArrayList<PlayerCharacter>(targets));
						nextCharacter();
					
					}
					else
					{
						cancelToPrevState();
						targets.clear();
						currentChar.setFace(faces.Idle);
					}
				}		
			}
			@Override
			public void touchActionDown(int x, int y)
			{
				getTouchTargets(x,y, targetType);
			}			
			@Override
			public void touchActionMove(int x, int y)
			{
				getTouchTargets(x,y, targetType);
			}			
		}.initialize(targetType);
	}
	private BattleState getActState()
	{
		return new BattleState() {
			@Override
			public void onSwitchedTo(BattleState prevState)
			{
				targets.clear();
				mainMenu.close();
				initActState();				
			}
			@Override
			public void update()
			{
				updateActStatus();
			}
		};
	}
	private BattleState getDefeatState()
	{
		return new BattleState()
		{
			@Override
			public void onSwitchedTo(BattleState prevState)
			{
				initDefeat();
			}		
			@Override
			public void update()
			{
				endOfBattleUpdate();
			}
		};
	}
	private BattleState getVictoryState()
	{
		return new BattleState() {
			@Override
			public void onSwitchedTo(BattleState prevState)
			{
				initVictory();
			}
			@Override
			public void touchActionUp(int x, int y)
			{
				if(messageQueue.size() > 0)
				{
					messageQueue.remove(0);
					if(messageQueue.isEmpty())
						infoPanel.close();
				}
			}
			@Override
			public void update()
			{
				endOfBattleUpdate();
			}			
		};
	}
	private BattleState getEscapedState()
	{
		return new BattleState() {
			@Override
			public void onSwitchedTo(BattleState prevState)
			{
				initEscaped();
			}
			@Override
			public void update()
			{
				endOfBattleUpdate();
			}			
		};
	}
	private BattleState getCharStatusState()
	{
		
		return new BattleState()
		{
			@Override
			public void onSwitchedTo(BattleState prevState) 
			{
				charStatusPanel.open();
			}
			@Override
			public void backButtonPressed() 
			{
				charStatusPanel.close();
				cancelToPrevState();
			}
			@Override
			public void touchActionUp(int x, int y) {
				if(backButton.touchActionUp(x, y) == LBStates.Selected)
				{
					charStatusPanel.close();
					cancelToPrevState();
				}
					
			}
			@Override
			public void update() {
				backButton.update();
			}
			@Override
			public void touchActionMove(int x, int y) {
				backButton.touchActionMove(x, y);
			}
			@Override
			public void touchActionDown(int x, int y) {
				backButton.touchActionDown(x, y);
			}
		};
		
	}
	
	private boolean infoPanelContains(int x, int y)
	{
		Rect r = new Rect(0, Global.vpHeight - frameMinHeight,Global.vpWidth, Global.vpHeight);
		
		return Global.vpToScreen(r).contains(x, y);
	}
	
	private void endOfBattleUpdate()
	{
		if(messageQueue.size() < 2 && Global.screenFader.isDone() && !battleOver)
			triggerEndBattle();
		else				
			if(Global.screenFader.isFadedOut() && battleOver)
			{				
				Global.map.getBackdrop().unload();
				
				//game over, man
				if(allowGameOver && isDefeated())
				{	
					Global.party.setAllowMovement(true);
					Global.clearPan();			
					Global.executeGameOver();
				}
				else
				{
					resetEscapeState();
					
					for (Enemy e : this.encounter.Enemies())
					{
						e.endBattle();
					}
					
					for (PlayerCharacter p : partyList)
					{
						p.endBattle();
					}				

					musicListener.onBattleUnload();
					battleEndHandler.onBattleEnd();
					
					Global.GameState = States.GS_WORLDMOVEMENT;	
				}		
			}
	}
	
	private void clearBattleOnlyEffects()
	{
		for (PlayerCharacter c : partyList)
		{
			c.setPositionSpecial(false);			
			for (StatusEffect effect: StatusEffect.filterList(c.getStatusEffects(), new StatusEffect.Filter()
			{
				public boolean filter(StatusEffect effect) {
					return effect.isBattleOnly();
				}
			}))
			{
				effect.onRemove(c);
			}
			c.setStatusEffects(StatusEffect.filterList(c.getStatusEffects(), new StatusEffect.Filter()
			{
				public boolean filter(StatusEffect effect) {
					return !effect.isBattleOnly();
				}
			}));
		}
	}
	private void resetEscapeState()
	{
		for(PlayerCharacter c : partyList)
			c.setEscaped(false);
	}
	
	public void startBattle(String encounter, boolean allowGameOver)
	{	
		
		this.musicListener.onStart();
		
		battleEnding = false;
		this.allowGameOver = allowGameOver;
		startTurn = new Event();
		onSelectStart = new Event();
		damageDealt = new Event();
		
		
		this.encounter = new Encounter(Global.encounters.get(encounter));
		
		if (this.encounter.slidesIn)
		{
			stateMachine.setState(getSlideInState());
		}
		else
		{
			stateMachine.setState(getStartState());	
		}
		
		
		for (Enemy e : this.encounter.Enemies())
		{
			e.startBattle();
		}
		
		partyList = Global.party.getPartyList(false);
		
		selectFirstChar();
		
		//all characters are dead (this should never happen)
		if(currentChar == null)
			stateMachine.setState(getDefeatState());
		else
		{
			//characters
			PlayerCharacter[] party = Global.party.getPartyMembers(false);
			for(int i = 0; i < 4; ++i)
				if(party[i] != null)
				{
					party[i].setIndex(i);
					party[i].genWeaponSwing();
					party[i].setFace(faces.Idle);
					party[i].setImageIndex(0);
					party[i].startBattle();
				}
			
			//init
			recedeChar();	
			
			//GFX
			Global.map.getBackdrop().load();
			buildPaints();
			
			this.encounter.onStart();
			//initial triggering
			startTurn.trigger();
			
			//UI
			initUI();		
			update();
			
			setInfoBarText(txtStart);
			
			
		}		
	}
	
	private void initUI()
	{
		buildCharacterPanes();
		buildPanels();
		buildMainMenu();
		
		charStatusPanel = new MenuPanel(Global.vpWidth/2, Global.vpHeight/2, 0, 0);
		charStatusPanel.anchor = Anchors.TrueCenter;
		charStatusPanel.setOpenSize(Global.vpWidth,  Global.vpHeight);
		charStatusPanel.openSpeed = 30;
		
	}
	private void buildCharacterPanes()
	{
		characterPanes = new MenuPanel[4];
		for(int i = 0; i < 4; ++i)
		{
			characterPanes[i] = new MenuPanel(
					partyPos.x + 64 + charXSpacing * i, 
					partyPos.y + statFrameBuffer + charPanelYSpacing*i,
					61,
					charPanelYSpacing - statFrameBuffer);
			characterPanes[i].thickFrame = false;
		}
	}
	private void buildPanels()
	{		
		infoPanel = new MenuPanel(0, Global.vpHeight, Global.vpWidth, 0);
		infoPanel.setOpenSize(Global.vpWidth, frameMinHeight);
		
		infoPanel.addTextBox(txtStart, 5, infoPanel.openSize.y/2, battleText);

		infoPanel.setOpened();
		infoPanel.anchor = Anchors.BottomLeft;
		infoPanel.thickFrame = false;
		infoPanel.update();
		
		displayNamePanel = new MenuPanel();
		displayNamePanel.thickFrame = false;
		displayNamePanel.setInset(-10, -7);
		displayNamePanel.addTextBox("", 0, 0, nameDisplayPaint);	
		displayNamePanel.hide(); 
		
		mpWindow = new MenuPanel(0, Global.vpHeight - frameMaxHeight-mpWindowHeight, mpWindowWidth, mpWindowHeight);
		mpWindow.thickFrame = false;
		mpWindow.addTextBox("", 5, mpWindowHeight/2, battleText);
		//mpWindow.hide();
		
		backButton = new ListBox(Global.vpWidth, 0, MainMenu.menuWidth, MainMenu.barHeight, 1, 1, nameDisplayPaint);
		backButton.addItem("BACK", null, false);
		backButton.anchor = Anchors.TopRight;
	}
	private void buildMainMenu()
	{
		mainMenu = new ListBox(0, Global.vpHeight, partyPos.x - partyFrameBuffer, 0, 3, 2, battleText);
		mainMenu.setOpenSize(mainMenu.width, frameMaxHeight);
		mainMenu.setDisabledPaint(grayBattleText);
		mainMenu.anchor = Anchors.BottomLeft;
		mainMenu.thickFrame = false;
		mainMenu.hideOnClosed = true;
	}
	private void buildPaints()
	{		
		selectPaint = new Paint();
		selectPaint.setARGB(255, 255, 255, 255);
		selectPaint.setStyle(Paint.Style.STROKE);
		selectPaint.setStrokeWidth(4);
		selectPaint.setStrokeCap(Cap.ROUND);
		selectPaint.setAntiAlias(true);
		
		statsText = Global.textFactory.getTextPaint(8, Color.WHITE, Align.LEFT);
		smallNameText = Global.textFactory.getTextPaint(7, Color.WHITE, Align.LEFT);
		nameText = Global.textFactory.getTextPaint(10, Color.WHITE, Align.LEFT);
		battleText = Global.textFactory.getTextPaint(13, Color.WHITE, Align.LEFT);
		grayBattleText = Global.textFactory.getTextPaint(13, Color.GRAY, Align.LEFT);
		enemyBattleText = Global.textFactory.getTextPaint(13, Color.WHITE, Align.CENTER);
		nameDisplayPaint = Global.textFactory.getTextPaint(13, Color.WHITE, Align.CENTER);
		
	}
	
	private void updateCharacterPanes()
	{		
		for(PlayerCharacter c : partyList)
		{
			int i = c.Index();		
			
			if (c.getEscaped())
			{
				characterPanes[i].hide();
				continue;
			}			
			
			characterPanes[i].clear();		
			characterPanes[i].obj = c;
			
			
			if (c.getDisplayName().length() > 6)
				characterPanes[i].addTextBox(c.getDisplayName(), 5, (int)((charPanelYSpacing - statFrameBuffer)*0.25f), smallNameText);
			else
				characterPanes[i].addTextBox(c.getDisplayName(), 5, (int)((charPanelYSpacing - statFrameBuffer)*0.25f), nameText);
			
			characterPanes[i].addTextBox("HP:" + c.getHP(), 7, (int)((charPanelYSpacing - statFrameBuffer)*0.50f), statsText);
			characterPanes[i].addTextBox("MP:" + c.getMP(), 7, (int)((charPanelYSpacing - statFrameBuffer)*0.75f), statsText);
			
			float iconScale = 1.5f;
			int j = 0, d = (int)(Global.iconSize*iconScale + 2);
			for(StatusEffect se : c.getStatusEffects())
				if(j < 4 && !se.isHidden() && se.icon().length() > 0)
					characterPanes[i].addPicBox(Global.createIcon(se.icon(),(d/2) +  d*j++, 0,iconScale));
			
			
			characterPanes[i].update();
			
		}
	}
	private void updatePanels()
	{	
		displayNamePanel.update();
		mpWindow.update();
		mainMenu.update();
		
		if(!(mainMenu.Closing() || mainMenu.Closed()) && infoPanel.Opened())
			infoPanel.close();
		
		infoPanel.update();
		if(messageQueue.size() > 0)
			infoPanel.getTextAt(0).text = messageQueue.get(0);
	}
	private void updateCharacterPositions()
	{
		for(PlayerCharacter c : partyList)
		{
			if (c.getMirroredSpecial() == false)
			{
				c.setMirrored(c.getDefaultMirrored());
			}
			if (c.getPositionSpecial()) continue;
			c.setPosition(partyPos.x + (charXSpacing * c.Index()), partyPos.y + (charYSpacing * c.Index()));
		}
		
		for (PlayerCharacter c : encounter.Enemies())
		{
			if (c.getMirroredSpecial() == false)
			{
				c.setMirrored(c.getDefaultMirrored());
			}
		}

		if(currentChar != null && currentChar.getPositionSpecial() == false)	
			currentChar.setPosition(partyPos.x + (charXSpacing * currentChar.Index()) - selCharX, partyPos.y + (charYSpacing * currentChar.Index()));
	}

	private void showDisplayName(String str)
	{
		displayNamePanel.show();		
		
		Rect displayNameRect = new Rect();
		nameDisplayPaint.getTextBounds(str, 0, str.length()-1, displayNameRect);
		
		displayNameRect = Global.screenToVP(displayNameRect);
		displayNameRect.inset(-10, 0);
		
		
		displayNamePanel.pos.x = (Global.vpWidth - displayNameRect.width())/2;
		displayNamePanel.pos.y = 0;
		displayNamePanel.width = displayNameRect.width();
		displayNamePanel.height = displayNameRect.height();
		displayNamePanel.update();
		
		displayNamePanel.getTextAt(0).text = str;
		displayNamePanel.getTextAt(0).x = displayNamePanel.insetWidth()/2;
		displayNamePanel.getTextAt(0).y = displayNamePanel.insetHeight()/2;

	}
	private void updateDamageMarkers()
	{
		List<DamageMarker> delete = new ArrayList<DamageMarker>();
		
		for(DamageMarker dm : markers)
			if(dm.isDone())
				delete.add(dm);
			else
				dm.update();
		
		for(DamageMarker dm : delete)
			markers.remove(dm);		
		
	}
	
	private void initVictory()
	{
		outcome = Outcome.Victory;
		messageQueue.clear();
		
		musicListener.onVictory();

		clearBattleOnlyEffects();
		//get characters still in the battle 
		List<PlayerCharacter> aliveChars = new ArrayList<PlayerCharacter>();
		for(PlayerCharacter c : partyList)
			if(c.isInBattle()) aliveChars.add(c);
		
		int avgLevel, levelTotal = 0;

		for(PlayerCharacter c : partyList)
			if(c.getAction() == Action.Item)
				c.unuseItem();
		
		for(PlayerCharacter c : aliveChars)
		{
			c.setFace(faces.Victory);
			levelTotal += c.getLevel();
		}
		avgLevel = levelTotal / aliveChars.size();
		
		addMsgToInfobar("You are victorious!");

		
		int gold = 0;
		int exp = 0;
		List<String> itemDrops = new ArrayList<String>();
		for(Enemy e : encounter.Enemies())
		{
			if (e.getEscaped()) continue;
			gold += e.getGold();
			exp += e.getExp(avgLevel); 
			String item = e.getDrop();
			if(item != null)
				itemDrops.add(item);
			
		}
		
		java.util.Collections.sort(itemDrops);
		
		Global.party.addGold(gold);
		
		addMsgToInfobar("Obtained " + gold + "G!");
		
		
		if (aliveChars.size() > 0) //not sure how mutual destruction would work, but might as well add it.
		{
			exp /= aliveChars.size(); //distribute experience to the whole party.  GG grinding.
			addMsgToInfobar("Earned " + exp + " experience!");
			
			String newAbilities = "";
			
			for(PlayerCharacter c : aliveChars)
			{
				int leftover = c.awardExperience(exp);
				while(leftover > 0)
				{
					addMsgToInfobar(c.getDisplayName() + " grew to level " + c.getLevel() + "!");
					
					do
					{
						newAbilities = c.checkForAbilities();
						if(newAbilities != "")
							addMsgToInfobar(c.getDisplayName() + " learned " + newAbilities + "!");
						
					}while(newAbilities != "");
					
					leftover = c.awardExperience(leftover);
				}				
			}

		}
		
		String prevItem = null;
		int winCount= 0;
		itemDrops.add(null);
		for (String item : itemDrops)
		{
			if (prevItem != item && prevItem != null)
			{
				if (winCount == 1)
				{
					Global.party.addItem(prevItem, 1);	
					addMsgToInfobar("Found a " + Global.items.get(prevItem).getDisplayName() + "!");
				}
				else
				{
					Global.party.addItem(prevItem, winCount);	
					addMsgToInfobar("Found " + Global.items.get(prevItem).getDisplayName() + "x" + winCount + "!");					
				}
				winCount = 0;
			}
			prevItem = item;
			++winCount;
		}
	}
	
	public Outcome getOutcome(){ return outcome;}
	
	private void initEscaped()
	{
		setInfoBarText("You have escaped!");	
		outcome = Outcome.Escape;
	}

	private void initDefeat()
	{
		setInfoBarText(txtDefeat);
		outcome = Outcome.Defeat;
		
		

	}
	private boolean battleOver = false;
	private void triggerEndBattle()
	{
		battleOver = true;
		Global.screenFader.setFadeColor(255, 0, 0, 0);
		Global.screenFader.fadeOut(2.0f);
		musicListener.onBattleEnd();
		clearBattleOnlyEffects();
	}
	
	//act state functions
	private void initActState()
	{	
		
		battleEvents.clear();
		
		//add enemy actions to event queue
		for(Enemy e : encounter.Enemies())
			if(e.isInBattle())
			{
				getEnemyBattleAction(e);
				addBattleEvent(e, e.getTargets());
			}

		for(PlayerCharacter c : partyList)
			if(c.isInBattle())
				addBattleEvent(c, c.getTargets());
		
		battleEvents = BattleCalc.genMoveOrder(battleEvents);
		
		for(BattleEvent be : battleEvents)
			be.init();		
		
		nextActor(true);
	}
	private void updateActStatus()
	{
		if(nextActor)
		{
			if(selCharClosed)
				nextActor(false);
		}
		else
		{
			BattleEvent currentEvent = battleEvents.get(0);
			PlayerCharacter actor = currentEvent.getSource();
			
			
			if(actor.isEnemy() || 
			   selCharOpened || 
			   !currentEvent.advances())
			{
				currentEvent.update(this);
				//check if we've been interrupted mid-event.
				if(currentEvent == battleEvents.get(0) && currentEvent.isDone())
				{
					nextActorInit();
				}
			}					
		}			
	}
	public void interruptEvent()
	{
		battleEvents.get(0).interrupt();
	}
	public void forceAddAbilityEvent(PlayerCharacter source, List<PlayerCharacter> targets, Ability ability)
	{
		nextActorInit();
		battleEvents.add(0,new BattleEvent(Action.Ability,ability, source, targets, markers, ActionType.Special));
		battleEvents.get(0).init();
		battleEvents.get(0).dontRunStatusEffects();
		battleEvents.get(0).setSpecial();
		battleEvents.add(0, null); //add event to be deleted in place of the interrupted event.		

	}
	public void forceNextAbilityEvent(PlayerCharacter source, List<PlayerCharacter> targets, Ability ability)
	{
		battleEvents.add(1,new BattleEvent(Action.Ability,ability, source, targets, markers));
		battleEvents.get(1).init();
		battleEvents.get(1).setSpecial();
		battleEvents.get(1).dontRunStatusEffects();
	}
	public BattleEvent currentBattleEvent()
	{
		return battleEvents.get(0);
	}
	public PlayerCharacter getCurrentActor()
	{
		return battleEvents.get(0).getSource();
	}
	public boolean getPlayerHasGone(PlayerCharacter c)
	{
		for (int i = 1; i < battleEvents.size(); ++i)
		{
			BattleEvent b  = battleEvents.get(i);
			if (b.getSource() == c  && !b.isSpecial()) return false;
		}
		return true;
	}
	private static class MoveReselecter implements PlayerBattleActionSelect
	{

		private enum SetType
		{
			Unset, Skipped, Attacking, Ability
		}
		SetType setType;
		BattleEvent replaceEvent;
		int eventNumber;
		Battle battle;
		
		MoveReselecter(BattleEvent replaceEvent, int eventNumber, Battle battle)
		{
			setType = SetType.Unset;
			this.replaceEvent = replaceEvent;
			this.eventNumber = eventNumber;
			this.battle = battle;
		}
		
		@Override
		public void skipPlayerInput() {
			setType = SetType.Skipped;
			replaceEvent.interrupt();
			battle.battleEvents.remove(replaceEvent);
		}

		@Override
		public PlayerCharacter getPlayer() {
			return replaceEvent.getSource();
		}

		@Override
		public void setUseAbility(Ability ability,
				List<PlayerCharacter> targets) {
			battle.battleEvents.add(eventNumber, new BattleEvent(Action.Ability, ability, replaceEvent.getSource(), targets,  battle.markers));
			skipPlayerInput();		
			setType = SetType.Ability;
		}

		@Override
		public void setAttack(PlayerCharacter target) {
			List<PlayerCharacter> targets = new ArrayList<PlayerCharacter>();
			targets.add(target);
			battle.battleEvents.add(eventNumber, new BattleEvent(Action.Attack, null, replaceEvent.getSource(), targets,  battle.markers));
			skipPlayerInput();		
			setType = SetType.Attacking;
		}

		@Override
		public boolean isSkipped() {
			return setType == SetType.Skipped;
		}

		@Override
		public boolean isAbilitySet() {
			return setType == SetType.Ability;
		}

		@Override
		public boolean isAttackSet() {
			return setType == SetType.Attacking;
		}
		
	}
	public PlayerBattleActionSelect resetPlayerAction(PlayerCharacter c)
	{
		int argNum = 0;
		for (BattleEvent b : battleEvents)
		{ 
			if (argNum == 0)
			{
				++argNum;
				continue;
			}
			
			if (b.getSource() == c  && !b.isSpecial()) return new MoveReselecter(b, argNum, this);
			++argNum;
		}
			 
		return null;		
	}
	private void nextActorInit()
	{
		nextActor = true;
		displayNamePanel.hide();
		//battleEvents.get(0).getSource().acting = false;
		PlayerCharacter actor = battleEvents.get(0).getSource();			
		if(!actor.isEnemy() && (selCharOpened || battleEvents.get(0).advances()))
		{
			recedeChar();
			actor.setFace(faces.Idle);
			actor.setImageIndex(0);
		}
	}
	public boolean isBattleOver()
	{
		return isVictory() || isDefeated() || isEscaped();
	}
	public boolean graphicsCleared()
	{
		return Global.noRunningAnims() && markers.isEmpty() && !graphicalBattleActionRunner.hasRemaining();
	}
	private void applyBattleOver()
	{
		battleEnding = true;
		if (!graphicsCleared()) return;
		
		clearItemUses();
		
		if(isVictory())
		{
			stateMachine.setState(getVictoryState());
		}			
		else if(isDefeated())
		{
			stateMachine.setState(getDefeatState());
		}
		else if (isEscaped())
		{
			stateMachine.setState(getEscapedState());
		}	
		
		battleEnding = false;
	}
	private void clearItemUses()
	{
		for (Item i : Global.party.getInventory())
		{
			i.clearUseCount();
		}
	}
	private void startPlayerChoice()
	{
		for(PlayerCharacter c : partyList)
			c.setFace(faces.Idle);
		
		selectFirstChar();
		if (!getCharacterBattleAction()) nextChar = true;	
	}
	private void nextActor(boolean firstActor)
	{	
		targets.clear();
		
		if (isBattleOver())
		{
			applyBattleOver();
		}
		else
		{
			nextActor = false;
			if(!firstActor)
			{
				battleEvents.remove(0);
			}
			
			
			if(battleEvents.size() == 0)
			{
				selectFirstChar();
				startTurn.trigger();
				clearItemUses();
				startPlayerChoice();
				return;
			}
			else
			{
				BattleEvent currentEvent = battleEvents.get(0);
				PlayerCharacter actor = battleEvents.get(0).getSource();
				List<PlayerCharacter> targets = currentEvent.getTargets();
				Action action = currentEvent.getAction();
				Ability ability = currentEvent.getAbility();
				
				//set frame text
				switch(action)
				{case Attack:setInfoBarText(actor.getDisplayName()+" attacks!");break;
				case Item:setInfoBarText(actor.getDisplayName()+" uses "+actor.getItemToUse().getDisplayName()+"!");break;
				case Ability:
					if (!ability.getDisplayName().equals(""))
					{
						setInfoBarText(actor.getDisplayName()+" casts "+ability.getDisplayName()+"!");	
					}
					break;
				case CombatAction:setInfoBarText(actor.getDisplayName()+actor.getCombatActionText());break;
				default: break;}
				
				if(!actor.isInBattle())
					nextActorInit();
				else 
				{					
					//reset targets
					if (!currentEvent.running()) //we're returning to this event, don't reset the targets!
					{   
						if (Action.Ability == action && ability != null)
						{
							currentEvent.setTargets(getTargetable(actor, targets, ability.TargetType()));
						}
						else if (Action.Item == action)
						{
							currentEvent.setTargets(getTargetable(actor, targets, actor.getItemToUse().getTargetType()));
						}
						else
						{
							currentEvent.setTargets(getTargetable(actor, targets, TargetTypes.Single));
						}
					}
					
					if(action == Action.CombatAction)
					{
						showDisplayName(actor.combatActionName);
					}						
					else if(action == Action.Ability)
					{
						if (!ability.getDisplayName().equals(""))
						{
							showDisplayName(ability.getDisplayName());	
						}
						actor.useAbility(ability);
					}						
					else if(action == Action.Item)
					{
						showDisplayName(actor.getItemToUse().getDisplayName());
					}
					else if (action == Action.Run)
					{
						showDisplayName("Run");
					}
					
					if(!actor.isEnemy())
					{
						currentChar = actor;
						if (currentEvent.advances())
							advanceChar();
					}
					else
						setTarget(actor);
				}
			}
		}
	}
	public List<PlayerCharacter> getTargetable(PlayerCharacter actor, List<PlayerCharacter> targets, TargetTypes targetType)
	{
		List<PlayerCharacter> validTargets = new ArrayList<PlayerCharacter>();
		//fill alive targets
		for(PlayerCharacter c : targets)
		{
			if (targetType == TargetTypes.SingleDeadAlly)
			{
				if (c.getEscaped() == false && c.isDead()) validTargets.add(c);
			}
			else
			{
				if(c.isInBattle()) validTargets.add(c);	
			}
		}
		
		//reset to random characters if targeting a dead guy
		if(validTargets.isEmpty() && !targets.isEmpty())
		{
			List<PlayerCharacter> aliveChars = new ArrayList<PlayerCharacter>();
			List<Enemy> aliveEnemies = new ArrayList<Enemy>();
			for(PlayerCharacter c : partyList)if(c.isInBattle())aliveChars.add(c);
			for(Enemy e : encounter.Enemies())if(e.isInBattle())aliveEnemies.add(e);
			
			if(actor.isEnemy())
				if(targets.get(0).isEnemy())
					//select new enemy
					validTargets.add(aliveEnemies.get(Global.rand.nextInt(aliveEnemies.size())));
				else
					//select new ally
					validTargets.add(aliveChars.get(Global.rand.nextInt(aliveChars.size())));
			else
				if(targets.get(0).isEnemy())
					//select new enemy
					validTargets.add(aliveEnemies.get(Global.rand.nextInt(aliveEnemies.size())));
				else
					//select new ally... for now, needs to check if dead are targetable.  generalized pass-fail filter later?
					validTargets.add(aliveChars.get(Global.rand.nextInt(aliveChars.size())));

		}
		
		return validTargets;
	}
	private boolean isDefeated()
	{
		for(PlayerCharacter c : partyList)
			if(!c.isDead())
				return false;
		
		return true;
	}
	public boolean isEscaped()
	{
		for(PlayerCharacter c : partyList)
			if(c.isInBattle())
				return false;
		
		return !isDefeated();	  
	}
	private boolean isVictory()
	{
		for(Enemy e : encounter.Enemies())
			if(e.isInBattle())
				return false;
		
		return true;
	}
	private CombatActionBuilder getCombatActionBuilder()
	{
		return new CombatActionBuilder()
		{
			public ListBox getMenu() {
				return mainMenu;
			}

			@Override
			public PlayerCharacter getUser() {
				return currentChar;
			}

			@Override
			public Battle getBattle() {
				return Global.battle;  //hackish for now... "this" is wrong to use here.
			}

			@Override
			public BattleStateMachine getStateMachine() {
				return stateMachine;
			}

			@Override
			public MenuPanel getMPWindow() {
				return mpWindow;
			}
			
		};
	}
	private void handleMenuOption(String opt)
	{
		if(opt.equals("atk"))
		{
			currentChar.setBattleAction(Action.Attack);
			currentChar.setFace(faces.Ready);
			stateMachine.setState(getTargetState(TargetTypes.Single));
		}
		else if(opt.equals("itm"))
		{
			stateMachine.setState(getSelectItemState());
		}
		else if(opt.equals("act"))
		{
			currentChar.setBattleAction(Action.CombatAction);
			currentChar.getCombatAction().onSelected(getCombatActionBuilder());
		}
		else if(opt.equals("grd"))
		{
			currentChar.setFace(faces.Ready);
			currentChar.setBattleAction(Action.Guard);
			nextCharacter();
		}
		else if(opt.equals("ab"))
		{
			stateMachine.setState(getSelectAbilityState());
		}
		else if(opt.equals("run"))
		{
			currentChar.setBattleAction(Action.Run);
			nextCharacter();
		}
	}
	private void advanceChar()
	{
		selCharClosed = false;
		selCharOpening = true;
	}
	private void recedeChar()
	{
		selCharOpened = false;
		selCharOpening = false;
	}
	private void handleCharAdvancing()
	{
		if(!selCharOpened && !selCharClosed)
		{
			if(selCharOpening)
			{
				selCharX += charMoveSpeed;
				if(selCharX >= advanceDistance)
				{
					selCharX = advanceDistance;
					selCharOpened = true;
				}
					
			}
			else
			{
				selCharX -= charMoveSpeed;
				if(selCharX <= 0)
				{
					selCharX = 0;
					selCharClosed = true;
				}
					
			}
		}
		
		
	}
	private void selectFirstChar()
	{
		currentChar = null;
		currentCharIndex = partyList.size();
		
		//determine first nondead character
		for(int i = 0; i < partyList.size(); ++i)
			if(partyList.get(i).isInBattle())
			{
				currentCharIndex = i;
				currentChar = partyList.get(currentCharIndex);
				break;
			}
	}
	
	public static interface PlayerBattleActionSelect
	{
		void skipPlayerInput();
		PlayerCharacter getPlayer();
		void setUseAbility(Ability ability, List<PlayerCharacter> targets);
		void setAttack(PlayerCharacter target);
		boolean isSkipped();
		boolean isAbilitySet();
		boolean isAttackSet();
	}
	
	private PlayerBattleActionSelect actionSetter; 
	public Event getOnActionSelectEvent()
	{
		return onSelectStart;
	}
	public Event getOnDamageDealt()
	{
		return damageDealt;
	}	
	public PlayerBattleActionSelect getActionSetter()
	{
		return actionSetter;
	}
	
	private class ActionSelector implements PlayerBattleActionSelect
	{
		public boolean inputSkipped = false;
		public Ability forceAbility = null;
		public boolean forceAttack = false;			
		public List<PlayerCharacter> targets;
		PlayerCharacter selectedChar;
		ActionSelector(PlayerCharacter selectedChar)
		{
			this.selectedChar = selectedChar;
		}
		@Override
		public void skipPlayerInput() {
			inputSkipped = true;
		}

		@Override
		public PlayerCharacter getPlayer() {
			return selectedChar;
		}

		@Override
		public void setUseAbility(Ability ability, List<PlayerCharacter> targets) {
			forceAbility = ability;
			this.targets = targets;
		}

		@Override
		public void setAttack(PlayerCharacter target) {
			forceAttack = true;
			targets = new ArrayList<PlayerCharacter>();
			targets.add(target);
		}
		@Override
		public boolean isSkipped() {
			return inputSkipped;
		}
		@Override
		public boolean isAbilitySet() {
			return forceAbility != null;
		}
		@Override
		public boolean isAttackSet() {
			return forceAttack == true;
		}			
	};

	private boolean processActionSelector(PlayerCharacter c)
	{
		ActionSelector selector = new ActionSelector(c);
		actionSetter = selector;
		
		onSelectStart.trigger();
		actionSetter = null;
		
		boolean turnOver = false;
		
		if (selector.inputSkipped == true)
		{
			c.setBattleAction(Action.Skipped);
			turnOver = true;
		}
		else if (selector.forceAbility != null)
		{
			c.setBattleAction(Action.Ability);
			c.setAbilityToUse(selector.forceAbility);
			c.setTargets(selector.targets);
			turnOver = true;
		}
		else if (selector.forceAttack)
		{
			c.setBattleAction(Action.Attack);
			c.setTargets(selector.targets);
			turnOver = true;
		}
		return turnOver;
	}
	private void getEnemyBattleAction(Enemy e)
	{
		
		boolean turnOver = processActionSelector(e);
		if (!turnOver)
		{
			e.genBattleEvent();
		}
		
	}
	private boolean getCharacterBattleAction()
	{
		if (currentChar == null) return false;
		boolean turnOver = processActionSelector(currentChar);
		if (!turnOver)
		{
			stateMachine.setState(getSelectState());	
			return true;
		}
		else
		{
			return false;
		}
	}
	private void addBattleEvent(PlayerCharacter source, List<PlayerCharacter> targets)
	{
		battleEvents.add(new BattleEvent(source.getAction(), source.getAbilityToUse(), source, targets, markers));
	}
	
	public void setInfoBarText(String str)
	{
		messageQueue.clear();
		addMsgToInfobar(str);
	}
	
	public void addMsgToInfobar(String str)
	{
		messageQueue.add(str);
		infoPanel.open();
		updatePanels();
	}
	
	public void showMessage(String msg)
	{
		msgBox.addBasicMessage(msg);
		msgBox.open();
	}
	
	private void nextCharacter()
	{
		//mainMenu.close();		
		recedeChar();
		nextChar = true;
	}
	private boolean previousCharacterExists()
	{
		int idx = currentCharIndex;
		PlayerCharacter character = currentChar;
	
		for (;;)
		{
			if(idx == 0)
			{	
				return false;
			}
			character = partyList.get(--idx);
			
			if (!character.isInBattle()) continue;
		
			PlayerCharacter temp = currentChar;
			currentChar = character;
			if (getCharacterBattleAction())
			{
				currentChar = temp;
				return true;
			}
			currentChar = temp;
		}
	}
	private void previousCharacter()
	{
		if (!previousCharacterExists())
		{
			return;
		}
		
		if(selCharOpened)
		{		
			recedeChar();
			prevChar = true;			
		}
	}
	private void handleNextPrev()
	{
		if((nextChar || prevChar) && selCharClosed)
		{
			if(prevChar)
			{
				if (!previousCharacterExists())
				{
					nextChar = prevChar = false;	
					return;
				}
				int startIndex = currentCharIndex;
				nextChar = prevChar = false;
				if(currentChar.getAction() == Action.Item)
					currentChar.unuseItem();
			
				for (;;)
				{
					if(currentCharIndex == 0)
					{
						//This assumes no items have been used!!!
						currentCharIndex = startIndex;
						nextChar = prevChar = false;	
						return;
					}
					--currentCharIndex;
					
					currentChar = partyList.get(currentCharIndex);
					
					if (!currentChar.isInBattle()) continue;
					
					if(currentChar.getAction() == Action.Item)
						currentChar.unuseItem();
				
					if (getCharacterBattleAction()) break;
				}
			}
			else 
			{
				nextChar = prevChar = false;
				for (;;)
				{
					//loop until nondead party member
					do ++currentCharIndex;
					while(currentCharIndex < partyList.size() && !partyList.get(currentCharIndex).isInBattle());
					
					if(currentCharIndex < partyList.size())
					{					
						currentChar = partyList.get(currentCharIndex);
						if (getCharacterBattleAction()) break;
					}
					else
					{
						stateMachine.setState(getActState());
						break;
					}
				}
			}			
		}
	}
	
	private void drawActors()
	{
		Global.screenFilter.save();
		Global.screenFilter.clear();
		for(PlayerCharacter c : partyList)
		{
			c.renderShadow();
		}
		for(Enemy e : encounter.Enemies())
		{
			e.renderShadow();
		}
		
		Global.screenFilter.restore();
		for(PlayerCharacter c : partyList)
		{
			c.battleRender();
		}

		for(Enemy e : encounter.Enemies())
			e.battleRender();
		
		Global.screenFilter.save();
		Global.screenFilter.clear();
		for(PlayerCharacter c : partyList)
		{
			characterPanes[c.Index()].render();
		}
		Global.screenFilter.restore();	
		
		
			
	}
	private void drawPanels()
	{	
		if(!mainMenu.Closed())
			mainMenu.render();
				
		stateMachine.getState().drawPanels();
		
		displayNamePanel.render();
		
		if(msgBox.Closed() && !infoPanel.Closed())		
			infoPanel.render();
		
		if(!charStatusPanel.Closed())
			charStatusPanel.render();
		
		if(charStatusPanel.Opened())
			backButton.render();
	}
	private void drawSelect()
	{
		for(PlayerCharacter t : targets)
		{
			if (t.getEscaped()) continue;
			
			Global.renderer.drawRect(t.getRect(), selectPaint, true);
			
			//draw enemy names
			if(t.isEnemy())
			{
				Rect eRect = t.getRect();//HAHA AINT I FUNNY				
				boolean drawTop = Global.vpToScreenY(eRect.top - (int)(enemyBattleText.getTextSize()/2)-4)> 0;
				float drawY = drawTop ? 
						eRect.top - enemyBattleText.getTextSize()/2 - 4 : 
						eRect.bottom + enemyBattleText.getTextSize()/2-4;					
				Global.renderer.drawText(t.getDisplayName(), eRect.centerX(), drawY, enemyBattleText);
				
			}
		}
			

	}
	private void drawDamageMarkers()
	{
		for(DamageMarker dm : markers)
			dm.render();
	}
	
	public BattleEventBuilder makeGraphicalBattleEventBuilder()
	{
		return new BattleEventBuilder(){
			 public List<PlayerCharacter> getTargets()
			 {
				 return null;
			 }
			 public PlayerCharacter getSource()
			 {
				 return null;
			 }
			 
			 public BattleAction getLast()
			 {
				 return graphicalBattleActionRunner.getLast();
			 }
			 
			 public void addEventObject(BattleAction eventObj)
			 {
				 graphicalBattleActionRunner.addAction(eventObj);
			 }
			 public void addMarker(DamageMarker marker)
			 {
				 markers.add(marker);
			 }
			 public int getCurrentBattleFrame()
			 {
				 return currentFrame;
			 }
			};
	}
	
	public void update()
	{
		updatePanels();
		updateCharacterPanes();
		charStatusPanel.update();
		msgBox.update();
		updateCurrentFrame();
		graphicalBattleActionRunner.run(makeGraphicalBattleEventBuilder()); 
		
		if(msgBox.Closed())
		{
			
			
			handleCharAdvancing();
			if (!isBattleOver())
			{
				updateCharacterPositions();	
			}
			updateDamageMarkers();
			
			handleNextPrev();
			
			
			stateMachine.getState().update();
			
		}
		
	}	
	public void render()
	{
		Global.map.getBackdrop().render();
		
		drawActors();

		
		Global.renderAnimations();
		
		Global.screenFilter.save();
		Global.screenFilter.clear();
		
		drawDamageMarkers();
		
		if (Global.screenFader.isDone())
		{
			Global.screenFader.render();			
		}

		

		drawPanels();
		drawSelect();
		
		if(!msgBox.Closed())
			msgBox.render();
		
		
		
		if (!Global.screenFader.isDone())
		{
			Global.screenFader.render();			
		}		
		
		
		Global.screenFilter.restore();
	}
	
	public void backButtonPressed()
	{
		if(!battleEnding)
			stateMachine.getState().backButtonPressed();
	}
	public void onLongPress(int x, int y)
	{
		if(msgBox.Closed() && !battleEnding)
			stateMachine.getState().onLongPress(x, y);
	}
	public void touchActionUp(int x, int y)
	{
		if(!msgBox.Closed())
			msgBox.touchActionUp(x, y);
		else 
		{	
			if(!battleEnding)
				stateMachine.getState().touchActionUp(x, y);
		}
			
		
	}
	public void touchActionDown(int x, int y)
	{
		if(!msgBox.Closed())
			msgBox.touchActionDown(x, y);

		if(!battleEnding)
			stateMachine.getState().touchActionDown(x, y);
	
	}
	public void touchActionMove(int x, int y)
	{
		if(!msgBox.Closed())
			msgBox.touchActionMove(x, y);
		else
			if(!battleEnding)
				stateMachine.getState().touchActionMove(x, y);

		
	}
	private void getTouchTargets(int x, int y, TargetTypes targetType)
	{
		targets.clear();		
		switch(targetType)
		{
		case Single:
			if(enemyArea.contains(x, y))
				targetEnemy(x, y);
			else
				targetAlly(x, y);
			break;
		case SingleEnemy:
			if(enemyArea.contains(x, y))
				targetEnemy(x, y);
			break;
		case SingleAlly:
			targetAlly(x, y);
			break;
		case SingleDeadAlly:
			targetDeadAlly(x, y);
			break;			
		case Self:
			targets.add(currentChar);
			break;
		case AllAllies:
			for(PlayerCharacter c : partyList)
				targets.add(c);
			break;
		case AllEnemies:
			for(Enemy e : encounter.Enemies())
				if(e.isInBattle())
					targets.add(e);
			break;
		case Everybody:
			for(Enemy e : encounter.Enemies())
				if(e.isInBattle())
					targets.add(e);
			for(PlayerCharacter c : partyList)
				targets.add(c);
			break;
			
		}		

	}
	//add closest enemy; assumes tapped in enemy area
	private void targetEnemy(int x, int y)
	{
		int lowestDist = Math.abs(enemyArea.top - enemyArea.bottom) + Math.abs(enemyArea.left - enemyArea.right);
		Enemy closest = null;
		for(Enemy e : encounter.Enemies())
		{
			if(!e.isDead())
			{
				Point pos = Global.vpToScreen(e.getPosition());
				int dist = Math.abs(pos.x - x) + Math.abs(pos.y - y);
				if(dist < lowestDist) {lowestDist = dist;closest = e;}
			}			
		}
		if(closest != null)		
			targets.add(closest);
	}	
	//target closest ally, assumes tapping within char rect
	private void targetAlly(int x, int y)
	{
		for(PlayerCharacter c : partyList)
			if(c.getRect().contains(x, y))
			{
				targets.add(c);
				break;
			}
	}
	private void targetDeadAlly(int x, int y)
	{
		for(PlayerCharacter c : partyList)
			if(c.getRect().contains(x, y)  && c.isDead()  && !c.getEscaped())
			{
				targets.add(c);
				break;
			}
	}	
	private void setTarget(PlayerCharacter c)
	{
		targets.clear();
		targets.add(c);
	}
	
	public void cancelToState(BattleState prevState)
	{
		if(currentChar.getAction() == Action.Item)
			currentChar.unuseItem();
		stateMachine.resetToState(prevState);				
	}
	
	public List<PlayerCharacter> getParty()
	{
		return partyList;
	}
	
	public Encounter getEncounter()
	{
		return encounter;
	}
	public void setBattleEndHandler(BattleEndHandler battleEndHandler)
	{
		this.battleEndHandler = battleEndHandler;
	}
	public Point getPlayerDefaultPosition(PlayerCharacter p)
	{
		if (p == currentChar)
		{
			return new Point(partyPos.x - selCharX + (charXSpacing * p.Index()), 
					        partyPos.y + (charYSpacing * p.Index()));
		}
		else
		{
			return new Point(partyPos.x + (charXSpacing * p.Index()), 
			        		 partyPos.y + (charYSpacing * p.Index()));			
		}
	}
	
	public enum Team
	{
		Player,
		Enemy
	}
	public enum Outcome
	{
		Victory,
		Defeat,
		Escape
	}
	
	
	public static List<PlayerCharacter> getRandomTargets(TargetTypes targetType, PlayerCharacter character)
	{
		Team team;
		if (character.isEnemy()) team = Team.Enemy;
		else team = Team.Player;
		
		return getRandomTargets(targetType, character, team);
	}
	public static List<PlayerCharacter> getRandomTargets(TargetTypes targetType, PlayerCharacter character, Team team)
	{
		List<PlayerCharacter> targets = new ArrayList<PlayerCharacter>();
		List<PlayerCharacter> us, them;
		
		if (team == Team.Enemy)
		{
			us = new ArrayList<PlayerCharacter>();			
			them = new ArrayList<PlayerCharacter>();
			for (Enemy e : Global.battle.getEncounter().Enemies())
			{
				if (e.isInBattle())
				{
					us.add(e);
				}	
			}
			for (PlayerCharacter c : Global.battle.getParty())
			{
				if (c.isInBattle())
				{
					them.add(c);
				}	
			}			
		}
		else
		{
			us = new ArrayList<PlayerCharacter>();
			them = new ArrayList<PlayerCharacter>();
			for (PlayerCharacter c : Global.battle.getParty())
			{
				if (c.isInBattle())
				{
					us.add(c);
				}	
			}						
			for (Enemy e : Global.battle.getEncounter().Enemies()) 
			{
				if (e.isInBattle())
				{
					them.add(e);
				}	
			}				
		}
		
		List<PlayerCharacter> everybody = new ArrayList<PlayerCharacter>();
		
		for(PlayerCharacter c : us) everybody.add(c);
		for(PlayerCharacter e : them) everybody.add(e);
		
		switch(targetType)
		{
			case Single:
				targets.add(everybody.get(Global.rand.nextInt(everybody.size())));
				break;
			case SingleEnemy:
				targets.add(them.get(Global.rand.nextInt(them.size())));
				break;				
			case SingleAlly:			
				targets.add(us.get(Global.rand.nextInt(us.size())));
				break;
			case SingleDeadAlly:			
				//TODO: Fix!
				targets.add(us.get(Global.rand.nextInt(us.size())));
				break;				
			case AllAllies:
				for(PlayerCharacter p : us)targets.add(p);
				break;
			case AllEnemies:
				for(PlayerCharacter p : them)targets.add(p);
				break;				
			case Everybody:
				for(PlayerCharacter p : everybody)targets.add(p);
				break;
			case Self:
				targets.add(character);
				break;			
		}
		return targets;
	}
	
	 public List<DamageMarker> getMarkers()
	 {
		 return markers;
	 }
}
