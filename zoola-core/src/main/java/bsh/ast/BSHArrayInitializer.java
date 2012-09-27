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

import java.lang.reflect.Array;

public class BSHArrayInitializer extends SimpleNode
{
    public BSHArrayInitializer(int id) { super(id); }

	/**
		Construct the array from the initializer syntax.

		@param baseType the base class type of the array (no dimensionality)
		@param dimensions the top number of dimensions of the array 
			e.g. 2 for a String [][];
	*/
    public Object eval( Class baseType, int dimensions, 
						CallStack callstack, Interpreter interpreter ) 
		throws EvalError
    {
        int numInitializers = jjtGetNumChildren();

		// allocate the array to store the initializers
		int [] dima = new int [dimensions]; // description of the array
		// The other dimensions default to zero and are assigned when 
		// the values are set.
		dima[0] = numInitializers;
        Object initializers =  Array.newInstance( baseType, dima );

		// Evaluate the initializers
        for (int i = 0; i < numInitializers; i++)
        {
			SimpleNode node = (SimpleNode)jjtGetChild(i);
            Object currentInitializer;
			if ( node instanceof BSHArrayInitializer ) {
				if ( dimensions < 2 )
					throw new EvalError(
						"Invalid Location for Intializer, position: "+i, 
						this, callstack );
            	currentInitializer = 
					((BSHArrayInitializer)node).eval( 
						baseType, dimensions-1, callstack, interpreter);
			} else
            	currentInitializer = node.accept(new BshEvaluatingVisitor(callstack, interpreter)); // TODO

			if ( currentInitializer == Primitive.VOID )
				throw new EvalError(
					"Void in array initializer, position"+i, this, callstack );

			// Determine if any conversion is necessary on the initializers.
			//
			// Quick test to see if conversions apply:
			// If the dimensionality of the array is 1 then the elements of
			// the initializer can be primitives or boxable types.  If it is
			// greater then the values must be array (object) types and there
			// are currently no conversions that we do on those.
			// If we have conversions on those in the future then we need to
			// get the real base type here instead of the dimensionless one.
			Object value = currentInitializer;
			if ( dimensions == 1 )
			{
				// We do a bsh cast here.  strictJava should be able to affect
				// the cast there when we tighten control
				try {
					value = Types.castObject(
						currentInitializer, baseType, Types.CAST );
				} catch ( UtilEvalError e ) {
					throw e.toEvalError(
						"Error in array initializer", this, callstack );
				}
				// unwrap any primitive, map voids to null, etc.
				value = Primitive.unwrap( value );
			}

				// store the value in the array
            try {
				Array.set(initializers, i, value);
            } catch( IllegalArgumentException e ) {
				Interpreter.debug("illegal arg"+e);
				throwTypeError( baseType, currentInitializer, i, callstack );
            } catch( ArrayStoreException e ) { // I think this can happen
				Interpreter.debug("arraystore"+e);
				throwTypeError( baseType, currentInitializer, i, callstack );
            }
        }

        return initializers;
    }

	private void throwTypeError( 
		Class baseType, Object initializer, int argNum, CallStack callstack ) 
		throws EvalError
	{
		String rhsType;
		if (initializer instanceof Primitive)
			rhsType = 
				((Primitive)initializer).getType().getName();
		else
			rhsType = Reflect.normalizeClassName(
				initializer.getClass());

		throw new EvalError ( "Incompatible type: " + rhsType 
			+" in initializer of array type: "+ baseType
			+" at position: "+argNum, this, callstack );
	}

    public <T> T accept(BshNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

}
