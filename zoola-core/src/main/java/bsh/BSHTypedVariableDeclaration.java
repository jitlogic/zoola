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

class BSHTypedVariableDeclaration extends SimpleNode
{
	public Modifiers modifiers;
	
    BSHTypedVariableDeclaration(int id) { super(id); }

	private BSHType getTypeNode() {
		return ((BSHType)jjtGetChild(0));
	}

	Class evalType( CallStack callstack, Interpreter interpreter )
		throws EvalError
	{
		BSHType typeNode = getTypeNode();
		return typeNode.getType( callstack, interpreter );
	}

	BSHVariableDeclarator [] getDeclarators() 
	{
		int n = jjtGetNumChildren();
		int start=1;
		BSHVariableDeclarator [] bvda = new BSHVariableDeclarator[ n-start ];
		for (int i = start; i < n; i++)
		{
			bvda[i-start] = (BSHVariableDeclarator)jjtGetChild(i);
		}
		return bvda;
	}

	/**
		evaluate the type and one or more variable declarators, e.g.:
			int a, b=5, c;
	*/
    public Object eval( CallStack callstack, Interpreter interpreter)  
		throws EvalError
    {
		try {
			NameSpace namespace = callstack.top();
			BSHType typeNode = getTypeNode();
			Class type = typeNode.getType( callstack, interpreter );

			BSHVariableDeclarator [] bvda = getDeclarators();
			for (int i = 0; i < bvda.length; i++)
			{
				BSHVariableDeclarator dec = bvda[i];

				// Type node is passed down the chain for array initializers
				// which need it under some circumstances
				Object value = dec.eval( typeNode, callstack, interpreter);

				try {
					namespace.setTypedVariable( 
						dec.name, type, value, modifiers );
				} catch ( UtilEvalError e ) { 
					throw e.toEvalError( this, callstack ); 
				}
			}
		} catch ( EvalError e ) {
			e.reThrow( "Typed variable declaration" );
		}

        return Primitive.VOID;
    }

	public String getTypeDescriptor( 
		CallStack callstack, Interpreter interpreter, String defaultPackage ) 
	{ 
		return getTypeNode().getTypeDescriptor( 
			callstack, interpreter, defaultPackage );
	}
}
