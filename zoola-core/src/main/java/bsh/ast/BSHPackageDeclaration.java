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

public class BSHPackageDeclaration extends SimpleNode
{

  public BSHPackageDeclaration(int id) {
    super(id);
  }

	public Object eval( CallStack callstack, Interpreter interpreter )
		throws EvalError
	{
		BSHAmbiguousName name = (BSHAmbiguousName)jjtGetChild(0);
		NameSpace namespace = callstack.top();
		namespace.setPackage( name.text );
		// import the package we're in by default...
		namespace.importPackage( name.text );
		return Primitive.VOID;
	}
}