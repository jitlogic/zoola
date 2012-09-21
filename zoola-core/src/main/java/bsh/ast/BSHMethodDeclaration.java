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

public class BSHMethodDeclaration extends SimpleNode
{
	public String name;

	// Begin Child node structure evaluated by insureNodesParsed

	public BSHReturnType returnTypeNode;
	public BSHFormalParameters paramsNode;
	public BSHBlock blockNode;
	// index of the first throws clause child node
	public int firstThrowsClause;

	// End Child node structure evaluated by insureNodesParsed

	public Modifiers modifiers;

	// Unsafe caching of type here.
	public Class returnType;  // null (none), Void.TYPE, or a Class
	public int numThrows = 0;

	public BSHMethodDeclaration(int id) { super(id); }

	/**
		Set the returnTypeNode, paramsNode, and blockNode based on child
		node structure.  No evaluation is done here.
	*/
	public synchronized void insureNodesParsed()
	{
		if ( paramsNode != null ) // there is always a paramsNode
			return;

		Object firstNode = jjtGetChild(0);
		firstThrowsClause = 1;
		if ( firstNode instanceof BSHReturnType )
		{
			returnTypeNode = (BSHReturnType)firstNode;
			paramsNode = (BSHFormalParameters)jjtGetChild(1);
			if ( jjtGetNumChildren() > 2+numThrows )
				blockNode = (BSHBlock)jjtGetChild(2+numThrows); // skip throws
			++firstThrowsClause;
		}
		else
		{
			paramsNode = (BSHFormalParameters)jjtGetChild(0);
			blockNode = (BSHBlock)jjtGetChild(1+numThrows); // skip throws
		}
	}

	/**
		Evaluate the return type node.
		@return the type or null indicating loosely typed return
	*/
	Class evalReturnType( CallStack callstack, Interpreter interpreter )
		throws EvalError
	{
		insureNodesParsed();
		if ( returnTypeNode != null )
			return returnTypeNode.evalReturnType( callstack, interpreter );
		else 
			return null;
	}

	public String getReturnTypeDescriptor(
		CallStack callstack, Interpreter interpreter, String defaultPackage )
	{
		insureNodesParsed();
		if ( returnTypeNode == null )
			return null;
		else
			return returnTypeNode.getTypeDescriptor( 
				callstack, interpreter, defaultPackage );
	}

	public BSHReturnType getReturnTypeNode() {
		insureNodesParsed();
		return returnTypeNode;
	}

	/**
		Evaluate the declaration of the method.  That is, determine the
		structure of the method and install it into the caller's namespace.
	*/
	public Object eval( CallStack callstack, Interpreter interpreter )
		throws EvalError
	{
		returnType = evalReturnType( callstack, interpreter );
		evalNodes( callstack, interpreter );

		// Install an *instance* of this method in the namespace.
		// See notes in BshMethod 

// This is not good...
// need a way to update eval without re-installing...
// so that we can re-eval params, etc. when classloader changes
// look into this

		NameSpace namespace = callstack.top();
		BshMethod bshMethod = new BshMethod( this, namespace, modifiers );
		try {
			namespace.setMethod( bshMethod );
		} catch ( UtilEvalError e ) {
			throw e.toEvalError(this,callstack);
		}

		return Primitive.VOID;
	}

	private void evalNodes( CallStack callstack, Interpreter interpreter ) 
		throws EvalError
	{
		insureNodesParsed();
		
		// validate that the throws names are class names
		for(int i=firstThrowsClause; i<numThrows+firstThrowsClause; i++)
			((BSHAmbiguousName)jjtGetChild(i)).toClass(
				callstack, interpreter );

		paramsNode.eval( callstack, interpreter );

		// if strictJava mode, check for loose parameters and return type
		if ( interpreter.getStrictJava() )
		{
			for(int i=0; i<paramsNode.paramTypes.length; i++)
				if ( paramsNode.paramTypes[i] == null )
					// Warning: Null callstack here.  Don't think we need
					// a stack trace to indicate how we sourced the method.
					throw new EvalError(
				"(Strict Java Mode) Undeclared argument type, parameter: " +
					paramsNode.getParamNames()[i] + " in method: " 
					+ name, this, null );

			if ( returnType == null )
				// Warning: Null callstack here.  Don't think we need
				// a stack trace to indicate how we sourced the method.
				throw new EvalError(
				"(Strict Java Mode) Undeclared return type for method: "
					+ name, this, null );
		}
	}

	public String toString() {
		return "MethodDeclaration: "+name;
	}
}
