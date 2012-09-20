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

public class BSHAssignment extends SimpleNode implements ParserConstants
{
    public int operator;

    public BSHAssignment(int id) { super(id); }

    public Object eval(
		CallStack callstack, Interpreter interpreter)
		throws EvalError
    {
        BSHPrimaryExpression lhsNode = 
			(BSHPrimaryExpression)jjtGetChild(0);

		if ( lhsNode == null )
			throw new InterpreterError( "Error, null LHSnode" );

		boolean strictJava = interpreter.getStrictJava();
        LHS lhs = lhsNode.toLHS( callstack, interpreter);
        if ( lhs == null )
            throw new InterpreterError( "Error, null LHS" );

		// For operator-assign operations save the lhs value before evaluating
		// the rhs.  This is correct Java behavior for postfix operations
		// e.g. i=1; i+=i++; // should be 2 not 3
		Object lhsValue = null;
		if ( operator != ASSIGN ) // assign doesn't need the pre-value
			try {
				lhsValue = lhs.getValue();
			} catch ( UtilEvalError e ) {
				throw e.toEvalError( this, callstack );
			}

        SimpleNode rhsNode = (SimpleNode)jjtGetChild(1);

        Object rhs;
		
		// implement "blocks" foo = { };
		// if ( rhsNode instanceof BSHBlock )
		//    rsh =
		// else
        rhs = rhsNode.eval(callstack, interpreter);

        if ( rhs == Primitive.VOID )
            throw new EvalError("Void assignment.", this, callstack );

		try {
			switch(operator)
			{
				case ASSIGN:
					return lhs.assign( rhs, strictJava );

				case PLUSASSIGN:
					return lhs.assign( 
						operation(lhsValue, rhs, PLUS), strictJava );

	            case MINUSASSIGN:
					return lhs.assign( 
						operation(lhsValue, rhs, MINUS), strictJava );

				case STARASSIGN:
					return lhs.assign( 
						operation(lhsValue, rhs, STAR), strictJava );

	            case SLASHASSIGN:
					return lhs.assign( 
						operation(lhsValue, rhs, SLASH), strictJava );

	            case ANDASSIGN:
				case ANDASSIGNX:
					return lhs.assign( 
						operation(lhsValue, rhs, BIT_AND), strictJava );

	            case ORASSIGN:
	            case ORASSIGNX:
	                return lhs.assign( 
						operation(lhsValue, rhs, BIT_OR), strictJava );

	            case XORASSIGN:
	                return lhs.assign( 
						operation(lhsValue, rhs, XOR), strictJava );

	            case MODASSIGN:
	                return lhs.assign( 
						operation(lhsValue, rhs, MOD), strictJava );

	            case LSHIFTASSIGN:
	            case LSHIFTASSIGNX:
	                return lhs.assign( 
						operation(lhsValue, rhs, LSHIFT), strictJava );

	            case RSIGNEDSHIFTASSIGN:
	            case RSIGNEDSHIFTASSIGNX:
	                return lhs.assign( 
					operation(lhsValue, rhs, RSIGNEDSHIFT ), strictJava );

	            case RUNSIGNEDSHIFTASSIGN:
	            case RUNSIGNEDSHIFTASSIGNX:
	                return lhs.assign( 
						operation(lhsValue, rhs, RUNSIGNEDSHIFT), 
						strictJava );

				default:
					throw new InterpreterError(
						"unimplemented operator in assignment BSH");
			}
		} catch ( UtilEvalError e ) {
			throw e.toEvalError( this, callstack );
		}
    }

    private Object operation( Object lhs, Object rhs, int kind ) 
		throws UtilEvalError
    {
		/*
			Implement String += value;
			According to the JLS, value may be anything.
			In BeanShell, we'll disallow VOID (undefined) values.
			(or should we map them to the empty string?)
		*/
		if ( lhs instanceof String && rhs != Primitive.VOID ) {
			if ( kind != PLUS )
				throw new UtilEvalError(
					"Use of non + operator with String LHS" );

			return (String)lhs + rhs;
		}

        if ( lhs instanceof Primitive || rhs instanceof Primitive )
            if(lhs == Primitive.VOID || rhs == Primitive.VOID)
                throw new UtilEvalError(
					"Illegal use of undefined object or 'void' literal" );
            else if ( lhs == Primitive.NULL || rhs == Primitive.NULL )
                throw new UtilEvalError(
					"Illegal use of null object or 'null' literal" );


        if( (lhs instanceof Boolean || lhs instanceof Character ||
             lhs instanceof Number || lhs instanceof Primitive) &&
            (rhs instanceof Boolean || rhs instanceof Character ||
             rhs instanceof Number || rhs instanceof Primitive) )
        {
            return Primitive.binaryOperation(lhs, rhs, kind);
        }

        throw new UtilEvalError("Non primitive value in operator: " +
            lhs.getClass() + " " + tokenImage[kind] + " " + rhs.getClass() );
    }
}
