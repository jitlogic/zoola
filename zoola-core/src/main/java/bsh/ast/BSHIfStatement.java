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

public class BSHIfStatement extends SimpleNode
{
    public BSHIfStatement(int id) { super(id); }

    public static boolean evaluateCondition(
		SimpleNode condExp, CallStack callstack, Interpreter interpreter) 
		throws EvalError
    {
        Object obj = condExp.accept(new BshEvaluatingVisitor(callstack, interpreter));
        if(obj instanceof Primitive) {
			if ( obj == Primitive.VOID )
				throw new EvalError("Condition evaluates to void type", 
					condExp, callstack );
            obj = ((Primitive)obj).getValue();
		}

        if(obj instanceof Boolean)
            return ((Boolean)obj).booleanValue();
        else
            throw new EvalError(
				"Condition must evaluate to a Boolean or boolean.", 
				condExp, callstack );
    }

    public <T> T accept(BshNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
