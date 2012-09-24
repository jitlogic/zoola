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

/**
 * This class handles both {@code while} statements and {@code do..while} statements.
*/
class BSHWhileStatement extends SimpleNode implements ParserConstants {

	/**
	 * Set by Parser, default {@code false}
	 */
	boolean isDoStatement;

    BSHWhileStatement(int id) {
		super(id);
	}


    public Object eval( CallStack callstack, Interpreter interpreter) throws EvalError {
		int numChild = jjtGetNumChildren();

		// Order of body and condition is swapped for do / while
        final SimpleNode condExp;
		final SimpleNode body;

		if ( isDoStatement ) {
			condExp = (SimpleNode) jjtGetChild(1);
			body = (SimpleNode) jjtGetChild(0);
		} else {
			condExp = (SimpleNode) jjtGetChild(0);
			if ( numChild > 1 )	{
				body = (SimpleNode) jjtGetChild(1);
			} else {
				body = null;
			}
		}

		boolean doOnceFlag = isDoStatement;

        while (doOnceFlag || BSHIfStatement.evaluateCondition(condExp, callstack, interpreter)) {
			doOnceFlag = false;
			// no body?
			if ( body == null ) {
				continue;
			}
			Object ret = body.eval(callstack, interpreter);
			if (ret instanceof ReturnControl) {
				switch(( (ReturnControl)ret).kind ) {
					case RETURN:
						return ret;

					case CONTINUE:
						break;

					case BREAK:
						return Primitive.VOID;
				}
			}
		}
        return Primitive.VOID;
    }

}
