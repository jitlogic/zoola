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

public class BSHAmbiguousName extends SimpleNode
{
    public String text;

    public BSHAmbiguousName(int id) { super(id); }
	
    public Name getName( NameSpace namespace )
    {
        return namespace.getNameResolver( text );
    }

    public Object toObject( CallStack callstack, Interpreter interpreter )
		throws EvalError
    {
		return toObject( callstack, interpreter, false );
    }

    public Object toObject(
		CallStack callstack, Interpreter interpreter, boolean forceClass ) 
		throws EvalError
    {
		try {
        	return 
				getName( callstack.top() ).toObject( 
					callstack, interpreter, forceClass );
		} catch ( UtilEvalError e ) {
			throw e.toEvalError( this, callstack );
		}
    }

    public Class toClass( CallStack callstack, Interpreter interpreter ) 
		throws EvalError
    {
		try {
        	return getName( callstack.top() ).toClass();
		} catch ( ClassNotFoundException e ) {
			throw new EvalError( e.getMessage(), this, callstack, e );
		} catch ( UtilEvalError e2 ) {
			// ClassPathException is a type of UtilEvalError
			throw e2.toEvalError( this, callstack );
		}
    }

    public LHS toLHS( CallStack callstack, Interpreter interpreter)
		throws EvalError
    {
		try {
			return getName( callstack.top() ).toLHS( callstack, interpreter );
		} catch ( UtilEvalError e ) {
			throw e.toEvalError( this, callstack );
		}
    }

	/*
		The interpretation of an ambiguous name is context sensitive.
		We disallow a generic eval( ).
	*/
    public Object eval( CallStack callstack, Interpreter interpreter ) 
		throws EvalError
    {
		throw new InterpreterError( 
			"Don't know how to eval an ambiguous name!"
			+"  Use toObject() if you want an object." );
    }

	public String toString() {
		return "AmbigousName: "+text;
	}
}

