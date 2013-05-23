package bladequest.world;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.graphics.Color;
import android.graphics.Point;
import android.util.Log;
import bladequest.combat.BattleLibrary;
import bladequest.combatactions.CombatAction;
import bladequest.combatactions.combAuras;
import bladequest.combatactions.combDragonForm;
import bladequest.combatactions.combStance;
import bladequest.combatactions.combSteal;
import bladequest.combatactions.combWish;
import bladequest.graphics.BattleAnim;
import bladequest.graphics.BattleAnimObjState;
import bladequest.graphics.BattleAnimObjState.PosTypes;
import bladequest.graphics.BattleAnimObject;
import bladequest.graphics.BattleAnimObject.Types;
import bladequest.scripting.FileTokenizer;
import bladequest.scripting.LibraryWriter;
import bladequest.scripting.Parser;
import bladequest.scripting.Script;
import bladequest.scripting.Script.BadSpecialization;
import bladequest.scripting.ScriptVar;
import bladequest.scripting.ScriptVar.BadTypeException;
import bladequest.system.BqActivity;
import bladequest.system.DataLine;
import bladequest.system.FileReader;


public class GameDataLoader 
{
	private static FileSections section;
	private static final String TAG = GameDataLoader.class.getSimpleName();
	public static int subtractScriptFn(int x, int y)
	{
		return x-y;
	}
	public static boolean equalsScriptFn(int x, int y)
	{
		return x == y;
	}	
	public static Map<String, ScriptVar> getStandardLibrary()
	{
		LibraryWriter library = new LibraryWriter();
	
		try {
			library.add("subtract", GameDataLoader.class.getMethod("subtractScriptFn", int.class, int.class));
			library.add("equals", GameDataLoader.class.getMethod("equalsScriptFn", int.class, int.class));
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (BadTypeException e) {
		} catch (BadSpecialization e) {
		}
		
		BattleLibrary.publishLibrary(library);
		return library.getLibrary();
	}
	private static Script compileScript(BqActivity activity, String file, Map<String, ScriptVar> standardLibrary)
	{
			Script script = new Script(standardLibrary);
		
		Parser p = null;
		try {
			p = new Parser(new FileTokenizer(activity.getAssets().open(file)), script);
		} catch (IOException e) {
			// ?
		}
		p.run(); //populates script!
		return script;
	}
	public static void load(BqActivity activity)
	{
		Map<String, ScriptVar> standardLibrary = getStandardLibrary();
		
		loadFile(activity, "data/sprites.dat");
		loadFile(activity, "data/battleanims.dat");
		compileScript(activity, "data/abilities.dat", standardLibrary);
		loadFile(activity, "data/music.dat");
		loadFile(activity, "data/items.dat");
		loadFile(activity, "data/characters.dat");
		loadFile(activity, "data/enemies.dat");
		loadFile(activity, "data/battles.dat");
		loadFile(activity, "data/merchants.dat");
		
		Script script = compileScript(activity, "data/testScript.dat", standardLibrary);
		
		try {
			ScriptVar helloWorld = script.getVariable("main").apply(new ScriptVar.EmptyList());
			if (!helloWorld.getString().equals("hello world"))
			{
				//Crash, trolllololo
				new ArrayList<String>().get(42);
			}
		} catch (BadTypeException e) {
		}
		
		Log.d(TAG, "Loading maps.");Global.loadMaps("maps");
		//loadFile(activity, "data/gamedata.dat");
		
	}
	
	public static void loadNewGame(BqActivity activity)
	{
		loadFile(activity, "data/gamedata.dat");		
	}
	
	private static void loadFile(BqActivity activity, String path)
	{
		Log.d(TAG, "Loading " + path);
		
		InputStream is = null;
		
		try {is = activity.getAssets().open(path);} catch (Exception e) {
			Log.d(TAG, "Unable to open file " + path);
			Global.closeGame();}		
		
		FileReader fr = new FileReader(is);
		
		String s = "";
		List<DataLine> lines = new ArrayList<DataLine>();
		
		do
		{ 
			s = fr.ReadLine();
			if(s.length() > 0)
				lines.add(new DataLine(s)); 
		} while(s.length() > 0);
		
		for(DataLine dl : lines)
			LoadDataLine(dl);
		
	}
	
	private static void LoadDataLine(DataLine dl)
	{
		if(dl.item.charAt(0) == '#')
			return;
		else
		if(dl.item.charAt(0) == '[')
		{
			if(dl.item.equals("[gamestart]"))
				section = FileSections.GameStart;
			else if(dl.item.equals("[items]"))
				section = FileSections.Items;
			else if(dl.item.equals("[battleanims]"))
				section = FileSections.BattleAnims;
			else if(dl.item.equals("[music]"))
				section = FileSections.Music;
			else if(dl.item.equals("[sprites]"))
				section = FileSections.Sprites;
			else if(dl.item.equals("[characters]"))
				section = FileSections.Characters;	
			else if(dl.item.equals("[enemies]"))
				section = FileSections.Enemies;
			else if(dl.item.equals("[encounters]"))
				section = FileSections.Encounters;
			else if(dl.item.equals("[merchants]"))
				section = FileSections.Merchants;
				
		}
		else
		{
			switch(section)
			{
			case GameStart:
				loadGameStartLine(dl);
				break;
			case Items:
				loadItemLine(dl);
				break;
			case Music:
				loadMusicLine(dl);
				break;
			case Sprites:
				loadSpriteLine(dl);
				break;
			case Characters:
				loadCharacterLine(dl);
				break;
			case Enemies:
				loadEnemyLine(dl);
				break;
			case Encounters:
				loadEncounterLine(dl);
				break;
			case Merchants:
				loadMerchantLine(dl);
				break;
			case BattleAnims:
				loadBattleAnimLine(dl);
				break;
			}
		}
		
	}
	
	private static void loadGameStartLine(DataLine dl)
	{
		
	}
	
	private static Item itm;
	private static String itemName;
	private static void loadItemLine(DataLine dl)
	{
		if(dl.item.equals("item"))
		{
			itemName = dl.values.get(0);
			
			itm = new Item(itemName, 
					dl.values.get(1), 
					getItemType(dl.values.get(2)),
					dl.values.get(3), 
					dl.values.get(4));
			
		}
		else if(dl.item.equals("power"))
		{
			itm.setPower(Integer.parseInt(dl.values.get(0)));
		}
		else if(dl.item.equals("usableby"))
		{
			for(String c : dl.values)
			{
				if(c.length() > 1)
					itm.addUsableBy(c);
			}
		}
		else if(dl.item.equals("targettype"))
		{
			itm.setTargetType(getTargetType(dl.values.get(0)));
		}
		else if(dl.item.equals("anim"))
		{
			itm.setAnim(dl.values.get(0));
		}
		else if(dl.item.equals("sellable"))
		{
			itm.setSellable(Boolean.parseBoolean(dl.values.get(0)));
		}
		else if(dl.item.equals("value"))
		{
			itm.setValue(Integer.parseInt(dl.values.get(0)));
		}
		else if(dl.item.equals("statmods"))
		{
			for(int i = 0; i < Stats.NUM_STATS.ordinal(); ++i)
				itm.addStatMod(i, Integer.parseInt(dl.values.get(i)));
		}
		else if(dl.item.equals("damagecomp"))
		{
			itm.addDamageComponent(getAffinity(dl.values.get(0)), Float.parseFloat(dl.values.get(1)));
		}
		else if(dl.item.equals("swing"))
		{
			itm.initSwingData(dl.values.get(0), dl.values.get(1));
		}
		else if(dl.item.equals("swingbase"))
		{
			itm.swingColor(
					false, 
					Integer.parseInt(dl.values.get(0)), 
					Color.argb(
							255, 
							Integer.parseInt(dl.values.get(1)), 
							Integer.parseInt(dl.values.get(2)), 
							Integer.parseInt(dl.values.get(3))));
		}
		else if(dl.item.equals("swingslash"))
		{
			itm.swingColor(
					true, 
					Integer.parseInt(dl.values.get(0)), 
					Color.argb(
							255, 
							Integer.parseInt(dl.values.get(1)), 
							Integer.parseInt(dl.values.get(2)), 
							Integer.parseInt(dl.values.get(3))));
		}
		else if(dl.item.equals("enditem"))
		{
			Global.items.put(itemName, itm);
		}
	}
	
	private static void loadMusicLine(DataLine dl)
	{
		
	}
	
	private static Stats getAffinity(String affname)
	{
		if(affname.equals("fire"))
			return Stats.Fire;
		else if(affname.equals("water"))
			return Stats.Water;
		else if(affname.equals("wind"))
			return Stats.Wind;
		else if(affname.equals("earth"))
			return Stats.Earth;
		else return null;
	}
	
	private static BattleAnim ba;
	private static BattleAnimObject baObj;
	private static BattleAnimObjState baState;	
	private static String baName;
	private static void loadBattleAnimLine(DataLine dl)
	{
		if(dl.item.equals("ba"))
		{	baName = dl.values.get(0);
			ba = new BattleAnim(Float.parseFloat(dl.values.get(1)));
			
		}else if(dl.item.equals("obj"))
		{	baObj = new BattleAnimObject(
				getBAObjType(dl.values.get(0)), 
				Integer.parseInt(dl.values.get(1)) != 0,
				dl.values.get(2));
		}else if(dl.item.equals("state"))
		{	baState = new BattleAnimObjState(
				Integer.parseInt(dl.values.get(0)),
				getBAStatePosType(dl.values.get(1)));
		}else if(dl.item.equals("stroke"))
		{	baState.strokeWidth = Float.parseFloat(dl.values.get(0));	
		}else if(dl.item.equals("pos"))
		{	
			if(Integer.parseInt(dl.values.get(0)) == 1)
				baState.pos1 = new Point(Integer.parseInt(dl.values.get(1)), Integer.parseInt(dl.values.get(2)));
			else
				baState.pos2 = new Point(Integer.parseInt(dl.values.get(1)), Integer.parseInt(dl.values.get(2)));
		}else if(dl.item.equals("size"))
		{	baState.size = new Point(Integer.parseInt(dl.values.get(0)), Integer.parseInt(dl.values.get(1)));
		}else if(dl.item.equals("show"))
		{	baState.show = Integer.parseInt(dl.values.get(0)) != 0;	
		}else if(dl.item.equals("colorize"))
		{	baState.colorize = Float.parseFloat(dl.values.get(0));	
		}else if(dl.item.equals("argb"))
		{	baState.argb(
				Integer.parseInt(dl.values.get(0)), 
				Integer.parseInt(dl.values.get(1)), 
				Integer.parseInt(dl.values.get(2)), 
				Integer.parseInt(dl.values.get(3)));
		}else if(dl.item.equals("copylast"))
		{	ba.copyLastObject(
				Integer.parseInt(dl.values.get(0)), 
				Integer.parseInt(dl.values.get(1)));
		}else if(dl.item.equals("endstate"))
		{	baObj.addState(baState);
		}else if(dl.item.equals("endobj"))
		{	ba.addObject(baObj);
		}else if(dl.item.equals("linterp"))
		{	baObj.interpolateLinearly();
		}else if(dl.item.equals("loop"))
		{	ba.loop();
		}else if(dl.item.equals("bmpsrc"))
		{	baState.setBmpSrcRect(Integer.parseInt(dl.values.get(0)), 
				Integer.parseInt(dl.values.get(1)), 
				Integer.parseInt(dl.values.get(2)), 
				Integer.parseInt(dl.values.get(3)));
		}else if(dl.item.equals("endba"))
		{ 
			Global.battleAnims.put(baName, ba);			
		}
	}
	
	private static void loadSpriteLine(DataLine dl)
	{
		if(dl.item.equals("worldsprite"))
			Global.createWorldSprite(
					dl.values.get(0), 
					dl.values.get(1), 
					Integer.parseInt(dl.values.get(2)), 
					Integer.parseInt(dl.values.get(3)));
		else if(dl.item.equals("enemysprite"))
			Global.createEnemySprite(
					dl.values.get(0), 
					dl.values.get(1), 
					Integer.parseInt(dl.values.get(2)), 
					Integer.parseInt(dl.values.get(3)), 
					Integer.parseInt(dl.values.get(4)), 
					Integer.parseInt(dl.values.get(5)));
			
		else if(dl.item.equals("battlesprite"))
			Global.createBattleSprite(
					dl.values.get(0), 
					Integer.parseInt(dl.values.get(1)), 
					Integer.parseInt(dl.values.get(2)));
	}
	
	private static PlayerCharacter c;	
	private static void loadCharacterLine(DataLine dl)
	{		
		if(dl.item.equals("character"))
		{
			c = new PlayerCharacter(
					dl.values.get(0),
					dl.values.get(1),
					dl.values.get(2),
					dl.values.get(3));
		}
		else if(dl.item.equals("portrait"))
		{
			c.setPortrait(Integer.parseInt(dl.values.get(0)), Integer.parseInt(dl.values.get(1)));
		}
		else if(dl.item.equals("abilities"))
		{
			c.setAbilitiesName(dl.values.get(0));
		}
		else if(dl.item.equals("combataction"))
		{
			c.setCombatAction(getCombatAction(dl.values.get(0)));
		}
		else if(dl.item.equals("basestats"))
		{
			c.setBaseStats(
					Integer.parseInt(dl.values.get(0)),
					Integer.parseInt(dl.values.get(1)),
					Integer.parseInt(dl.values.get(2)),
					Integer.parseInt(dl.values.get(3)));
		}
		else if(dl.item.equals("level"))
		{
			c.modifyLevel(Integer.parseInt(dl.values.get(0)), false);
		}
		else if(dl.item.equals("equip"))
		{
			c.firstEquip(dl.values.get(0));
		}
		else if(dl.item.equals("ability"))
		{
			c.addAbility(dl.values.get(0));
		}
		else if(dl.item.equals("lability"))
		{
			c.addLearnableAbility(dl.values.get(0), Integer.parseInt(dl.values.get(1)));
		}
		else if(dl.item.equals("endcharacter"))
		{
			c.fullRestore();
			Global.characters.put(c.getName(), c);
		}
	}
	
	
	
	private static Merchant me;
	private static String meName;	
	private static void loadMerchantLine(DataLine dl)
	{
		if(dl.item.equals("merchant"))
		{
			meName = dl.values.get(0);
			me = new Merchant(meName);
			
		}
		else if(dl.item.equals("item"))
		{
			me.addItem(Global.items.get(dl.values.get(0)));
			if(dl.values.get(1) != null && dl.values.get(1).length() > 0)
				me.setLimitedQtyItem(dl.values.get(0), Integer.parseInt(dl.values.get(1)), true);
		}
		else if(dl.item.equals("endmerchant"))
			Global.merchants.put(meName, me);	
		else if(dl.item.equals("greeting"))
			me.greeting = dl.values.get(0);
		else if(dl.item.equals("buying"))
			me.buying = dl.values.get(0);
		else if(dl.item.equals("selling"))
			me.selling = dl.values.get(0);
		else if(dl.item.equals("equipConfirm"))
			me.equipConfirm = dl.values.get(0);
		else if(dl.item.equals("equipSellOld"))
			me.equipSellOld = dl.values.get(0);
		else if(dl.item.equals("farewell"))
			me.farewell = dl.values.get(0);		
	}
	
	
	private static Enemy e;
	private static String eName;
	private static void loadEnemyLine(DataLine dl)
	{		
		if(dl.item.equals("enemy"))
		{
			eName = dl.values.get(0);			
			e = new Enemy(dl.values.get(1),dl.values.get(2));
		}
		else if(dl.item.equals("basestats"))
		{
			e.setBaseStats(
					Integer.parseInt(dl.values.get(0)),
					Integer.parseInt(dl.values.get(1)),
					Integer.parseInt(dl.values.get(2)),
					Integer.parseInt(dl.values.get(3)));
		}
		else if(dl.item.equals("statmods"))
		{
			for(int i = 0; i < Stats.NUM_STATS.ordinal(); ++i)
				e.setStatMod(i, Integer.parseInt(dl.values.get(i)));
		}
		else if(dl.item.equals("level"))
		{
			e.modifyLevel(Integer.parseInt(dl.values.get(0)));
		}
		else if(dl.item.equals("atkanim"))
		{
			e.setAttackAnimation(dl.values.get(0));
		}
		else if(dl.item.equals("gold"))
		{
			e.setGold(Integer.parseInt(dl.values.get(0)));
		}
		else if(dl.item.equals("items"))
		{
			e.setItems(
					dl.values.get(0), 
					Integer.parseInt(dl.values.get(1)), 
					dl.values.get(2), 
					Integer.parseInt(dl.values.get(3)), 
					Boolean.parseBoolean(dl.values.get(4)));
		}
		else if(dl.item.equals("ai"))
		{
			e.setAI(Integer.parseInt(dl.values.get(0)));
		}
		else if(dl.item.equals("bossmods"))
		{
			e.setBossMods(Float.parseFloat(dl.values.get(0)));
		}
		else if(dl.item.equals("ability"))
		{
			e.addAbility(
					dl.values.get(0), 
					Integer.parseInt(dl.values.get(1)), 
					Integer.parseInt(dl.values.get(2)), 
					Integer.parseInt(dl.values.get(3)));
		}
		else if(dl.item.equals("endenemy"))
		{
			Global.enemies.put(eName, e);
		}
	}
	
	private static String encName;
	private static Encounter enc;
	
	private static void loadEncounterLine(DataLine dl)
	{
		if(dl.item.equals("encounter"))
		{
			encName = dl.values.get(0);
			enc = new Encounter(encName, dl.values.get(1));
		}
		else if(dl.item.equals("enemy"))
		{
			enc.addEnemy(
					dl.values.get(0), 
					Integer.parseInt(dl.values.get(1)), 
					Integer.parseInt(dl.values.get(2)));
		}
		else if(dl.item.equals("disablerunning"))
		{
			enc.disableRunning = true;
		}
		else if(dl.item.equals("endencounter"))
		{
			Global.encounters.put(encName, enc);
		}
	}
	
	private static TargetTypes getTargetType(String str)
	{
		if(str.equals("single"))
			return TargetTypes.Single;
		else if(str.equals("singleenemy"))
			return TargetTypes.SingleEnemy;
		else if(str.equals("singleally"))
			return TargetTypes.SingleAlly;
		else if(str.equals("allallies"))
			return TargetTypes.AllAllies;
		else if(str.equals("allenemies"))
			return TargetTypes.AllEnemies;
		else if(str.equals("self"))
			return TargetTypes.Self;
		else
			return null;
	}
	
	private static Item.Type getItemType(String str)
	{
		if(str.equals("usable"))
			return Item.Type.Usable;
		else if(str.equals("weapon"))
			return Item.Type.Weapon;
		else if(str.equals("shield"))
			return Item.Type.Shield;
		else if(str.equals("torso"))
			return Item.Type.Torso;
		else if(str.equals("helmet"))
			return Item.Type.Helmet;
		else if(str.equals("accessory"))
			return Item.Type.Accessory;
		else if(str.equals("key"))
			return Item.Type.Key;
		else
			return null;
	}
	
	private static CombatAction getCombatAction(String str)
	{
		if(str.equals("combDragonForm"))
			return new combDragonForm();
		else if(str.equals("combStance"))
			return new combStance();
		else if(str.equals("combSteal"))
			return new combSteal();
		else if(str.equals("combWish"))
			return new combWish();
		else if(str.equals("combAuras"))
			return new combAuras();		
		else
			return null;
	}
	
	private static BattleAnimObject.Types getBAObjType(String str)
	{
		if(str.equals("elipse"))
			return Types.Elipse;
		else if(str.equals("line"))
			return Types.Line;
		else if(str.equals("rect"))
			return Types.Rect;
		else if(str.equals("bmp"))
			return Types.Bitmap;
		else
			return null;
	}
	
	private static BattleAnimObjState.PosTypes getBAStatePosType(String str)
	{
		if(str.equals("tar"))
			return PosTypes.Target;
		else if(str.equals("src"))
			return PosTypes.Source;
		else if(str.equals("scrn"))
			return PosTypes.Screen;
		else
			return null;
	}
	
	private static enum FileSections
	{
		GameStart,
		Items,
		Music,
		Sprites,
		Characters,
		Enemies,
		Encounters,
		Merchants,
		BattleAnims
	}

}
