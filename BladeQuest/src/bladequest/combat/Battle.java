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
import bladequest.UI.MenuPanel;
import bladequest.UI.MenuPanel.Anchors;
import bladequest.graphics.BattleSprite.faces;
import bladequest.statuseffects.StatusEffect;
import bladequest.world.Ability;
import bladequest.world.PlayerCharacter;
import bladequest.world.PlayerCharacter.Action;
import bladequest.world.Encounter;
import bladequest.world.Enemy;
import bladequest.world.Global;
import bladequest.world.Item;
import bladequest.world.States;
import bladequest.world.TargetTypes;

public class Battle 
{	
	private final int frameMaxHeight = 96;
	private final int frameMinHeight = 32;
	private final int partyFrameBuffer = 32;
	private final int advanceDistance = 32;
	private final int charMoveSpeed = 7;
	private final Point partyPos = new Point(Global.vpWidth - 128, 0);
	private final int statFrameBuffer = 16;
	private final int charYSpacing = 64;
	private final int mpWindowHeight = 32;
	private final int mpWindowWidth = 128;	
	
	private final String txtStart = "Tap screen to start!";
	private final String txtTargetSingle = "Select target...";
	private final String txtTargetSingleAlly = "Select ally...";
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
	
	private PlayerCharacter currentChar;
	private int currentCharIndex;
	private TargetTypes targetType;
	private List<PlayerCharacter> targets;
	private List<BattleEvent> battleEvents;
	private List<DamageMarker> markers;
	private List<String> messageQueue;
	
	private BattleStates state;
	private Encounter encounter;

	private List<PlayerCharacter> partyList;
	
	private Rect enemyArea;
	
	//gfx
	private Paint selectPaint;
	private Paint statsText, nameText, battleText, grayBattleText, enemyBattleText,nameDisplayPaint;
	
	//menu panels
	private MenuPanel startBar, infoPanel, displayNamePanel, mpWindow;
	private MenuPanel characterPanes[];
	private ListBox mainMenu;
	
	public Battle()
	{
		enemyArea = Global.vpToScreen(new Rect(0,0,partyPos.x-partyFrameBuffer, Global.vpHeight-frameMinHeight));
		targets = new ArrayList<PlayerCharacter>();
		battleEvents = new ArrayList<BattleEvent>();
		markers = new ArrayList<DamageMarker>();
		messageQueue = new ArrayList<String>();
	}
	
	public void startBattle(String encounter)
	{
		this.state = BattleStates.START;
		this.encounter = new Encounter(Global.encounters.get(encounter));
		
		partyList = Global.party.getPartyList(false);
		
		selectFirstChar();
		
		//all characters are dead (this should never happen)
		if(currentChar == null)
			changeState(BattleStates.DEFEAT);
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
				}
			
			//init
			recedeChar();	
			
			//GFX
			Global.map.getBackdrop().load();
			buildPaints();
			
			//UI
			initUI();		
			update();
		}		
	}
	
	private void initUI()
	{
		buildCharacterPanes();
		buildPanels();
		buildMainMenu();
		
	}
	private void buildCharacterPanes()
	{
		characterPanes = new MenuPanel[4];
		for(int i = 0; i < 4; ++i)
		{
			characterPanes[i] = new MenuPanel(
					partyPos.x + 64, 
					charYSpacing*i + statFrameBuffer,
					Global.vpWidth - (partyPos.x + 64),
					charYSpacing - statFrameBuffer);
			characterPanes[i].thickFrame = false;
		}
	}
	private void buildPanels()
	{		
		startBar = new MenuPanel(0, Global.vpHeight, partyPos.x - partyFrameBuffer, frameMinHeight);
		startBar.addTextBox(txtStart, 5, frameMinHeight/2, battleText);
		startBar.anchor = Anchors.BottomLeft;
		startBar.thickFrame = false;
		
		infoPanel = new MenuPanel(0, Global.vpHeight, Global.vpWidth, frameMinHeight);
		infoPanel.addTextBox("", 5, frameMinHeight/2, battleText);
		infoPanel.anchor = Anchors.BottomLeft;
		infoPanel.thickFrame = false;
		infoPanel.hide();
		
		displayNamePanel = new MenuPanel();
		displayNamePanel.thickFrame = false;
		displayNamePanel.setInset(-10, -7);
		displayNamePanel.addTextBox("", 0, 0, nameDisplayPaint);	
		displayNamePanel.hide(); 
		
		mpWindow = new MenuPanel(0, Global.vpHeight - frameMaxHeight-mpWindowHeight, mpWindowWidth, mpWindowHeight);
		mpWindow.thickFrame = false;
		mpWindow.addTextBox("", 5, mpWindowHeight/2, battleText);
		//mpWindow.hide();
	}
	private void buildMainMenu()
	{
		mainMenu = new ListBox(0, Global.vpHeight, partyPos.x - partyFrameBuffer, frameMinHeight, 3, 2, battleText);
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
			
			characterPanes[i].clear();			
			
			characterPanes[i].addTextBox(c.getDisplayName(), 5, (int)((charYSpacing - statFrameBuffer)*0.25f), nameText);
			characterPanes[i].addTextBox("HP:" + c.getHP(), 7, (int)((charYSpacing - statFrameBuffer)*0.50f), statsText);
			characterPanes[i].addTextBox("MP:" + c.getMP(), 7, (int)((charYSpacing - statFrameBuffer)*0.75f), statsText);
			
			float iconScale = 1.5f;
			int j = 0, d = (int)(Global.iconSize*iconScale + 2);
			for(StatusEffect se : c.getStatusEffects())
				if(j < 4 && se.icon().length() > 0)
					characterPanes[i].addPicBox(Global.createIcon(se.icon(),(d/2) +  d*j++,-(d/2),iconScale));
			
			
			characterPanes[i].update();
			
		}
	}
	private void updatePanels()
	{
		startBar.update();		
		displayNamePanel.update();
		mpWindow.update();
		mainMenu.update();
		
		if(messageQueue.size() == 0 && infoPanel.isShown())
			infoPanel.hide();
		else
			if(messageQueue.size() > 0)
				infoPanel.getTextAt(0).text = messageQueue.get(0);
		
		infoPanel.update();
	}
	private void updateCharacterPositions()
	{
		for(PlayerCharacter c : partyList)
			c.setPosition(partyPos.x, partyPos.y + (charYSpacing * c.Index()));
		
		if(currentChar != null)
			currentChar.setPosition(partyPos.x-selCharX, partyPos.y + (charYSpacing * currentChar.Index()));
	}
	private void updateMenuOptions(BattleStates state)
	{
		switch(state)
		{
		case SELECT:
			mainMenu.clearObjects();
			mainMenu.addItem("Attack", "atk", false);
			mainMenu.addItem("Item", "itm", false);
			mainMenu.addItem(currentChar.getActionName(), "act", false);
			mainMenu.addItem("Guard", "grd", false);
			mainMenu.addItem(currentChar.getAbilitiesName(), "ab", false);
			mainMenu.addItem("Run", "run", false);		
			mainMenu.update();
			break;
		case SELECTABILITY:
			mainMenu.clearObjects();
			for(Ability a : currentChar.getAbilities())
				mainMenu.addItem(a.getDisplayName(), a, a.MPCost() > currentChar.getMP());
			mainMenu.update();
			break;
		case SELECTITEM:
			mainMenu.clearObjects();
			for(Item i : Global.party.getInventory(true))
				mainMenu.addItem(i.getName(), i, false);
			mainMenu.update();
			break;
		default:
			break;
		}
		
		mainMenu.update();
		
	}
	public void changeStartBarText(String str){startBar.getTextAt(0).text = str;}
	private void showDisplayName(String str)
	{
		displayNamePanel.show();		
		
		Rect displayNameRect = new Rect();
		nameDisplayPaint.getTextBounds(str, 0, str.length()-1, displayNameRect);
		displayNameRect = Global.screenToVP(displayNameRect);
		
		
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
		//play victory music
		Global.musicBox.play("victory", true, -1);
		
		//get alive characters
		List<PlayerCharacter> aliveChars = new ArrayList<PlayerCharacter>();
		for(PlayerCharacter c : partyList)
			if(!c.isDead()) aliveChars.add(c);
		
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
		
		addMessage("You are victorious!");

		int gold = 0;
		int exp = 0;
		List<String> itemDrops = new ArrayList<String>();
		for(Enemy e : encounter.Enemies())
		{
			gold += e.getGold();
			exp += e.getExp(avgLevel); 
			if(e.hasItems())
			{
				String item = e.getItem(false);
				if(item != null)
					itemDrops.add(item);
			}
			
		}
		
		//pick random drop
		String wonItem = null;
		if(itemDrops.size() > 0)
			wonItem = itemDrops.get(Global.rand.nextInt(itemDrops.size()));
		
		Global.party.addGold(gold);
		
		addMessage("Obtained " + gold + "G!");
		addMessage("Earned " + exp + " experience!");

		
		String newAbilities = "";
		
		for(PlayerCharacter c : aliveChars)
		{
			int leftover = c.awardExperience(exp);
			while(leftover > 0)
			{
				addMessage(c.getDisplayName() + " grew to level " + c.getLevel() + "!");
				
				do
				{
					newAbilities = c.checkForAbilities();
					if(newAbilities != "")
						addMessage(c.getDisplayName() + " learned " + newAbilities + "!");
					
				}while(newAbilities != "");
				
				leftover = c.awardExperience(leftover);
			}				
		}
		
		if(wonItem != null)
		{
			Global.party.addItem(wonItem);	
			addMessage("Found a " + Global.items.get(wonItem).getName() + "!");
		}		
		
	}
	private void initDefeat()
	{
		changeStartBarText(txtDefeat);
		
		//clear further actions of the calling object
		if(Global.BattleStartObject != null)
			Global.BattleStartObject.clearActions();
		
		Global.party.allowMovement = true;
		Global.setPanned(0, 0);				
		Global.map.gameOverObject.execute();
	}
	private void triggerEndBattle()
	{
		Global.screenFader.setFadeColor(255, 0, 0, 0);
		Global.screenFader.fadeOut(2);
		changeStartBarText("");

	}
	
	//act state functions
	private void initActState()
	{	
		battleEvents.clear();
		
		//add enemy actions to event queue
		for(Enemy e : encounter.Enemies())
			if(!e.isDead())
				battleEvents.add(e.genBattleEvent(partyList, encounter.Enemies()));

		for(PlayerCharacter c : partyList)
			if(!c.isDead() && c.getAction() != Action.Guard)
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
			
			
			if(actor.isEnemy() || selCharOpened)
			{
				currentEvent.update(this, markers);
				if(currentEvent.isDone())
					nextActorInit();
			}					
		}			
	}
	private void nextActorInit()
	{
			nextActor = true;
			displayNamePanel.hide();
			//battleEvents.get(0).getSource().acting = false;
			PlayerCharacter actor = battleEvents.get(0).getSource();			
			if(!actor.isEnemy())
			{
				recedeChar();
				actor.setFace(faces.Idle);
				actor.setImageIndex(0);
			}
	}
	private void nextActor(boolean firstActor)
	{	
		targets.clear();
		
		if(isVictory())
		{
			if(Global.noRunningAnims() && markers.isEmpty())
				changeState(BattleStates.VICTORY);
		}			
		else if(isDefeated())
		{
			if(Global.noRunningAnims() && markers.isEmpty())
				changeState(BattleStates.DEFEAT);
		}			
		else
		{
			nextActor = false;
			if(!firstActor)
				battleEvents.remove(0);
			
			if(battleEvents.size() == 0)
			{
				selectFirstChar();
				changeState(BattleStates.START);
			}
			else
			{
				BattleEvent currentEvent = battleEvents.get(0);
				PlayerCharacter actor = battleEvents.get(0).getSource();
				List<PlayerCharacter> targets = currentEvent.getTargets();
				
				//set frame text
				switch(actor.getAction())
				{case Attack:changeStartBarText(actor.getDisplayName()+" attacks!");break;
				case Item:changeStartBarText(actor.getDisplayName()+" uses "+actor.getItemToUse().getName()+"!");break;
				case Ability:changeStartBarText(actor.getDisplayName()+" casts "+actor.getAbilityToUse().getDisplayName()+"!");break;
				case CombatAction:changeStartBarText(actor.getDisplayName()+actor.getCombatActionText());break;
				default: break;}
				
				if(actor.isDead())
					nextActorInit();
				else 
				{					
					//reset targets
					currentEvent.setTargets(getLivingTargets(actor, targets));
					
					if(actor.getAction() == Action.Ability)
					{
						showDisplayName(actor.getAbilityToUse().getDisplayName());
						actor.useAbility();
					}						
					else if(actor.getAction() == Action.Item)
						showDisplayName(actor.getItemToUse().getName());
					
					if(!actor.isEnemy())
					{
						currentChar = actor;
						advanceChar();
					}
					else
						setTarget(actor);
				}
			}
		}
	}
	public List<PlayerCharacter> getLivingTargets(PlayerCharacter actor, List<PlayerCharacter> targets)
	{
		List<PlayerCharacter> aliveTargets = new ArrayList<PlayerCharacter>();
		//fill alive targets
		for(PlayerCharacter c : targets) if(!c.isDead())aliveTargets.add(c);
		
		//reset to random characters if targeting a dead guy
		if(aliveTargets.isEmpty())
		{
			List<PlayerCharacter> aliveChars = new ArrayList<PlayerCharacter>();
			List<Enemy> aliveEnemies = new ArrayList<Enemy>();
			for(PlayerCharacter c : partyList)if(!c.isDead())aliveChars.add(c);
			for(Enemy e : encounter.Enemies())if(!e.isDead())aliveEnemies.add(e);
			
			if(actor.isEnemy())
				if(targets.get(0).isEnemy())
					//select new enemy
					aliveTargets.add(aliveEnemies.get(Global.rand.nextInt(aliveEnemies.size())));
				else
					//select new ally
					aliveTargets.add(aliveChars.get(Global.rand.nextInt(aliveChars.size())));
			else
				if(targets.get(0).isEnemy())
					//select new enemy
					aliveTargets.add(aliveEnemies.get(Global.rand.nextInt(aliveEnemies.size())));
				else
					//reselect original target
					aliveTargets.add(targets.get(0));

		}
		
		return aliveTargets;
	}
	private boolean isDefeated()
	{
		for(PlayerCharacter c : partyList)
			if(!c.isDead())
				return false;
		
		return true;
	}
	private boolean isVictory()
	{
		for(Enemy e : encounter.Enemies())
			if(!e.isDead())
				return false;
		
		return true;
	}
		
	private void changeState(BattleStates newState)
	{
		switch(newState)
		{
		case START:
			recedeChar();
			mainMenu.close();
			changeStartBarText(txtStart);
			for(PlayerCharacter c : partyList)
				c.setFace(faces.Idle);
			break;
		case SELECT:
			updateMenuOptions(newState);
			mainMenu.open();
			advanceChar();
			targets.clear();
			currentChar.setFace(faces.Idle);
			break;
		case SELECTABILITY:
		case SELECTITEM:
			updateMenuOptions(newState);
			break;
		case ACT:
			targets.clear();
			mainMenu.close();
			initActState();
			break;
		case VICTORY:
			initVictory();
			break;
		case DEFEAT:
			initDefeat();			
			break;			
		case TARGET:
			
			switch(targetType)
			{
			case AllAllies:
				changeStartBarText(txtTargetAllies);
				getTouchTargets(-1, -1);
				break;
			case AllEnemies:
				changeStartBarText(txtTargetEnemies);
				getTouchTargets(-1, -1);
				break;
			case Self:
				changeStartBarText(txtTargetSelf);
				break;
			case Single:
				changeStartBarText(txtTargetSingle);
				break;
			case SingleAlly:
				changeStartBarText(txtTargetSingleAlly);
				break;
			case SingleEnemy:
				changeStartBarText(txtTargetSingleEnemy);
				break;
			case Everybody:
				changeStartBarText(txtTargetEverybody);
				getTouchTargets(-1, -1);
				break;
			}
			mainMenu.close();
		default:
			break;			
		}
		
		state = newState;
		
	}
	private void handleMenuOption(String opt)
	{
		if(opt.equals("atk"))
		{
			targetType = TargetTypes.Single;
			currentChar.setBattleAction(Action.Attack);
			currentChar.setFace(faces.Ready);
			changeState(BattleStates.TARGET);
		}
		else if(opt.equals("itm"))
		{
			changeState(BattleStates.SELECTITEM);
		}
		else if(opt.equals("act"))
		{
			targetType = currentChar.getCombatActionTargetType();
			currentChar.setBattleAction(Action.CombatAction);
			currentChar.setFace(faces.Ready);
			changeState(BattleStates.TARGET);
		}
		else if(opt.equals("grd"))
		{
			currentChar.setFace(faces.Ready);
			currentChar.setBattleAction(Action.Guard);
			nextCharacter();
		}
		else if(opt.equals("ab"))
		{
			changeState(BattleStates.SELECTABILITY);
		}
		else if(opt.equals("run"))
		{
			changeState(BattleStates.SELECT);
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
		
		//determine first nondead character
		for(int i = 0; i < partyList.size(); ++i)
			if(!partyList.get(i).isDead())
			{
				currentCharIndex = i;
				currentChar = partyList.get(currentCharIndex);
				break;
			}
	}
	private void addBattleEvent(PlayerCharacter source, List<PlayerCharacter> targets){battleEvents.add(new BattleEvent(source, targets));}
	public void addMessage(String str)
	{
		messageQueue.add(str);
		infoPanel.show();
		updatePanels();
	}
	
	private void nextCharacter()
	{
		//changeState(BattleStates.SELECT);
		//mainMenu.close();		
		recedeChar();
		nextChar = true;
	}
	private void previousCharacter()
	{
		if(selCharOpened)
		{
			if(currentCharIndex == 0)
				changeState(BattleStates.START);
			else
			{			
				recedeChar();
				prevChar = true;
				
			}
			
		}
		
		
	}
	private void handleNextPrev()
	{
		if((nextChar || prevChar) && selCharClosed)
		{
			if(prevChar)
			{
				if(currentChar.getAction() == Action.Item)
					currentChar.unuseItem();
				
				currentChar = partyList.get(--currentCharIndex);
				changeState(BattleStates.SELECT);
				
				if(currentChar.getAction() == Action.Item)
					currentChar.unuseItem();

			}
			else 
			{
				//loop until nondead party member
				do ++currentCharIndex;
				while(currentCharIndex < partyList.size() && partyList.get(currentCharIndex).isDead());
				
				if(currentCharIndex < partyList.size())
				{					
					currentChar = partyList.get(currentCharIndex);
					changeState(BattleStates.SELECT);
				}
				else
					changeState(BattleStates.ACT);				
			}			
			nextChar = prevChar = false;
		}
	}
	
	private void drawActors()
	{
		for(PlayerCharacter c : partyList)
		{
			c.battleRender();
			characterPanes[c.Index()].render();
		}
			
		
		for(Enemy e : encounter.Enemies())
			e.battleRender();
		
			
	}
	private void drawPanels()
	{		
		mainMenu.render();
		if(mainMenu.Closed())
			startBar.render();
		
		infoPanel.render();
		
		switch(state)
		{				
		case SELECTITEM:							
			if(mainMenu.getCurrentSelectedEntry() != null)
			{
				mpWindow.getTextAt(0).text = "Qty: "+((Item)mainMenu.getCurrentSelectedEntry().obj).getCount();
				mpWindow.render();
			}
			break;
			
		case SELECTABILITY:			
			if(mainMenu.getCurrentSelectedEntry() != null)
			{
				mpWindow.getTextAt(0).text = "Cost: "+((Ability)mainMenu.getCurrentSelectedEntry().obj).MPCost();
				mpWindow.render();
			}
			break;
		default:
			break;
		
		}
		displayNamePanel.render();
	}
	private void drawSelect()
	{
		for(PlayerCharacter t : targets)
		{
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
	
	public void update()
	{
		updatePanels();
		updateCharacterPanes();
		
		handleCharAdvancing();
		updateCharacterPositions();
		updateDamageMarkers();
		
		handleNextPrev();
		
		switch(state)
		{
		case ACT:
			updateActStatus();
			break;
		case DEFEAT:
		case VICTORY:
			if(messageQueue.size() == 0 && Global.screenFader.isFadedIn())
				triggerEndBattle();
			else				
				if(Global.screenFader.isFadedOut())
				{
					Global.map.getBackdrop().unload();
					Global.musicBox.resumeLastSong();
					Global.screenFader.fadeIn(2);
					Global.GameState = States.GS_WORLDMOVEMENT;				
				}
		default:
			break;		
		}		
	}	
	public void render()
	{
		Global.map.getBackdrop().render();
		drawActors();
		drawPanels();
		drawSelect();
		
		Global.renderAnimations();
		drawDamageMarkers();
		
		Global.screenFader.render();
		
	}
	
	public void backButtonPressed()
	{
		switch(state)
		{
		case TARGET:
			changeState(BattleStates.SELECT);
			if(currentChar.getAction() == Action.Item)
				currentChar.unuseItem();
			break;
		case SELECT:
			previousCharacter();
			break;
		default:
			break;
				
		}
		
	}
	public void touchActionUp(int x, int y)
	{
		if(messageQueue.size() > 0)
		{
			messageQueue.remove(0);
		}
		else
		{
			switch(state)
			{
			case START:
				changeState(BattleStates.SELECT);
				break;
			case SELECT:
				switch(mainMenu.touchActionUp(x, y))
				{
				case Selected:
					handleMenuOption((String)(mainMenu.getSelectedEntry().obj));
					break;
				case Close:
					previousCharacter();
					break;
				default:
					break;
				}
				break;
			case SELECTABILITY:
				switch(mainMenu.touchActionUp(x, y))
				{
				case Selected:
					if(!mainMenu.getSelectedEntry().Disabled())
					{
						Ability ab = (Ability)(mainMenu.getSelectedEntry().obj);
						targetType = ab.TargetType();
						currentChar.setFace(faces.Casting);
						currentChar.setAbilityToUse(ab);
						changeState(BattleStates.TARGET);
					}				
					break;
				case Close:
					changeState(BattleStates.SELECT);
					break;
				default:break;
				}
				break;
			case SELECTITEM:
				switch(mainMenu.touchActionUp(x, y))
				{
				case Selected:
					Item itm = (Item)(mainMenu.getSelectedEntry().obj);
					targetType = itm.getTargetType();
					currentChar.setFace(faces.Ready);
					currentChar.setItemToUse(itm);
					changeState(BattleStates.TARGET);
					break;
				case Close:
					changeState(BattleStates.SELECT);
					break;
				default:break;
				}
				break;
			case TARGET:
				if(startBar.contains(x, y))
				{
					if(currentChar.getAction() == Action.Item)
						currentChar.unuseItem();
					changeState(BattleStates.SELECT);
				}
					
				else
				{
					if(targets.size() > 0)
					{
						//targets were selected
						//changeState(BattleStates.SELECT);
						currentChar.setTargets(new ArrayList<PlayerCharacter>(targets));
						nextCharacter();
						//targets.clear();
					
					}
					else
					{
						if(currentChar.getAction() == Action.Item)
							currentChar.unuseItem();
						changeState(BattleStates.SELECT);	
					}
										
				}			
				break;
			default:
				break;
			}
		}		
	}
	public void touchActionDown(int x, int y)
	{
		if(messageQueue.size() == 0)
		{
			switch(state)
			{
			case SELECTABILITY:
			case SELECTITEM:
			case SELECT:
				mainMenu.touchActionDown(x, y);
				break;
			case TARGET:
				getTouchTargets(x, y);
				break;
			default:
				break;
			}
		}		
	}
	public void touchActionMove(int x, int y)
	{
		if(messageQueue.size() == 0)
		{
			switch(state)
			{
			case SELECTABILITY:
			case SELECTITEM:
			case SELECT:
				mainMenu.touchActionMove(x, y);
				break;
			case TARGET:
				getTouchTargets(x, y);
				break;
			default:
				break;
				
			}
		}
		
	}
	private void getTouchTargets(int x, int y)
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
		case Self:
			targets.add(currentChar);
			break;
		case AllAllies:
			for(PlayerCharacter c : partyList)
				targets.add(c);
			break;
		case AllEnemies:
			for(Enemy e : encounter.Enemies())
				if(!e.isDead())
					targets.add(e);
			break;
		case Everybody:
			for(Enemy e : encounter.Enemies())
				if(!e.isDead())
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
	private void setTarget(PlayerCharacter c)
	{
		targets.clear();
		targets.add(c);
	}
	
	public enum BattleStates
	{
		START,
		SELECT,
		SELECTITEM,
		SELECTABILITY,
		TARGET,
		ACT,
		RUN,
		VICTORY,
		DEFEAT		
	}
	
}