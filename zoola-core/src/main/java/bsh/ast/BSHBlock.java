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

public class BSHBlock extends SimpleNode
{
	public boolean isSynchronized = false;

	public BSHBlock(int id) { super(id); }

	public Object eval( CallStack callstack, Interpreter interpreter)
		throws EvalError
	{
        return this.accept(new BshEvaluatingVisitor(callstack, interpreter));
    }

	/**
		@param overrideNamespace if set to true the block will be executed
		in the current namespace (not a subordinate one).
		<p>
		If true *no* new BlockNamespace will be swapped onto the stack and 
		the eval will happen in the current
		top namespace.  This is used by BshMethod, TryStatement, etc.  
		which must intialize the block first and also for those that perform 
		multiple passes in the same block.
	*/
	public Object eval( 
		CallStack callstack, Interpreter interpreter, 
		boolean overrideNamespace ) 
		throws EvalError
	{
		Object syncValue = null;
		if ( isSynchronized ) 
		{
			// First node is the expression on which to sync
			SimpleNode exp = ((SimpleNode)jjtGetChild(0));
			syncValue = exp.eval(callstack, interpreter);
		}

		Object ret;
		if ( isSynchronized ) // Do the actual synchronization
			synchronized( syncValue )
			{
				ret = evalBlock( 
					callstack, interpreter, overrideNamespace, null/*filter*/);
			}
		else
				ret = evalBlock( 
					callstack, interpreter, overrideNamespace, null/*filter*/ );

		return ret;
	}

	public Object evalBlock(
		CallStack callstack, Interpreter interpreter, 
		boolean overrideNamespace, NodeFilter nodeFilter ) 
		throws EvalError
	{	
		Object ret = Primitive.VOID;
		NameSpace enclosingNameSpace = null;
		if ( !overrideNamespace ) 
		{
			enclosingNameSpace= callstack.top();
			BlockNameSpace bodyNameSpace = 
				new BlockNameSpace( enclosingNameSpace );

			callstack.swap( bodyNameSpace );
		}

		int startChild = isSynchronized ? 1 : 0;
		int numChildren = jjtGetNumChildren();

		try {
			/*
				Evaluate block in two passes: 
				First do class declarations then do everything else.
			*/
			for(int i=startChild; i<numChildren; i++)
			{
				SimpleNode node = ((SimpleNode)jjtGetChild(i));

				if ( nodeFilter != null && !nodeFilter.isVisible( node ) )
					continue;

				if ( node instanceof BSHClassDeclaration )
					node.eval( callstack, interpreter );
			}
			for(int i=startChild; i<numChildren; i++)
			{
				SimpleNode node = ((SimpleNode)jjtGetChild(i));
				if ( node instanceof BSHClassDeclaration )
					continue;

				// filter nodes
				if ( nodeFilter != null && !nodeFilter.isVisible( node ) )
					continue;

				ret = node.eval( callstack, interpreter );

				// statement or embedded block evaluated a return statement
				if ( ret instanceof ReturnControl )
					break;
			}
		} finally {
			// make sure we put the namespace back when we leave.
			if ( !overrideNamespace ) 
				callstack.swap( enclosingNameSpace );
		}
		return ret;
	}

	public interface NodeFilter {
		public boolean isVisible( SimpleNode node );
	}

    public <T> T accept(BshNodeVisitor<T> visitor) {
        return visitor.visit(this);
    }

}

