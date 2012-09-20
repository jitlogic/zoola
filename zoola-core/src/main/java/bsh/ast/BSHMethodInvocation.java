/**
 *
 * This file is a part of ZOOLA - an extensible BeanShell implementation.
 * Zoola is based on original BeanShell code created by Pat Niemeyer.
 *
 * Original BeanShell code is Copyright (C) 2000 Pat Niemeyer <pat@pat.net>.
 *
 * New portions are Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 *
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ZOOLA. If not, see <http://www.gnu.org/licenses/>.
 *
 */


package bsh.ast;

import bsh.*;
import bsh.ast.BSHAmbiguousName;
import bsh.ast.BSHArguments;

import java.lang.reflect.InvocationTargetException;

public class BSHMethodInvocation extends SimpleNode
{
	public BSHMethodInvocation (int id) { super(id); }

	public BSHAmbiguousName getNameNode() {
		return (BSHAmbiguousName)jjtGetChild(0);
	}

	public BSHArguments getArgsNode() {
		return (BSHArguments)jjtGetChild(1);
	}

	/**
		Evaluate the method invocation with the specified callstack and 
		interpreter
	*/
	public Object eval( CallStack callstack, Interpreter interpreter )
		throws EvalError
	{
		NameSpace namespace = callstack.top();
		BSHAmbiguousName nameNode = getNameNode();

		// Do not evaluate methods this() or super() in class instance space
		// (i.e. inside a constructor)
		if ( namespace.getParent() != null && namespace.getParent().isClass
			&& ( nameNode.text.equals("super") || nameNode.text.equals("this") )
		)
			return Primitive.VOID;
 
		Name name = nameNode.getName(namespace);
		Object[] args = getArgsNode().getArguments(callstack, interpreter);

// This try/catch block is replicated is BSHPrimarySuffix... need to
// factor out common functionality...
// Move to Reflect?
		try {
			return name.invokeMethod( interpreter, args, callstack, this);
		} catch ( ReflectError e ) {
			throw new EvalError(
				"Error in method invocation: " + e.getMessage(), 
				this, callstack, e );
		} catch ( InvocationTargetException e ) 
		{
			String msg = "Method Invocation "+name;
			Throwable te = e.getTargetException();

			/*
				Try to squeltch the native code stack trace if the exception
				was caused by a reflective call back into the bsh interpreter
				(e.g. eval() or source()
			*/
			boolean isNative = true;
			if ( te instanceof EvalError ) 
				if ( te instanceof TargetError )
					isNative = ((TargetError)te).inNativeCode();
				else
					isNative = false;
			
			throw new TargetError( msg, te, this, callstack, isNative );
		} catch ( UtilEvalError e ) {
			throw e.toEvalError( this, callstack );
		}
	}
}

