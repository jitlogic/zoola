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
import bsh.interpreter.BshEvaluatingVisitor;

public class BSHArguments extends SimpleNode
{
    public BSHArguments(int id) { super(id); }

	/**
		This node holds a set of arguments for a method invocation or
		constructor call.

		Note: arguments are not currently allowed to be VOID.
	*/
	/*
		Disallowing VOIDs here was an easy way to support the throwing of a 
		more descriptive error message on use of an undefined argument to a 
		method call (very common).  If it ever turns out that we need to 
		support that for some reason we'll have to re-evaluate how we get 
		"meta-information" about the arguments in the various invoke() methods 
		that take Object [].  We could either pass BSHArguments down to 
		overloaded forms of the methods or throw an exception subtype 
		including the argument position back up, where the error message would
		be compounded.
	*/
    public Object[] getArguments( BshEvaluatingVisitor visitor)
		throws EvalError
    {
        // evaluate each child
        Object[] args = new Object[jjtGetNumChildren()];
        for(int i = 0; i < args.length; i++)
		{
            args[i] = ((SimpleNode)jjtGetChild(i)).accept(visitor);
			if ( args[i] == Primitive.VOID )
				throw new EvalError( "Undefined argument: " + 
					((SimpleNode)jjtGetChild(i)).getText(), this, visitor.getCallstack() );
		}

        return args;
    }

    public <T> T accept(BshNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

}

