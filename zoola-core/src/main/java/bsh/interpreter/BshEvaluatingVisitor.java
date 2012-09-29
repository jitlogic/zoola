package bsh.interpreter;

import bsh.*;
import bsh.ast.*;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static bsh.ParserConstants.*;

/**
 * @author RLE <rafal.lewczuk@gmail.com>
 */
public class BshEvaluatingVisitor extends BshNodeVisitor<Object> {

    private CallStack callstack;
    private Interpreter interpreter;


    public BshEvaluatingVisitor(CallStack callstack, Interpreter interpreter) {
        this.callstack = callstack;
        this.interpreter = interpreter;
    }


    public CallStack getCallstack() {
        return callstack;
    }


    public Interpreter getInterpreter() {
        return interpreter;
    }

    public Object visit(BSHAllocationExpression node) {
        //return node.eval(callstack, interpreter);
        // type is either a class name or a primitive type
        SimpleNode type = (SimpleNode)node.jjtGetChild(0);

        // args is either constructor arguments or array dimensions
        SimpleNode args = (SimpleNode)node.jjtGetChild(1);

        if ( type instanceof BSHAmbiguousName)
        {
            BSHAmbiguousName name = (BSHAmbiguousName)type;

            if (args instanceof BSHArguments)
                return node.objectAllocation(name, (BSHArguments) args,
                        callstack, interpreter);
            else
                return node.objectArrayAllocation(name, (BSHArrayDimensions) args,
                        callstack, interpreter);
        }
        else
            return node.primitiveArrayAllocation((BSHPrimitiveType) type,
                    (BSHArrayDimensions) args, callstack, interpreter);

    }


    public Object visit(BSHAmbiguousName node) {
        throw new InterpreterError(
                "Don't know how to eval an ambiguous name!"
                        +"  Use toObject() if you want an object." );
    }


    public Object visit(BSHArguments node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHArguments class.");
    }


    /**
     Evaluate the structure of the array in one of two ways:

     a) an initializer exists, evaluate it and return
     the fully constructed array object, also record the dimensions
     of that array

     b) evaluate and record the lengths in each dimension and
     return void.

     The structure of the array dims is maintained in dimensions.
     */
    public Object visit(BSHArrayDimensions node) {
        SimpleNode child = (SimpleNode)node.jjtGetChild(0);

        /*
              Child is array initializer.  Evaluate it and fill in the
              dimensions it returns.  Initialized arrays are always fully defined
              (no undefined dimensions to worry about).
              The syntax uses the undefinedDimension count.
              e.g. int [][] { 1, 2 };
          */
        if (child instanceof BSHArrayInitializer)
        {
            if ( node.baseType == null )
                throw new EvalError(
                        "Internal Array Eval err:  unknown base type",
                        node, callstack );

            Object initValue = ((BSHArrayInitializer)child).eval(
                    node.baseType, node.numUndefinedDims, callstack, interpreter);

            Class arrayClass = initValue.getClass();
            int actualDimensions = Reflect.getArrayDimensions(arrayClass);
            node.definedDimensions = new int[ actualDimensions ];

            // Compare with number of dimensions actually created with the
            // number specified (syntax uses the undefined ones here)
            if ( node.definedDimensions.length != node.numUndefinedDims )
                throw new EvalError(
                        "Incompatible initializer. Allocation calls for a " +
                                node.numUndefinedDims+ " dimensional array, but initializer is a " +
                                actualDimensions + " dimensional array", node, callstack );

            // fill in definedDimensions [] lengths
            Object arraySlice = initValue;
            for ( int i = 0; i < node.definedDimensions.length; i++ ) {
                node.definedDimensions[i] = Array.getLength(arraySlice);
                if ( node.definedDimensions[i] > 0 )
                    arraySlice = Array.get(arraySlice, 0);
            }

            return initValue;
        }
        else
        // Evaluate the defined dimensions of the array
        {
            node.definedDimensions = new int[ node.numDefinedDims ];

            for(int i = 0; i < node.numDefinedDims; i++)
            {
                try {
                    Object length = ((SimpleNode)node.jjtGetChild(i)).accept(this);
                    node.definedDimensions[i] = ((Primitive)length).intValue();
                }
                catch(Exception e)
                {
                    throw new EvalError(
                            "Array index: " + i +
                                    " does not evaluate to an integer", node, callstack );
                }
            }
        }

        return Primitive.VOID;
    }


    public Object visit(BSHArrayInitializer node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHArrayInitializer class.");
    }


    public Object visit(BSHAssignment node) {
        BSHPrimaryExpression lhsNode =
                (BSHPrimaryExpression)node.jjtGetChild(0);

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
        if ( node.operator != ASSIGN ) // assign doesn't need the pre-value
            try {
                lhsValue = lhs.getValue();
            } catch ( UtilEvalError e ) {
                throw e.toEvalError( node, callstack );
            }

        SimpleNode rhsNode = (SimpleNode)node.jjtGetChild(1);

        Object rhs;

        // implement "blocks" foo = { };
        // if ( rhsNode instanceof BSHBlock )
        //    rsh =
        // else
        rhs = rhsNode.accept(this);

        if ( rhs == Primitive.VOID )
            throw new EvalError("Void assignment.", node, callstack );

        try {
            switch(node.operator)
            {
                case ASSIGN:
                    return lhs.assign( rhs, strictJava );

                case PLUSASSIGN:
                    return lhs.assign(
                            node.operation(lhsValue, rhs, PLUS), strictJava );

                case MINUSASSIGN:
                    return lhs.assign(
                            node.operation(lhsValue, rhs, MINUS), strictJava );

                case STARASSIGN:
                    return lhs.assign(
                            node.operation(lhsValue, rhs, STAR), strictJava );

                case SLASHASSIGN:
                    return lhs.assign(
                            node.operation(lhsValue, rhs, SLASH), strictJava );

                case ANDASSIGN:
                case ANDASSIGNX:
                    return lhs.assign(
                            node.operation(lhsValue, rhs, BIT_AND), strictJava );

                case ORASSIGN:
                case ORASSIGNX:
                    return lhs.assign(
                            node.operation(lhsValue, rhs, BIT_OR), strictJava );

                case XORASSIGN:
                    return lhs.assign(
                            node.operation(lhsValue, rhs, XOR), strictJava );

                case MODASSIGN:
                    return lhs.assign(
                            node.operation(lhsValue, rhs, MOD), strictJava );

                case LSHIFTASSIGN:
                case LSHIFTASSIGNX:
                    return lhs.assign(
                            node.operation(lhsValue, rhs, LSHIFT), strictJava );

                case RSIGNEDSHIFTASSIGN:
                case RSIGNEDSHIFTASSIGNX:
                    return lhs.assign(
                            node.operation(lhsValue, rhs, RSIGNEDSHIFT ), strictJava );

                case RUNSIGNEDSHIFTASSIGN:
                case RUNSIGNEDSHIFTASSIGNX:
                    return lhs.assign(
                            node.operation(lhsValue, rhs, RUNSIGNEDSHIFT),
                            strictJava );

                default:
                    throw new InterpreterError(
                            "unimplemented operator in assignment BSH");
            }
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( node, callstack );
        }
    }


    public Object visit(BSHBinaryExpression node) {
        Object lhs = ((SimpleNode)node.jjtGetChild(0)).accept(this);

        /*
              Doing instanceof?  Next node is a type.
          */
        if (node.kind == INSTANCEOF)
        {
            // null object ref is not instance of any type
            if ( lhs == Primitive.NULL )
                return new Primitive(false);

            Class rhs = ((BSHType)node.jjtGetChild(1)).getType(
                    callstack, interpreter );
            /*
               // primitive (number or void) cannot be tested for instanceof
               if (lhs instanceof Primitive)
                   throw new EvalError("Cannot be instance of primitive type." );
           */
            /*
                   Primitive (number or void) is not normally an instanceof
                   anything.  But for internal use we'll test true for the
                   bsh.Primitive class.
                   i.e. (5 instanceof bsh.Primitive) will be true
               */
            if ( lhs instanceof Primitive )
                if ( rhs == bsh.Primitive.class )
                    return new Primitive(true);
                else
                    return new Primitive(false);

            // General case - performe the instanceof based on assignability
            boolean ret = Types.isJavaBaseAssignable( rhs, lhs.getClass() );
            return new Primitive(ret);
        }


        // The following two boolean checks were tacked on.
        // This could probably be smoothed out.

        /*
              Look ahead and short circuit evaluation of the rhs if:
                  we're a boolean AND and the lhs is false.
          */
        if ( node.kind == BOOL_AND || node.kind == BOOL_ANDX ) {
            Object obj = lhs;
            if ( node.isPrimitiveValue(lhs) )
                obj = ((Primitive)lhs).getValue();
            if ( obj instanceof Boolean &&
                    ( ((Boolean)obj).booleanValue() == false ) )
                return new Primitive(false);
        }
        /*
              Look ahead and short circuit evaluation of the rhs if:
                  we're a boolean AND and the lhs is false.
          */
        if ( node.kind == BOOL_OR || node.kind == BOOL_ORX ) {
            Object obj = lhs;
            if ( node.isPrimitiveValue(lhs) )
                obj = ((Primitive)lhs).getValue();
            if ( obj instanceof Boolean &&
                    ( ((Boolean)obj).booleanValue() == true ) )
                return new Primitive(true);
        }

        // end stuff that was tacked on for boolean short-circuiting.

        /*
              Are both the lhs and rhs either wrappers or primitive values?
              do binary op
          */
        boolean isLhsWrapper = node.isWrapper(lhs);
        Object rhs = ((SimpleNode)node.jjtGetChild(1)).accept(this); //eval(callstack, interpreter);
        boolean isRhsWrapper = node.isWrapper(rhs);
        if (
                ( isLhsWrapper || node.isPrimitiveValue(lhs) )
                        && ( isRhsWrapper || node.isPrimitiveValue(rhs) )
                )
        {
            // Special case for EQ on two wrapper objects
            if ( (isLhsWrapper && isRhsWrapper && node.kind == EQ))
            {
                /*
                        Don't auto-unwrap wrappers (preserve identity semantics)
                        FALL THROUGH TO OBJECT OPERATIONS BELOW.
                    */
            } else
                try {
                    return Primitive.binaryOperation(lhs, rhs, node.kind);
                } catch ( UtilEvalError e ) {
                    throw e.toEvalError( node, callstack  );
                }
        }
        /*
      Doing the following makes it hard to use untyped vars...
      e.g. if ( arg == null ) ...what if arg is a primitive?
      The answer is that we should test only if the var is typed...?
      need to get that info here...

          else
          {
          // Do we have a mixture of primitive values and non-primitives ?
          // (primitiveValue = not null, not void)

          int primCount = 0;
          if ( isPrimitiveValue( lhs ) )
              ++primCount;
          if ( isPrimitiveValue( rhs ) )
              ++primCount;

          if ( primCount > 1 )
              // both primitive types, should have been handled above
              throw new InterpreterError("should not be here");
          else
          if ( primCount == 1 )
              // mixture of one and the other
              throw new EvalError("Operator: '" + tokenImage[kind]
                  +"' inappropriate for object / primitive combination.",
                  this, callstack );

          // else fall through to handle both non-primitive types

          // end check for primitive and non-primitive mix
          }
      */

        /*
              Treat lhs and rhs as arbitrary objects and do the operation.
              (including NULL and VOID represented by their Primitive types)
          */
        //System.out.println("binary op arbitrary obj: {"+lhs+"}, {"+rhs+"}");
        switch(node.kind)
        {
            case EQ:
                return new Primitive((lhs == rhs));

            case NE:
                return new Primitive((lhs != rhs));

            case PLUS:
                if(lhs instanceof String || rhs instanceof String)
                    return lhs.toString() + rhs.toString();

                // FALL THROUGH TO DEFAULT CASE!!!

            default:
                if(lhs instanceof Primitive || rhs instanceof Primitive)
                    if ( lhs == Primitive.VOID || rhs == Primitive.VOID )
                        throw new EvalError(
                                "illegal use of undefined variable, class, or 'void' literal",
                                node, callstack );
                    else
                    if ( lhs == Primitive.NULL || rhs == Primitive.NULL )
                        throw new EvalError(
                                "illegal use of null value or 'null' literal", node, callstack);

                throw new EvalError("Operator: '" + tokenImage[node.kind] +
                        "' inappropriate for objects", node, callstack );
        }
    }


    public Object visit(BSHBlock node) {
        return node.eval(callstack, interpreter, false);
    }


    public Object visit(BSHCastExpression node) {
        NameSpace namespace = callstack.top();
        Class toType = ((BSHType)node.jjtGetChild(0)).getType(
                callstack, interpreter );
        SimpleNode expression = (SimpleNode)node.jjtGetChild(1);

        // evaluate the expression
        Object fromValue = expression.accept(this);
        Class fromType = fromValue.getClass();

        // TODO: need to add isJavaCastable() test for strictJava
        // (as opposed to isJavaAssignable())
        try {
            return Types.castObject( fromValue, toType, Types.CAST );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( node, callstack  );
        }
    }


    public Object visit(BSHClassDeclaration node) {
        synchronized (node) {
            if (node.generatedClass == null) {
                node.generatedClass = node.generateClass(callstack, interpreter);
            }
            return node.generatedClass;
        }
    }


    public Object visit(BSHEnhancedForStatement node) {
        Class elementType = null;
        SimpleNode expression, statement=null;

        NameSpace enclosingNameSpace = callstack.top();
        SimpleNode firstNode =((SimpleNode)node.jjtGetChild(0));
        int nodeCount = node.jjtGetNumChildren();

        if ( firstNode instanceof BSHType )
        {
            elementType=((BSHType)firstNode).getType( callstack, interpreter );
            expression=((SimpleNode)node.jjtGetChild(1));
            if ( nodeCount>2 )
                statement=((SimpleNode)node.jjtGetChild(2));
        } else
        {
            expression=firstNode;
            if ( nodeCount>1 )
                statement=((SimpleNode)node.jjtGetChild(1));
        }

        BlockNameSpace eachNameSpace = new BlockNameSpace( enclosingNameSpace );
        callstack.swap( eachNameSpace );

        final Object iteratee = expression.accept(this);

        if ( iteratee == Primitive.NULL )
            throw new EvalError("The collection, array, map, iterator, or " +
                    "enumeration portion of a for statement cannot be null.",
                    node, callstack );

        CollectionManager cm = CollectionManager.getCollectionManager();
        if ( !cm.isBshIterable( iteratee ) )
            throw new EvalError("Can't iterate over type: "
                    +iteratee.getClass(), node, callstack );
        Iterator iterator = cm.getBshIterator( iteratee );

        Object returnControl = Primitive.VOID;
        while( iterator.hasNext() )
        {
            try {
                Object value = iterator.next();
                if ( value == null )
                    value = Primitive.NULL;
                if ( elementType != null )
                    eachNameSpace.setTypedVariable(
                            node.varName/*name*/, elementType/*type*/,
                            value, new Modifiers()/*none*/ );
                else
                    eachNameSpace.setVariable( node.varName, value, false );
            } catch ( UtilEvalError e ) {
                throw e.toEvalError(
                        "for loop iterator variable:"+ node.varName, node, callstack );
            }

            boolean breakout = false; // switch eats a multi-level break here?
            if ( statement != null ) // not empty statement
            {
                Object ret = statement.accept(this); //eval( callstack, interpreter );

                if (ret instanceof ReturnControl)
                {
                    switch(((ReturnControl)ret).kind)
                    {
                        case RETURN:
                            returnControl = ret;
                            breakout = true;
                            break;

                        case CONTINUE:
                            break;

                        case BREAK:
                            breakout = true;
                            break;
                    }
                }
            }

            if (breakout)
                break;
        }

        callstack.swap(enclosingNameSpace);
        return returnControl;
    }


    public Object visit(BSHFormalComment node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHFormalComment class.");
    }


    public Object visit(BSHFormalParameter node) {
        if ( node.jjtGetNumChildren() > 0 )
            node.type = ((BSHType)node.jjtGetChild(0)).getType( callstack, interpreter );
        else
            node.type = node.UNTYPED;

        return node.type;
    }


    public Object visit(BSHFormalParameters node) {
        if ( node.paramTypes != null )
            return node.paramTypes;

        node.insureParsed();
        Class [] paramTypes = new Class[node.numArgs];

        for(int i=0; i<node.numArgs; i++)
        {
            BSHFormalParameter param = (BSHFormalParameter)node.jjtGetChild(i);
            paramTypes[i] = (Class)param.accept(this); //eval( callstack, interpreter );
        }

        node.paramTypes = paramTypes;

        return paramTypes;
    }


    public Object visit(BSHForStatement node) {
        int i = 0;
        if(node.hasForInit)
            node.forInit = ((SimpleNode)node.jjtGetChild(i++));
        if(node.hasExpression)
            node.expression = ((SimpleNode)node.jjtGetChild(i++));
        if(node.hasForUpdate)
            node.forUpdate = ((SimpleNode)node.jjtGetChild(i++));
        if(i < node.jjtGetNumChildren()) // should normally be
            node.statement = ((SimpleNode)node.jjtGetChild(i));

        NameSpace enclosingNameSpace= callstack.top();
        BlockNameSpace forNameSpace = new BlockNameSpace( enclosingNameSpace );

        /*
              Note: some interesting things are going on here.

              1) We swap instead of push...  The primary mode of operation
              acts like we are in the enclosing namespace...  (super must be
              preserved, etc.)

              2) We do *not* call the body block eval with the namespace
              override.  Instead we allow it to create a second subordinate
              BlockNameSpace child of the forNameSpace.  Variable propogation
              still works through the chain, but the block's child cleans the
              state between iteration.
              (which is correct Java behavior... see forscope4.bsh)
          */

        // put forNameSpace it on the top of the stack
        // Note: it's important that there is only one exit point from this
        // method so that we can swap back the namespace.
        callstack.swap( forNameSpace );

        // Do the for init
        if ( node.hasForInit )
            node.forInit.accept(this);

        Object returnControl = Primitive.VOID;
        while(true)
        {
            if ( node.hasExpression )
            {
                boolean cond = BSHIfStatement.evaluateCondition(
                        node.expression, callstack, interpreter);

                if ( !cond )
                    break;
            }

            boolean breakout = false; // switch eats a multi-level break here?
            if ( node.statement != null ) // not empty statement
            {
                // do *not* invoke special override for block... (see above)
                Object ret = node.statement.accept(this);

                if (ret instanceof ReturnControl)
                {
                    switch(((ReturnControl)ret).kind)
                    {
                        case RETURN:
                            returnControl = ret;
                            breakout = true;
                            break;

                        case CONTINUE:
                            break;

                        case BREAK:
                            breakout = true;
                            break;
                    }
                }
            }

            if ( breakout )
                break;

            if ( node.hasForUpdate )
                node.forUpdate.accept(this);
        }

        callstack.swap( enclosingNameSpace );  // put it back
        return returnControl;
    }


    public Object visit(BSHIfStatement node) {
        Object ret = null;

        if( node.evaluateCondition(
                (SimpleNode) node.jjtGetChild(0), callstack, interpreter) )
            ret = ((SimpleNode)node.jjtGetChild(1)).accept(this);
        else
        if(node.jjtGetNumChildren() > 2)
            ret = ((SimpleNode)node.jjtGetChild(2)).accept(this);

        if(ret instanceof ReturnControl)
            return ret;
        else
            return Primitive.VOID;
    }


    public Object visit(BSHImportDeclaration node) {
        NameSpace namespace = callstack.top();
        if ( node.superImport )
            try {
                namespace.doSuperImport();
            } catch ( UtilEvalError e ) {
                throw e.toEvalError( node, callstack  );
            }
        else
        {
            if ( node.staticImport )
            {
                if ( node.importPackage )
                {
                    Class clas = ((BSHAmbiguousName)node.jjtGetChild(0)).toClass(
                            callstack, interpreter );
                    namespace.importStatic( clas );
                } else
                    throw new EvalError(
                            "static field imports not supported yet",
                            node, callstack );
            } else
            {
                String name = ((BSHAmbiguousName)node.jjtGetChild(0)).text;
                if ( node.importPackage )
                    namespace.importPackage(name);
                else
                    namespace.importClass(name);
            }
        }

        return Primitive.VOID;
    }


    public Object visit(BSHLiteral node) {
        if (node.value == null)
            throw new InterpreterError("Null in bsh literal: "+node.value);

        return node.value;
    }


    public Object visit(BSHMethodDeclaration node) {
        node.returnType = node.evalReturnType(callstack, interpreter);
        node.evalNodes(callstack, interpreter);

        // Install an *instance* of this method in the namespace.
        // See notes in BshMethod

        // This is not good...
        // need a way to update eval without re-installing...
        // so that we can re-eval params, etc. when classloader changes
        // look into this

        NameSpace namespace = callstack.top();
        BshMethod bshMethod = new BshMethod( node, namespace, node.modifiers );
        try {
            namespace.setMethod( bshMethod );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError(node,callstack);
        }

        return Primitive.VOID;
    }


    public Object visit(BSHMethodInvocation node) {
        NameSpace namespace = callstack.top();
        BSHAmbiguousName nameNode = node.getNameNode();

        // Do not evaluate methods this() or super() in class instance space
        // (i.e. inside a constructor)
        if ( namespace.getParent() != null && namespace.getParent().isClass
                && ( nameNode.text.equals("super") || nameNode.text.equals("this") )
                )
            return Primitive.VOID;

        Name name = nameNode.getName(namespace);
        Object[] args = node.getArgsNode().getArguments(this);

// This try/catch block is replicated is BSHPrimarySuffix... need to
// factor out common functionality...
// Move to Reflect?
        try {
            return name.invokeMethod( interpreter, args, callstack, node);
        } catch ( ReflectError e ) {
            throw new EvalError(
                    "Error in method invocation: " + e.getMessage(),
                    node, callstack, e );
        } catch ( InvocationTargetException e )
        {
            String msg = "Method Invocation "+name;
            Throwable te = e.getTargetException();

            /*
                   Try to squeltch the native code stack trace if the exception
                   was caused by a reflective call back into the bsh interpreter
                   (e.g. eval() or source()
               */
            boolean isNative = true;
            if ( te instanceof EvalError )
                if ( te instanceof TargetError )
                    isNative = ((TargetError)te).inNativeCode();
                else
                    isNative = false;

            throw new TargetError( msg, te, node, callstack, isNative );
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( node, callstack );
        }
    }


    public Object visit(BSHPackageDeclaration node) {
        BSHAmbiguousName name = (BSHAmbiguousName)node.jjtGetChild(0);
        NameSpace namespace = callstack.top();
        namespace.setPackage( name.text );
        // import the package we're in by default...
        namespace.importPackage( name.text );
        return Primitive.VOID;
    }


    public Object visit(BSHPrimaryExpression node) {
        return node.eval( false, callstack, interpreter );
    }


    public Object visit(BSHPrimarySuffix node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHPrimarySuffix class.");
    }


    public Object visit(BSHPrimitiveType node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHPrimarySuffix class.");
    }


    public Object visit(BSHReturnStatement node) {
        Object value;
        if(node.jjtGetNumChildren() > 0)
            value = ((SimpleNode)node.jjtGetChild(0)).accept(this);
        else
            value = Primitive.VOID;

        return new ReturnControl( node.kind, value, node );
    }


    public Object visit(BSHReturnType node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHPrimarySuffix class.");
    }


    public Object visit(BSHStatementExpressionList node) {
        int n = node.jjtGetNumChildren();
        for(int i=0; i<n; i++)
        {
            SimpleNode nn = ((SimpleNode)node.jjtGetChild(i));
            nn.accept(this);
        }
        return Primitive.VOID;
    }


    public Object visit(BSHSwitchLabel node) {
        if ( node.isDefault )
            return null; // should probably error
        SimpleNode label = ((SimpleNode)node.jjtGetChild(0));
        return label.accept(this);
    }


    public Object visit(BSHSwitchStatement node) {
        int numchild = node.jjtGetNumChildren();
        int child = 0;
        SimpleNode switchExp = ((SimpleNode)node.jjtGetChild(child++));
        Object switchVal = switchExp.accept(this);

        /*
              Note: this could be made clearer by adding an inner class for the
              cases and an object context for the child traversal.
          */
        // first label
        BSHSwitchLabel label;
        Object obj;
        ReturnControl returnControl=null;

        // get the first label
        if ( child >= numchild )
            throw new EvalError("Empty switch statement.", node, callstack );
        label = ((BSHSwitchLabel)node.jjtGetChild(child++));

        // while more labels or blocks and haven't hit return control
        while ( child < numchild && returnControl == null )
        {
            // if label is default or equals switchVal
            if ( label.isDefault
                    || node.primitiveEquals(
                    switchVal, label.accept(this),
                    callstack, switchExp)
                    )
            {
                // execute nodes, skipping labels, until a break or return
                while ( child < numchild )
                {
                    obj = node.jjtGetChild(child++);
                    if ( obj instanceof BSHSwitchLabel )
                        continue;
                    // eval it
                    Object value =
                            ((SimpleNode)obj).accept(this);

                    // should check to disallow continue here?
                    if ( value instanceof ReturnControl ) {
                        returnControl = (ReturnControl)value;
                        break;
                    }
                }
            } else
            {
                // skip nodes until next label
                while ( child < numchild )
                {
                    obj = node.jjtGetChild(child++);
                    if ( obj instanceof BSHSwitchLabel ) {
                        label = (BSHSwitchLabel)obj;
                        break;
                    }
                }
            }
        }

        if ( returnControl != null && returnControl.kind == RETURN )
            return returnControl;
        else
            return Primitive.VOID;
    }


    public Object visit(BSHTernaryExpression node) {
        SimpleNode
                cond = (SimpleNode)node.jjtGetChild(0),
                evalTrue = (SimpleNode)node.jjtGetChild(1),
                evalFalse = (SimpleNode)node.jjtGetChild(2);

        if ( BSHIfStatement.evaluateCondition(cond, callstack, interpreter) )
            return evalTrue.accept(this);
        else
            return evalFalse.accept(this);
    }


    public Object visit(BSHThrowStatement node) {
        Object obj = ((SimpleNode)node.jjtGetChild(0)).accept(this);

        // need to loosen this to any throwable... do we need to handle
        // that in interpreter somewhere?  check first...
        if(!(obj instanceof Exception))
            throw new EvalError("Expression in 'throw' must be Exception type",
                    node, callstack );

        // wrap the exception in a TargetException to propogate it up
        throw new TargetError( (Exception)obj, node, callstack );
    }

    public Object visit(BSHTryStatement node) {
        BSHBlock tryBlock = ((BSHBlock)node.jjtGetChild(0));

        List<BSHFormalParameter> catchParams = new ArrayList<BSHFormalParameter>();
        List<BSHBlock> catchBlocks = new ArrayList<BSHBlock>();

        int nchild = node.jjtGetNumChildren();
        Node nodeObj = null;
        int i=1;
        while((i < nchild) && ((nodeObj = node.jjtGetChild(i++)) instanceof BSHFormalParameter))
        {
            catchParams.add((BSHFormalParameter)nodeObj);
            catchBlocks.add((BSHBlock)node.jjtGetChild(i++));
            nodeObj = null;
        }
        // finaly block
        BSHBlock finallyBlock = null;
        if(nodeObj != null)
            finallyBlock = (BSHBlock)nodeObj;

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
            ret = tryBlock.accept(this);
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
                fp.accept(this);

                if ( fp.type == null && interpreter.getStrictJava() )
                    throw new EvalError(
                            "(Strict Java) Untyped catch block", node, callstack );

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
                    ret = cb.accept(this);
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
            Object result = finallyBlock.accept(this);
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

    public Object visit(BSHType node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHType class.");
    }

    public Object visit(BSHTypedVariableDeclaration node) {
        try {
            NameSpace namespace = callstack.top();
            BSHType typeNode = node.getTypeNode();
            Class type = typeNode.getType( callstack, interpreter );

            BSHVariableDeclarator [] bvda = node.getDeclarators();
            for (int i = 0; i < bvda.length; i++)
            {
                BSHVariableDeclarator dec = bvda[i];

                // Type node is passed down the chain for array initializers
                // which need it under some circumstances
                Object value = dec.eval( typeNode, callstack, interpreter);

                try {
                    namespace.setTypedVariable(
                            dec.name, type, value, node.modifiers );
                } catch ( UtilEvalError e ) {
                    throw e.toEvalError( node, callstack );
                }
            }
        } catch ( EvalError e ) {
            e.reThrow( "Typed variable declaration" );
        }

        return Primitive.VOID;
    }


    public Object visit(BSHUnaryExpression node) {
        SimpleNode simpleNode = (SimpleNode)node.jjtGetChild(0);

        // If this is a unary increment of decrement (either pre or postfix)
        // then we need an LHS to which to assign the result.  Otherwise
        // just do the unary operation for the value.
        try {
            if ( node.kind == INCR || node.kind == DECR ) {
                LHS lhs = ((BSHPrimaryExpression)simpleNode).toLHS(
                        callstack, interpreter );
                return node.lhsUnaryOperation(lhs, interpreter.getStrictJava());
            } else
                return
                        node.unaryOperation(simpleNode.accept(this), node.kind);
        } catch ( UtilEvalError e ) {
            throw e.toEvalError( node, callstack );
        }
    }

    public Object visit(BSHVariableDeclarator node) {
        throw new InterpreterError(
                "Unimplemented or inappropriate for BSHVariableDeclarator class.");
    }

    public Object visit(BSHWhileStatement node) {
        int numChild = node.jjtGetNumChildren();

        // Order of body and condition is swapped for do / while
        final SimpleNode condExp;
        final SimpleNode body;

        if ( node.isDoStatement ) {
            condExp = (SimpleNode) node.jjtGetChild(1);
            body = (SimpleNode) node.jjtGetChild(0);
        } else {
            condExp = (SimpleNode) node.jjtGetChild(0);
            if ( numChild > 1 )	{
                body = (SimpleNode) node.jjtGetChild(1);
            } else {
                body = null;
            }
        }

        boolean doOnceFlag = node.isDoStatement;

        while (doOnceFlag || BSHIfStatement.evaluateCondition(condExp, callstack, interpreter)) {
            doOnceFlag = false;
            // no body?
            if ( body == null ) {
                continue;
            }
            Object ret = body.accept(this);
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
