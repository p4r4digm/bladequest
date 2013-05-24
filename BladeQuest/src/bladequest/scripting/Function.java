package bladequest.scripting;

import java.util.ArrayList;
import java.util.List;

public class Function extends ScriptVar {
	
	public Function(FunctionSpecializer specializer)
	{
		this.specializer = specializer;
		children = new ArrayList<ScriptVar>();
	}
	FunctionSpecializer specializer;
	List<ScriptVar> children;
	List<ScriptVar> curriedValues;
	public ScriptVar clone()
	{
		Function out = new Function(specializer);
		out.children = children; //needs currying anywho
		return out;
	}
	@Override
	public ScriptVar curryValues(List<ScriptVar> values)
	{
		curriedValues = values;
		return this;
	}
	@Override
	public void addChildFunction(ScriptVar childFunc)
	{
		children.add(childFunc);
	}	
	@Override
	public FunctionSpecializer getSpecializer()  
	{
		return specializer;
	}
	@Override
	public ScriptVar getSpecializedChild(FunctionSpecializer specialization) throws BadTypeException 
	{
		for (ScriptVar child : children)
		{
			if (child.getSpecializer().Equals(specialization)) return child;
		}
		return null;
	}	
	@Override
	public ScriptVar apply(ScriptVar var) throws BadTypeException {
		// TODO Auto-generated method stub
		ScriptVar nextFunc = null;
		curriedValues.add(var);
		SpecializationLevel specialization = SpecializationLevel.NotSpecialized;
		for (ScriptVar child : children)
		{
			SpecializationLevel thisSpecialization = child.getSpecializer().getSpecializationLevelFor(var);
			if (thisSpecialization.ordinal() > specialization.ordinal())
			{
				nextFunc = child;
				specialization = thisSpecialization;
			}
		}
		if (nextFunc == null)
		{
			throw new BadTypeException();
		}
		return nextFunc.clone().curryValues(curriedValues);
	}
	@Override
	public boolean isFunction()
	{
		return true;
	}	
}