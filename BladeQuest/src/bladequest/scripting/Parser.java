package bladequest.scripting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import bladequest.scripting.Script.BadSpecialization;
import bladequest.scripting.ScriptVar.BadTypeException;
import bladequest.scripting.ScriptVar.EmptyList;
import bladequest.scripting.ScriptVar.ListNode;
import bladequest.scripting.ScriptVar.SpecializationLevel;

public class Parser {
		
	private abstract class ParserState
	{
		protected List<ParserState> stateStack;
		
		public ParserState(List<ParserState> stateStack)
		{
			this.stateStack = stateStack;
		}
		protected void popState()
		{
			stateStack.remove(stateStack.size()-1);
		}
		protected void pushState(ParserState state)
		{
			stateStack.add(state);
		}
		public void publishStatement(Statement statement) throws ParserException, BadTypeException
		{
			stateStack.get(stateStack.size()-2).addChildStatement(statement);
		}
		public void beginList() throws ParserException {throw new ParserException("Not implemented!");}
		public void endList() throws ParserException {throw new ParserException("Not implemented!");}
		public void beginParen() throws ParserException {throw new ParserException("Not implemented!");}
		public void endParen() throws ParserException {throw new ParserException("Not implemented!");}
		public void endLine() throws ParserException, BadTypeException, BadSpecialization {throw new ParserException("Not implemented!");}
		public void localDef() throws ParserException {throw new ParserException("Not implemented!");}
		public void listSeparator() throws ParserException {throw new ParserException("Not implemented!");}
		public void readName(String name) throws ParserException {throw new ParserException("Not implemented!");}
		public void readNumber(int number) throws ParserException {throw new ParserException("Not implemented!");}
		public void readString(String string) throws ParserException {throw new ParserException("Not implemented!");}
		public void readFloat(float number) throws ParserException {throw new ParserException("Not implemented!");}
		public void readBool(boolean bool) throws ParserException {throw new ParserException("Not implemented!");}
		public void lambdaFunction() throws ParserException {throw new ParserException("Not implemented!");}
		public void endLambdaFunction() throws ParserException {throw new ParserException("Not implemented!");}
		public void patternMatch() throws ParserException {throw new ParserException("Not implemented!");}
		public void caseMarker() throws ParserException {throw new ParserException("Not implemented!");}		
		public void addChildStatement(Statement statement) throws ParserException, BadTypeException {throw new ParserException("Not implemented!");}
	}
	
	
	List<ParserState> stateStack;
	Script script;
	
	ParserState getFunctionLevelParserState()
	{
		return new ParserState(stateStack)
		{
			@Override
			public void readName(String name)
			{
				pushState(getFunctionSpecializationState(name));
			}
			@Override			
			public void endLine() 
			{
				//zzz
			}
			@Override
			public void addChildStatement(Statement statement) throws ParserException //variable declaration statement!
			{
				ExecutionState state = new ExecutionState();
				state.functionArgs = new ArrayList<ScriptVar>();
				ScriptVar globalVar = statement.invoke(state);
				String globalVarName = state.locals.keySet().iterator().next();  //holy shit java really? 
				script.addGlobal(globalVarName, globalVar);
			}
			@Override
			public void localDef() throws ParserException 
			{
				pushState(getStatementState(new ArrayList<String>(), new ArrayList<String>()));
				stateStack.get(stateStack.size()-1).localDef();
			}
		};
	}
	
	
	Map<String, FunctionSpecializer> typeSpecializers;
	FunctionSpecializer genericSpecializer;
	
	ParserState getFunctionSpecializationState(String functionName)
	{
		return new ParserState(stateStack)
		{
			String fnName;
			List<FunctionSpecializer> specializers;
			List<String> argNames;
			FunctionSpecializer prevSpecializer;
			ParserState initialize(String fnName)
			{
				this.argNames = new ArrayList<String>();
				this.specializers = new ArrayList<FunctionSpecializer>();
				this.fnName = fnName;
				return this;
			}
			@Override
			public void readName(String name)
			{
				//a name... see if it's a specializer
				FunctionSpecializer specializer = typeSpecializers.get(name);
				if (specializer == null)
				{
					if (prevSpecializer == null)
					{
						specializers.add(genericSpecializer);	
					}
					else
					{
						specializers.add(prevSpecializer);
						prevSpecializer = null;
					}
					argNames.add(name);
				}
				else
				{
					prevSpecializer = specializer;
				}
			}
			public void readNumber(int number) throws ParserException 
			{
				argNames.add("");
				specializers.add(new FunctionSpecializer()
				{
					public int val;
					FunctionSpecializer initialize(int val)
					{
						this.val = val;
						return this;
					}
					@Override					
					public boolean Equals(FunctionSpecializer rhs) {
						return this.getClass() == rhs.getClass() && val == this.getClass().cast(rhs).val;
					}

					@Override
					public SpecializationLevel getSpecializationLevelFor(
							ScriptVar var) {
						try {
							if (var.isInteger() && var.getInteger() == val) return SpecializationLevel.ValueSpecialized;
						} catch (BadTypeException e) {
						}
						return SpecializationLevel.NotSpecialized;
					}
				}.initialize(number));
			}
			@Override
			public void endLine()

			{
				popState();
				pushState(getFunctionDefinitionState(fnName, specializers, argNames));
			}
		}.initialize(functionName);		
	}
	
	private class ExecutionState
	{
		ExecutionState()
		{
			locals = new HashMap<String, ScriptVar>();
		}
		public List<ScriptVar> functionArgs;
		public Map<String, ScriptVar> locals;
	}
	
	private interface Statement
	{
		ScriptVar invoke(ExecutionState state) throws BadTypeException;
	}
	
	private Statement getStatementFromLocal(String name)
	{
		return new Statement()
		{
			private String localName;
			public Statement initialize(String localName)
			{
				this.localName = localName;
				return this;
			}					
			public ScriptVar invoke(ExecutionState state)
			{
				return state.locals.get(localName);
			}
		}.initialize(name);		
	}
	
	private Statement getStatementFromArgument(int argNumber)
	{
		return new Statement()
		{
			private int argNumber;
			public Statement initialize(int argNumber)
			{
				this.argNumber = argNumber;
				return this;
			}					
			public ScriptVar invoke(ExecutionState state)
			{
				return state.functionArgs.get(argNumber);
			}
		}.initialize(argNumber);		
	}
	
	private Statement getStatementFromGlobal(ScriptVar globalVar)
	{
		return new Statement()
		{
			private ScriptVar global;
			
			public Statement initialize(ScriptVar global)
			{
				this.global = global;
				return this;
			}
			public ScriptVar invoke(ExecutionState state)
			{
				return global;
			}
		}.initialize(globalVar);	
	}
	
	private Statement getStatementFromName(String name, List<String> argNames, List<String> locals) throws ParserException
	{
		//local variable?
		for (String arg : locals)
		{
			if (arg.equals(name))
			{
				return getStatementFromLocal(name);
			}
		}
		//argument name?
		int argNumber = 0;
		for (String arg : argNames)
		{
			if (arg.equals(name))
			{
				return getStatementFromArgument(argNumber);
			}
			++argNumber;
		}
		//global variable name?
		ScriptVar global = script.getVariable(name);
		if (global != null)
		{
			return getStatementFromGlobal(global);
		}
		//don't know what this is, guess it's an error.
		throw new ParserException("Undefined variable!");
	}
	
	private Statement getIntegerStatement(int arg)
	{
		return new Statement()
		{
			private int arg;
			public Statement initialize(int arg)
			{
				this.arg = arg;
				return this;
			}					
			public ScriptVar invoke(ExecutionState state)
			{
				return ScriptVar.toScriptVar(arg);
			}
		}.initialize(arg);		
	}
	private Statement getFloatStatement(float arg)
	{
		return new Statement()
		{
			private float arg;
			public Statement initialize(float arg)
			{
				this.arg = arg;
				return this;
			}				
			@Override
			public ScriptVar invoke(ExecutionState state)
			{
				return new ScriptVar()
				{
					@Override
					public ScriptVar clone() {
						return this;
					}
					@Override
					public boolean isFloat() { return true; }
					public float getFloat() {return arg;}
				};
			}
		}.initialize(arg);		
	}	
	private Statement getBoolStatement(boolean arg)
	{
		return new Statement()
		{
			private boolean arg;
			public Statement initialize(boolean arg)
			{
				this.arg = arg;
				return this;
			}					
			public ScriptVar invoke(ExecutionState state)
			{
				return new ScriptVar()
				{
					@Override
					public ScriptVar clone() {
						return this;
					}
					@Override
					public boolean isBoolean() { return true; }
					@Override
					public boolean getBoolean() {return arg;}
				};
			}
		}.initialize(arg);		
	}	
	private Statement getStringStatement(String arg)
	{
		return new Statement()
		{
			private String arg;
			public Statement initialize(String arg)
			{
				this.arg = arg;
				return this;
			}					
			public ScriptVar invoke(ExecutionState state)
			{
				return ScriptVar.toScriptVar(arg);
			}
		}.initialize(arg);		
	}		

	Statement compileStatementList(List<Statement> statements)
	{
		//GG
		return new Statement()
		{
			List<Statement> statements;
			Statement initialize(List<Statement> statements)
			{
				this.statements = statements;
				return this;
			}
			public ScriptVar invoke(ExecutionState state) throws BadTypeException {
				ScriptVar out = null;
				for (Statement statement : statements)
				{
					ScriptVar currentVar = statement.invoke(state);
					if (out == null)
					{
						out = currentVar;
					}
					else
					{
						out = out.apply(currentVar);
					}
				}
				return out;
			}
		}.initialize(statements);
	}
	
	
	
	Statement compileStatementListToList(List<Statement> statements)
	{
		//GGGGGGGGG
		return new Statement()
		{
			List<Statement> statements;
			Statement initialize(List<Statement> statements)
			{
				this.statements = statements;
				return this;
			}
			public ScriptVar invoke(ExecutionState state) throws BadTypeException {
				ScriptVar out = new EmptyList();
				for (Statement statement : statements)
				{
					out = new ListNode(statement.invoke(state), out);
				}
				return out;
			}
		}.initialize(statements);
	}
		
	InvokeFunction createLambdaFunction(List<String> argNames, ExecutionState state, Statement body)
	{
		return new InvokeFunction(genericSpecializer)
		{
			List<String> argNames;
			ExecutionState state;
			Statement body;
			InvokeFunction initialize(List<String> argNames, ExecutionState state, Statement body)
			{
				this.argNames = argNames;
				this.state = state;
				this.body = body;
				return this;
			}
			@Override
			public ScriptVar invoke(List<ScriptVar> values)
					throws BadTypeException {
				ExecutionState lambdaState= new ExecutionState();
				lambdaState.functionArgs = values;  //WHOA DUDE
				lambdaState.locals = new HashMap<String, ScriptVar>(state.locals); //lambda capture!
				int argNum = 0;
				for (ScriptVar var : state.functionArgs)
				{
					//even more lambda capture.  move arguments -> locals.
					lambdaState.locals.put(argNames.get(argNum++), var);
				}
				state.functionArgs = values;
				return body.invoke(lambdaState);
			}

			@Override
			public ScriptVar clone() {
				return this;
			}
			
		}.initialize(argNames, state, body);
	}
	Statement createLambdaFunctionStatement(List<String> argNames, List<String> lambdaArgs, Statement body)
	{
		return new Statement()
		{
			List<String> argNames;
			List<FunctionSpecializer> specializers;
			Statement body;
			
			Statement initialize(List<String> argNames, List<String> lambdaArgs, Statement body)
			{
				this.argNames = argNames;
				this.body = body;
				specializers = new ArrayList<FunctionSpecializer>();
				for (int i = 0; i < lambdaArgs.size(); ++i)
				{
					specializers.add(genericSpecializer);
				}
				specializers.remove(specializers.size()-1);
				return this;
			}
			public ScriptVar invoke(ExecutionState state)
					throws BadTypeException {

				return Script.createFunction(createLambdaFunction(argNames, state, body), specializers);
			}
			
		}.initialize(argNames, lambdaArgs, body);
	}
	
	ParserState getLambdaFunctionBodyState(List<String> argNames, List<String> locals, List<String> lambdaArgs)
	{
		//locals are the same, which is nifty.
		//argNames are added to locals, for bonus effect...
		for (String argName : argNames)
		{
			locals.add(argName); //arguments are inside the closure!!
		}
		//lambda args is the argument names now.
		return new StatementParser(stateStack, lambdaArgs, locals)
		{
			@Override
			public void endParen() throws ParserException
			{
				publishStatement(compileSubstatements());
				popState(); //pop lambda function base state.
			}
		};		
	}
	ParserState getLambdaFunctionState(List<String> argNames, List<String> locals)
	{
		return new ParserState(stateStack)
		{
			List<String> argNames;
			List<String> locals;
			List<String> lambdaArgs;
			{
				lambdaArgs = new ArrayList<String>();
			}
			ParserState initialize(List<String> argNames, List<String> locals)
			{
				this.argNames = argNames;
				this.locals = argNames;
				return this;
			}
			//child statement here is the lambda function!
			public void addChildStatement(Statement statement) throws ParserException
			{
				popState(); //pop to send to parent!
				publishStatement(createLambdaFunctionStatement(argNames, lambdaArgs, statement));
			}
			@Override
			public void endLambdaFunction() throws ParserException
			{
				pushState(getLambdaFunctionBodyState(argNames, locals, lambdaArgs));
			}
			@Override
			public void readName(String name)
			{
				lambdaArgs.add(name);
			}
		}.initialize(argNames, locals);
	}
	ParserState getListParserState(List<String> argNames, List<String> locals)
	{
		return new StatementParser(stateStack, argNames, locals)
		{
			List<Statement> listStatements;
			{
				listStatements = new ArrayList<Statement>(); 
			}
			@Override
			public void listSeparator() 
			{
				listStatements.add(compileSubstatements());				
			}
			@Override
			public void endList() throws ParserException 
			{
				listStatements.add(compileSubstatements());				
				publishStatement(compileStatementListToList(listStatements));
				popState();
			}			
		};
	}

	Statement getPatternMatchStatement(Statement matchingStatement, List<Statement> conditionalStatements, List<Statement> outputStatements)
	{
		return new Statement()
		{
			Statement matchingStatement; List<Statement> conditionalStatements; List<Statement> outputStatements;
			
			Statement initialize(Statement matchingStatement, List<Statement> conditionalStatements, List<Statement> outputStatements)
			{
				this.matchingStatement = matchingStatement; 
				this.conditionalStatements = conditionalStatements; 
				this.outputStatements = outputStatements;
				return this;
			}
			@Override
			public ScriptVar invoke(ExecutionState state) throws BadTypeException {

				ScriptVar var = matchingStatement.invoke(state);
				
				int num = 0;
				for (Statement s : conditionalStatements)
				{
					//function that takes var and returns bool
					if (s.invoke(state).apply(var).getBoolean() == true)
					{
						return outputStatements.get(num).invoke(state);
					}
					++num;
				}
				return null;
			}
		}.initialize(matchingStatement, conditionalStatements, outputStatements);
	}
	
	ParserState getPatternOutputState(List<String> argNames, List<String> locals, Statement matchingStatement, List<Statement> conditionalStatements, List<Statement> outputStatements)
	{
		return new StatementParser(stateStack, argNames, locals)
		{
			Statement matchingStatement;
			List<Statement> conditionalStatements;
			List<Statement> outputStatements;
			
			StatementParser initialize(Statement matchingStatement, List<Statement> conditionalStatements, List<Statement> outputStatements)
			{
				this.matchingStatement = matchingStatement;
				this.conditionalStatements = conditionalStatements;
				this.outputStatements = outputStatements;
				return this;
			}
			
			@Override
			public void patternMatch() throws ParserException 
			{
				outputStatements.add(compileSubstatements());
				popState();
				pushState(getPatternConditionalState(argNames, locals, matchingStatement, conditionalStatements, outputStatements));
			}
			
			@Override
			public void endParen() throws ParserException 
			{
				outputStatements.add(compileSubstatements());
				publishStatement(getPatternMatchStatement(matchingStatement, conditionalStatements, outputStatements));
				popState();
			}			
			
			@Override
			public void endLine() throws ParserException
			{
				//ignore end lines.  It's common form for the pattern match to be on the other side.
			}			
		}.initialize(matchingStatement, conditionalStatements, outputStatements);		
	}
	
	ParserState getPatternConditionalState(List<String> argNames, List<String> locals, Statement matchingStatement, List<Statement> conditionalStatements, List<Statement> outputStatements)
	{
		return new StatementParser(stateStack, argNames, locals)
		{
			Statement matchingStatement;
			List<Statement> conditionalStatements;
			List<Statement> outputStatements;
			
			StatementParser initialize(Statement matchingStatement, List<Statement> conditionalStatements, List<Statement> outputStatements)
			{
				this.matchingStatement = matchingStatement;
				this.conditionalStatements = conditionalStatements;
				this.outputStatements = outputStatements;
				return this;
			}
			
			@Override
			public void caseMarker() throws ParserException 
			{
				conditionalStatements.add(compileSubstatements());
				popState();
				pushState(getPatternOutputState(argNames, locals, matchingStatement, conditionalStatements, outputStatements));
			}
		}.initialize(matchingStatement, conditionalStatements, outputStatements);		
	}
	ParserState getPatternMatchInitState(List<String> argNames, List<String> locals)
	{
		return new StatementParser(stateStack, argNames, locals)
		{
			@Override
			public void patternMatch() throws ParserException 
			{
				Statement matchingStatement = compileSubstatements();
				List<Statement> conditionalStatements =  new ArrayList<Statement>();
				List<Statement> outputStatements =  new ArrayList<Statement>();
				
				popState();
				pushState(getPatternConditionalState(argNames, locals, matchingStatement, conditionalStatements, outputStatements));
			}
			@Override
			public void endLine() throws ParserException
			{
				//ignore end lines.  It's common form for the pattern match to be on the other side.
			}
		};
	}
	ParserState getParenthesisParserState(List<String> argNames, List<String> locals)
	{
		return new StatementParser(stateStack, argNames, locals)
		{
			@Override
			public void endParen() throws ParserException
			{
				publishStatement(compileSubstatements());
				popState();
			}
			@Override
			public void lambdaFunction() throws ParserException 
			{
				if (!substatements.isEmpty())
				{
					throw new ParserException("bad lambda function placement.");
				}
				//secret lambda function block!
				//woot woot hoot hoot
				popState();
				pushState(getLambdaFunctionState(argNames, locals));
			}					
			@Override
			public void patternMatch() throws ParserException 
			{
				if (!substatements.isEmpty())
				{
					throw new ParserException("bad pattern matching placement, requires no previous statements.");
				}
				popState();
				pushState(getPatternMatchInitState(argNames, locals));
			}								
		};
	}
		
	
	private ScriptVar getAddLocalFunction(String localName, ExecutionState state)
	{
		return new ScriptVar()
		{
			private String localName;
			private ExecutionState state;
			
			public ScriptVar initialize(String localName, ExecutionState state)
			{
				this.localName = localName;
				this.state = state;
				return this;
			}
			
			@Override
			public ScriptVar apply(ScriptVar var) throws BadTypeException {
				state.locals.put(localName, var);
				return var;
			}
			@Override
			public boolean isFunction()
			{
				return true;
			}	
			@Override
			public ScriptVar clone() {
				return this;
			}
		
		}.initialize(localName, state);		
	}
	Statement getAddLocalStatement(String localName)
	{
		return new Statement()
		{
			String localName;
			Statement initialize(String localName)
			{
				this.localName = localName;
				return this;
			}
			@Override
			public ScriptVar invoke(ExecutionState state)
			{
				return getAddLocalFunction(localName, state);
			}
		}.initialize(localName);
	}
	ParserState getLocalDefClass(List<String> locals, StatementParser parser)
	{
		return new ParserState(stateStack)
		{
			List<String> locals;
			StatementParser parser;
			String name;
			ParserState initialize(List<String> locals, StatementParser parser)
			{
				this.locals = locals;
				this.parser = parser;
				return this;
			}
			@Override
			public void readName(String name) throws ParserException 
			{
				if (this.name == null)
				{
					this.name = name;
				}
				else
				{
					if (!name.equals("<-"))
					{
						throw new ParserException("Not implemented!");
					}
					
					locals.add(this.name);
					this.parser.setLocalCreateStatement(getAddLocalStatement(this.name));
					popState(); //return back to whatever parser to fill in the function.
				}
			}			
		}.initialize(locals, parser);
	}
	
	private class StatementParser extends ParserState
	{
		List<String> argNames;
		List<String> locals;
		List<Statement> substatements;
		Statement localCreateStatement;
		public StatementParser(List<ParserState> stateStack, List<String> argNames, List<String> locals)
		{
			super(stateStack);
			this.argNames = argNames;
			this.locals = locals;
			this.substatements = new ArrayList<Statement>();
			localCreateStatement = null;
		}
		public void setLocalCreateStatement(Statement localCreateStatement)
		{
			this.localCreateStatement = localCreateStatement;  
		}
		public Statement compileSubstatements()
		{
			Statement out = null;
			if (localCreateStatement == null)
			{
				out = compileStatementList(substatements);
			}
			else
			{
				out = compileStatementList(substatements);
				substatements = new ArrayList<Statement>();
				
				//add the local variable.
				substatements.add(localCreateStatement);
				substatements.add(out);
				out = compileStatementList(substatements);
			}
			return out;
		}
		@Override
		public void addChildStatement(Statement subStatement)
		{
			substatements.add(subStatement);
		}
		@Override
		public void beginList() throws ParserException 
		{
		   pushState(getListParserState(argNames, locals));
		}
		@Override
		public void beginParen() throws ParserException 
		{
			pushState(getParenthesisParserState(argNames, locals));
		}
		@Override
		public void readName(String name) throws ParserException 
		{
			substatements.add(getStatementFromName(name, argNames, locals));			
		}
		@Override
		public void readNumber(int number) throws ParserException
		{
			substatements.add(getIntegerStatement(number));	
		}
		@Override
		public void readString(String string) throws ParserException 
		{
			substatements.add(getStringStatement(string));	
		}
		@Override
		public void readFloat(float number) throws ParserException 
		{
			substatements.add(getFloatStatement(number));
		}
		@Override
		public void readBool(boolean bool) throws ParserException 
		{
			substatements.add(getBoolStatement(bool));
		}
		public void localDef() 
		{
			pushState(getLocalDefClass(locals, this));	
		}
	}
	
	ParserState getStatementState(List<String> argNames, List<String> locals)
	{
		return new StatementParser(stateStack, argNames, locals)
		{
			@Override
			public void endLine() throws ParserException 
			{
				publishStatement(compileSubstatements());
				popState();
			}			
		};
	}	
	
	InvokeFunction makeScriptFunction(String functionName, List<Statement> statements, FunctionSpecializer finalSpecializer)
	{
		return new InvokeFunction(finalSpecializer)
		{
			List<Statement> statements;
			InvokeFunction initialize(List<Statement> statements)
			{
				this.statements = statements;
				return this;
			}
			@Override
			public ScriptVar invoke(List<ScriptVar> arguments) throws BadTypeException {
				//time to run the function!
				ScriptVar last = null;
				ExecutionState state = new ExecutionState();
				state.functionArgs = arguments;
				for (Statement statement : statements)
				{
					last = statement.invoke(state);
				}
				return last;
			}

			@Override
			public ScriptVar clone() {
				return this;
			}
		}.initialize(statements);
	}
	
	ParserState getFunctionDefinitionState(String functionName, List<FunctionSpecializer> specializers, List<String> argNames)
	{
		return new ParserState(stateStack)
		{
			String fnName;
			List<FunctionSpecializer> specializers;
			List<String> argNames;
			List<String> locals;
			List<Statement> statements;
			ParserState initialize(String fnName, List<FunctionSpecializer> specializers, List<String> argNames)
			{
				this.argNames = argNames;
				this.specializers = specializers;
				this.fnName = fnName;
				this.locals = new ArrayList<String>();
				this.statements = new ArrayList<Statement>();
				return this;
			}	
			void moveToStatementState()
			{
				pushState(getStatementState(argNames, locals));
			}
			void moveToListState()
			{
				pushState(getListParserState(argNames, locals));
			}
			void moveToParenthesisState()
			{
				pushState(getParenthesisParserState(argNames, locals));
			}
			@Override
			public void addChildStatement(Statement statement) 
			{
				statements.add(statement);
			} 
			public void beginList() {moveToListState();}
			public void beginParen() throws ParserException {moveToParenthesisState();}
			public void readName(String name) throws ParserException
			{
				moveToStatementState();
				stateStack.get(stateStack.size()-1).readName(name);
			}
			public void readNumber(int number)  throws ParserException
			{
				moveToStatementState();
				stateStack.get(stateStack.size()-1).readNumber(number);
			}
			public void readString(String string)  throws ParserException
			{
				moveToStatementState();
				stateStack.get(stateStack.size()-1).readString(string);	
			}
			public void readFloat(float number)  throws ParserException
			{
				moveToStatementState();
				stateStack.get(stateStack.size()-1).readFloat(number);	
			}
			public void readBool(boolean bool)   throws ParserException
			{
				moveToStatementState();
				stateStack.get(stateStack.size()-1).readBool(bool);
			}
			
			
			public void endLine() throws BadTypeException, BadSpecialization 
			{
				FunctionSpecializer lastSpecializer = specializers.get(specializers.size()-1);
				specializers.remove(specializers.size()-1);				
				script.populateFunction(fnName, specializers);
				InvokeFunction function = makeScriptFunction(fnName, statements, lastSpecializer);
				script.addInvokeFunction(fnName, function, specializers);
				popState();
			}
			public void localDef() throws ParserException
			{
				moveToStatementState();
				stateStack.get(stateStack.size()-1).localDef();				
			}			
		}.initialize(functionName, specializers, argNames);
	}
	Tokenizer tokenizer;
	
	public static abstract class TypeSpecializer implements FunctionSpecializer
	{

		@Override
		public boolean Equals(FunctionSpecializer rhs) {
			return this.getClass() == rhs.getClass(); //reflection fun times!
		}

		public abstract boolean specializes(ScriptVar var);
		@Override
		public SpecializationLevel getSpecializationLevelFor(ScriptVar var) {
			if (specializes(var))
			{
				return SpecializationLevel.TypeSpecialized;
			}
			return SpecializationLevel.NotSpecialized;
		}
		
	}
	
	public static TypeSpecializer getIntSpecializer()
	{
		return new TypeSpecializer(){public boolean specializes(ScriptVar var) {return var.isInteger();}};
	}
	
	public static TypeSpecializer getFloatSpecializer()
	{
		return new TypeSpecializer(){public boolean specializes(ScriptVar var) {return var.isFloat();}};
	}	
	
	public static TypeSpecializer getStringSpecializer()
	{
		return new TypeSpecializer(){public boolean specializes(ScriptVar var) {return var.isString();}};
	}
	
	public static TypeSpecializer getBoolSpecializer()
	{
		return new TypeSpecializer(){public boolean specializes(ScriptVar var) {return var.isBoolean();}};
	}		
	
	public static TypeSpecializer getListSpecializer()
	{
		return new TypeSpecializer(){public boolean specializes(ScriptVar var) {return var.isList();}};
	}		
	
	public static TypeSpecializer getOpaqueSpecializer()
	{
		return new TypeSpecializer(){public boolean specializes(ScriptVar var) {return var.isOpaque();}};
	}			
	
	public static TypeSpecializer getFunctionSpecializer()
	{
		return new TypeSpecializer(){public boolean specializes(ScriptVar var) {return var.isFunction();}};
	}				
		
	
	public static FunctionSpecializer getGenericSpecializer()
	{
		return new FunctionSpecializer()
		{

			@Override
			public boolean Equals(FunctionSpecializer rhs) {
				return rhs.getClass() == this.getClass();
			}

			@Override
			public SpecializationLevel getSpecializationLevelFor(ScriptVar var) {
				return SpecializationLevel.Generic;
			}
		};
	}

	private void populateTypeSpecializers()
	{
		typeSpecializers.put("int", getIntSpecializer());
		typeSpecializers.put("float", getFloatSpecializer());
		typeSpecializers.put("string", getStringSpecializer());
		typeSpecializers.put("bool", getBoolSpecializer());
		typeSpecializers.put("list", getListSpecializer());
		typeSpecializers.put("opaque", getOpaqueSpecializer());
		typeSpecializers.put("func", getFunctionSpecializer());		
	}
	
	public Parser(Tokenizer tokenizer, Script script)
	{
		this.script = script;
		typeSpecializers = new HashMap<String, FunctionSpecializer>();
		populateTypeSpecializers();
		genericSpecializer = getGenericSpecializer();
		stateStack = new ArrayList<ParserState>();
		this.tokenizer = tokenizer;
	}
	
	public void run()
	{
		stateStack.add(getFunctionLevelParserState());
		try
		{
			
			for(ScriptToken token = tokenizer.getNextToken();;token = tokenizer.getNextToken())
			{	
				ParserState state = stateStack.get(stateStack.size()-1);
				switch (token.getType())
				{
				case BeginList:
					state.beginList();
					break;
				case BeginParen:
					state.beginParen();
					break;
				case Boolean:
					state.readBool(token.getBoolean());
					break;
				case EndFile:
					state.endLine();
					state = stateStack.get(stateStack.size()-1);
					state.endLine();
					break;
				case EndLine:
					state.endLine();
					break;
				case EndList:
					state.endList();
					break;
				case EndParen:
					state.endParen();
					break;
				case Float:
					state.readFloat(token.getFloat());
					break;
				case Name:
					state.readName(token.getName());
					break;
				case Number:
					state.readNumber(token.getNumber());
					break;
				case String:
					state.readString(token.getString());
					break;
				case LocalDef:
					state.localDef();
					break;
				case ListSeparator:
					state.listSeparator();
					break;
				case beginLambdaFunction:
					state.lambdaFunction();
					break;
				case endLambdaFunction:
					state.endLambdaFunction();
					break;	
				case patternMatch:
					state.patternMatch();
					break;
				case caseMarker:
					state.caseMarker();
					break;
				default:
					break; 
				}
				if (token.getType() == ScriptToken.Type.EndFile) break; 
			}		
		}	
		catch(ParserException e)
		{
			//YOU DUN GOOFED
		}	
	}
	
}
