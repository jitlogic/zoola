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

import java.util.List;
import java.util.ArrayList;


class BSHTryStatement extends SimpleNode
{
	BSHTryStatement(int id)
	{
		super(id);
	}

	public Object eval( CallStack callstack, Interpreter interpreter)  
		throws EvalError
	{
		BSHBlock tryBlock = ((BSHBlock)jjtGetChild(0));

		List<BSHFormalParameter> catchParams = new ArrayList<BSHFormalParameter>();
		List<BSHBlock> catchBlocks = new ArrayList<BSHBlock>();

		int nchild = jjtGetNumChildren();
		Node node = null;
		int i=1;
		while((i < nchild) && ((node = jjtGetChild(i++)) instanceof BSHFormalParameter))
		{
			catchParams.add((BSHFormalParameter)node);
			catchBlocks.add((BSHBlock)jjtGetChild(i++));
			node = null;
		}
		// finaly block
		BSHBlock finallyBlock = null;
		if(node != null)
			finallyBlock = (BSHBlock)node;

// Why both of these?

		TargetError target = null;
		Throwable thrown = null;
		Object ret = null;

		/*
			Evaluate the contents of the try { } block and catch any resulting
			TargetErrors generated by the script.
			We save the callstack depth and if an exception is thrown we pop
			back to that depth before contiuing.  The exception short circuited
			any intervening method context pops.

			Note: we the stack info... what do we do with it?  append
			to exception message?
		*/
		int callstackDepth = callstack.depth();
		try {
			ret = tryBlock.eval(callstack, interpreter);
		}
		catch( TargetError e ) {
			target = e;
			String stackInfo = "Bsh Stack: ";
			while ( callstack.depth() > callstackDepth )
				stackInfo += "\t" + callstack.pop() +"\n";
		}

		// unwrap the target error
		if ( target != null )
			thrown = target.getTarget();

		
		// If we have an exception, find a catch
		if (thrown != null) 
		{
			int n = catchParams.size();
			for(i=0; i<n; i++)
			{
				// Get catch block
				BSHFormalParameter fp = catchParams.get(i);

				// Should cache this subject to classloader change message
				// Evaluation of the formal parameter simply resolves its
				// type via the specified namespace.. it doesn't modify the
				// namespace.
				fp.eval( callstack, interpreter );

				if ( fp.type == null && interpreter.getStrictJava() )
					throw new EvalError(
						"(Strict Java) Untyped catch block", this, callstack );

				// If the param is typed check assignability
				if ( fp.type != null ) 
					try {
						thrown = (Throwable)Types.castObject(
							thrown/*rsh*/, fp.type/*lhsType*/, Types.ASSIGNMENT );
					} catch( UtilEvalError e ) {
						/*
							Catch the mismatch and continue to try the next
							Note: this is innefficient, should have an
							isAssignableFrom() that doesn't throw
							// TODO: we do now have a way to test assignment
							// 	in castObject(), use it?
						*/
						continue;
					}

				// Found match, execute catch block
				BSHBlock cb = catchBlocks.get(i);

				// Prepare to execute the block.
				// We must create a new BlockNameSpace to hold the catch
				// parameter and swap it on the stack after initializing it.

				NameSpace enclosingNameSpace = callstack.top();
				BlockNameSpace cbNameSpace = 
					new BlockNameSpace( enclosingNameSpace );

				try {
					if ( fp.type == BSHFormalParameter.UNTYPED )
						// set an untyped variable directly in the block
						cbNameSpace.setBlockVariable( fp.name, thrown );
					else
					{
						// set a typed variable (directly in the block)
						Modifiers modifiers = new Modifiers();
						cbNameSpace.setTypedVariable(
							fp.name, fp.type, thrown, new Modifiers()/*none*/ );
					}
				} catch ( UtilEvalError e ) {
					throw new InterpreterError(
						"Unable to set var in catch block namespace." );
				}

				// put cbNameSpace on the top of the stack
				callstack.swap( cbNameSpace );
				try {
					ret = cb.eval( callstack, interpreter );
				} finally {
					// put it back
					callstack.swap( enclosingNameSpace );
				}

				target = null;  // handled target
				break;
			}
		}

		// evaluate finally block
		if( finallyBlock != null ) {
			Object result = finallyBlock.eval(callstack, interpreter);
			if( result instanceof ReturnControl )
				return result;
		}

		// exception fell through, throw it upward...
		if(target != null)
			throw target;

		if(ret instanceof ReturnControl)
			return ret;
		else	
			return Primitive.VOID;
	}
}
