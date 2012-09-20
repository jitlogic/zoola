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
import bsh.ast.BSHAmbiguousName;
import bsh.ast.BSHBlock;

/**
*/
public class BSHClassDeclaration extends SimpleNode
{
	/**
		The class instance initializer method name.
		A BshMethod by this name is installed by the class delcaration into 
		the static class body namespace.  
		It is called once to initialize the static members of the class space 
		and each time an instances is created to initialize the instance
		members.
	*/
	static final String CLASSINITNAME = "_bshClassInit";

	public String name;
	public Modifiers modifiers;
	public int numInterfaces;
	public boolean extend;
	public boolean isInterface;
	private Class<?> generatedClass;

	public BSHClassDeclaration(int id) { super(id); }

	/**
	*/
	public synchronized Object eval(final CallStack callstack, final Interpreter interpreter ) throws EvalError {
		if (generatedClass == null) {
			generatedClass = generateClass(callstack, interpreter);
		}
		return generatedClass;
	}


	private Class<?> generateClass(final CallStack callstack, final Interpreter interpreter) throws EvalError {
		int child = 0;

		// resolve superclass if any
		Class superClass = null;
		if ( extend ) {
			BSHAmbiguousName superNode = (BSHAmbiguousName)jjtGetChild(child++);
			superClass = superNode.toClass( callstack, interpreter );
		}

		// Get interfaces
		Class [] interfaces = new Class[numInterfaces];
		for( int i=0; i<numInterfaces; i++) {
			BSHAmbiguousName node = (BSHAmbiguousName)jjtGetChild(child++);
			interfaces[i] = node.toClass(callstack, interpreter);
			if ( !interfaces[i].isInterface() )
				throw new EvalError(
					"Type: "+node.text+" is not an interface!",
					this, callstack );
		}

		BSHBlock block;
		// Get the class body BSHBlock
		if ( child < jjtGetNumChildren() )
			block = (BSHBlock) jjtGetChild(child);
		else
			block = new BSHBlock( ParserTreeConstants.JJTBLOCK );

		return ClassGenerator.getClassGenerator().generateClass(
			name, modifiers, interfaces, superClass, block, isInterface,
			callstack, interpreter );
	}


	public String toString() {
		return "ClassDeclaration: " + name;
	}
}
