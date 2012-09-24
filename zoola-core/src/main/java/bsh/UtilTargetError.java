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
	UtilTargetError is an error corresponding to a TargetError but thrown by a 
	utility or other class that does not have the caller context (Node) 
	available to it.  See UtilEvalError for an explanation of the difference
	between UtilEvalError and EvalError.
	<p>

	@see UtilEvalError
*/
public class UtilTargetError extends UtilEvalError
{
	public Throwable t;

	public UtilTargetError( String message, Throwable t ) {
		super( message );
		this.t = t;
	}

	public UtilTargetError( Throwable t ) {
		this( null, t );
	}

	/**
		Override toEvalError to throw TargetError type.
	*/
	public EvalError toEvalError( 
		String msg, SimpleNode node, CallStack callstack  ) 
	{
		if ( msg == null )
			msg = getMessage();
		else
			msg = msg + ": " + getMessage();

		return new TargetError( msg, t, node, callstack, false );
	}
}

