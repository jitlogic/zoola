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


package bsh;

class BSHThrowStatement extends SimpleNode
{
	BSHThrowStatement(int id) { super(id); }

	public Object eval( CallStack callstack, Interpreter interpreter)  
		throws EvalError
	{
		Object obj = ((SimpleNode)jjtGetChild(0)).eval(callstack, interpreter);

		// need to loosen this to any throwable... do we need to handle
		// that in interpreter somewhere?  check first...
		if(!(obj instanceof Exception))
			throw new EvalError("Expression in 'throw' must be Exception type",
				this, callstack );

		// wrap the exception in a TargetException to propogate it up
		throw new TargetError( (Exception)obj, this, callstack );
	}
}

