package compiler;

import symbol.Symbol;
import symbol.SymbolTable;
import syntax.Node;
import syntax.SimpleNode;

public class Semantic extends Compiler
{
    public boolean analyse(SimpleNode root)
    {
        boolean continueAnalysis;
        Node classNode = root.jjtGetChild(0);

        for (int i = 0; i < classNode.jjtGetNumChildren(); i++)
        {
            Node child = classNode.jjtGetChild(i);

            switch (child.toString())
            {
                case "Main":
                case "Method":
                    continueAnalysis = analyseFunction(child);
                    break;

                default:
                    continue;
            }

            if (!continueAnalysis)
                return false;
        }

        return true;
    }

    public boolean analyseFunction(Node func)
    {
        String funcName = getFunctionName(func);

        for(int i = 0; i < func.jjtGetNumChildren(); i++)
        {
            if(func.jjtGetChild(i).toString().equals("Var") || func.jjtGetChild(i).toString().equals("Arg"))
                continue;
            else
            {
                if(func.jjtGetChild(i).toString().equals("Return"))
                {
                    if(!isTheSameType(evaluatesTo(func.jjtGetChild(i).jjtGetChild(0), funcName), func.getReturnType()))
                    {
                        System.out.println("Return value in function " + func.getName() + " does not meet function prototype");
                        return false;
                    }
                    else
                        return true;

                }
                else
                {
                    if(!analyseStatement(func.jjtGetChild(i), funcName))
                    {
                        System.out.println("Error in function " + func.getName() + " statement(s)");
                        return false;
                    }
                }
            }

        }

        return true;
    }

    public boolean analyseStatement(Node statement, String funcName)
    {
        boolean continueAnalysis;

        switch (statement.toString())
        {
            case "If":
                continueAnalysis = analyseIf(statement, funcName);
                break;

            case "While":
                continueAnalysis = analyseWhile(statement, funcName);
                break;

            case "TERM":
                continueAnalysis = !termEvaluatesTo(statement, funcName).equals("error");
                break;

            case "EQUALS":
                continueAnalysis = analyseEquals(statement, funcName);
                break;

            case "Else":
            case "Then":
                if(statement.jjtGetNumChildren() == 1)
                    continueAnalysis = analyseStatement(statement.jjtGetChild(0), funcName);
                else
                    continueAnalysis = true;

                break;

            // Standalone arithmetic and boolean expressions not valid (?)
            case "AND":
            case "LOWER":
            case "ADD":
            case "SUB":
            case "MUL":
            case "DIV":
                System.out.println("Standalone arithmetic or boolean expressions detected in " + funcName);
                return false;

            default:
                System.out.println("Unknown statement type " + statement.toString() + " in function " + funcName);
                return false;
        }

        if(!continueAnalysis)
            return false;

        return true;
    }

    public boolean analyseIf(Node ifNode, String funcName)
    {
        if(ifNode.jjtGetNumChildren() < 3)
        {
            System.out.println("If in " + funcName + " doesn't have enough children");
            return false;
        }

        if(!isTheSameType(evaluatesTo(ifNode.jjtGetChild(0), funcName), "boolean"))
        {
            System.out.println("if condition in function " + funcName + " doesn't evaluate to a boolean");
            return false;
        }

        Node then = ifNode.jjtGetChild(1);

        for(int i = 0; i < then.jjtGetNumChildren(); i++)
        {
            if (!analyseStatement(then.jjtGetChild(i), funcName))
            {
                System.out.println("if 'then' statement is invalid in function " + funcName);
                return false;
            }
        }

        Node elseNode = ifNode.jjtGetChild(2);

        for(int i = 0; i < elseNode.jjtGetNumChildren(); i++)
        {
            if (!analyseStatement(elseNode.jjtGetChild(i), funcName))
            {
                System.out.println("if 'then' statement is invalid in function " + funcName);
                return false;
            }
        }


        return true;
    }

    public boolean analyseWhile(Node whileNode, String funcName)
    {
        if(whileNode.jjtGetNumChildren() < 2)
        {
            System.out.println("While in " + funcName + " doesn't have enough children");
            return false;
        }

        if(!isTheSameType(evaluatesTo(whileNode.jjtGetChild(0), funcName), "boolean"))
        {
            System.out.println("while condition in function " + funcName + " doesn't evaluate to a boolean");
            return false;
        }

        Node then = whileNode.jjtGetChild(1);

        for (int i = 0; i < then.jjtGetNumChildren(); i++)
        {
            if (!analyseStatement(then.jjtGetChild(i), funcName))
            {
                System.out.println("while 'then' statement is invalid in function " + funcName);
                return false;
            }
        }

        return true;
    }

    public boolean analyseEquals(Node equals, String funcName)
    {
        String op1Value = identifierEvaluatesTo(equals.jjtGetChild(0), funcName, false, false);
        String op2Value = evaluatesTo(equals.jjtGetChild(1), funcName);

        if(op1Value.equals("error") || op2Value.equals("error"))
        {
            System.out.println("Error in equals operand(s) in function " + funcName);
            return false;
        }

        if(isTheSameType(op1Value, op2Value))
            return true;
        else
        {
            if(extendingClass != null && op1Value.equals(extendingClass) && op2Value.equals(className))
                return true;

            System.out.println("Equals operator types don't match in function "
                + funcName + ": " + op1Value + " vs " + op2Value);

            return false;
        }
    }

    public boolean isTheSameType(String type1, String type2)
    {
        return type1.equals(type2) || type1.equals("all") || type2.equals("all");
    }

    public String evaluatesTo(Node expression, String funcName)
    {
        switch(expression.toString())
        {
            case "ADD":
            case "SUB":
            case "DIV":
            case "MUL":
                if(expression.jjtGetNumChildren() == 2
                    && isTheSameType(evaluatesTo(expression.jjtGetChild(0), funcName), "int")
                    && isTheSameType(evaluatesTo(expression.jjtGetChild(1), funcName), "int"))
                    return "int";
                else
                {
                    System.out.println("Operand(s) in expression of type " + expression.toString()
                        + " don't evaluate to integers in function " + funcName);
                    return "error";
                }


            case "AND":
                if(expression.jjtGetNumChildren() == 2
                    && isTheSameType(evaluatesTo(expression.jjtGetChild(0), funcName), "boolean")
                    && isTheSameType(evaluatesTo(expression.jjtGetChild(1), funcName), "boolean"))
                    return "boolean";
                else
                {
                    System.out.println("Operand(s) in expression of type AND don't evaluate to booleans in function "
                        + funcName);
                    return "error";
                }

            case "NOT":
                if(expression.jjtGetNumChildren() == 1 
                    && isTheSameType(evaluatesTo(expression.jjtGetChild(0), funcName), "boolean"))
                    return "boolean";
                else
                {
                    System.out.println("Operand in negated expression doesn't evaluate to boolean in function "
                        + funcName);
                    return "error";
                }


            case "LOWER":
                if(expression.jjtGetNumChildren() == 2
                    && isTheSameType(evaluatesTo(expression.jjtGetChild(0), funcName), "int")
                    && isTheSameType(evaluatesTo(expression.jjtGetChild(1), funcName), "int"))
                    return "boolean";
                else
                {
                    System.out.println("Operand(s) in expression of type LOWER don't evaluate to integers in function "
                        + funcName);
                    return "error";
                }


            case "TERM":
                return termEvaluatesTo(expression, funcName);

            default:
                System.out.println("Unexpected token to evaluate in function " + funcName + ": " + expression.toString());
                return "error";
        }
    }

    public String termEvaluatesTo(Node term, String funcName)
    {
        String termName = term.getName();
        String value = "";

        if(termName != null)
        {
            switch(termName)
            {
                case "true":
                case "false":
                    value = "boolean";
                    break;

                case "this":

                    if(funcName.equals("main"))
                    {
                        System.out.println("This can't be used in static function main");
                        return "error";
                    }
                    
                    value = className;
                    break;

                default:

                    try
                    {
                        Integer.parseInt(termName); //Integer

                        return "int";
                    } //Variable
                    catch(NumberFormatException nfe)
                    {
                        value = identifierEvaluatesTo(term, funcName, true, false);

                        if(value.equals("error"))
                        {
                            System.out.println("Unexpected term in term evaluation in function " + funcName + ": "
                                + termName);

                            return "error";
                        }
                    }
            }
        }

        if(term.jjtGetNumChildren() == 0)
        {
            if(termName == null)
            {
                System.out.println("Term has no name nor the expected number of children in function " + funcName
                    + ": " + term.toString());

                return "error";
            }

            return value;
        }

        Node termSon = term.jjtGetChild(0);
        boolean noNewNorEnclosedExpr = false;

        switch(termSon.toString())
        {
            case "ENCLOSED_EXPR":
                if(termSon.jjtGetNumChildren() != 0)
                {
                    value = evaluatesTo(termSon.jjtGetChild(0), funcName);
                    break;
                }
                else
                {
                    System.out.println("Enclosed expression is childless");
                    return  "error";
                }

            case "NEW":
                if(termSon.jjtGetNumChildren() == 0) //New object
                {
                    value = termSon.getType();
                    break;
                }
                else
                {
                    Node arrayAcces = termSon.jjtGetChild(0);

                    if(analyseArrayAccs(arrayAcces, funcName))
                    {
                        value = "int[]";
                        break;
                    }
                    else
                    {
                        System.out.println("Array initialization failed in function " + funcName);
                        return "error";
                    }
                }

            default:
                noNewNorEnclosedExpr = true;
        }

        int childIndex;

        if(term.jjtGetNumChildren() == 1)
        {
            if(!noNewNorEnclosedExpr)
                return value;
            else
                childIndex = 0;
        }
        else
        {
            if(noNewNorEnclosedExpr)
            {
                System.out.println("Unknown term child in term evaluation in function " + funcName + ": "
                    + termSon.toString());

                return "error";
            }
            else
                childIndex = 1;
        }

        Node termSecondSon = term.jjtGetChild(childIndex);

        switch(termSecondSon.toString())
        {
            case "ArrayAccs":
                if(identifierEvaluatesTo(termSecondSon.jjtGetParent(), funcName, true, true).equals("int[]")
                    && analyseArrayAccs(termSecondSon, funcName))
                    return "int";
                else
                {
                    System.out.println("Array access failed in function " + funcName + ": " + value);
                    return "error";
                }

            case "Member":
                return analyseFunctionCall(termSecondSon, funcName, value);

            default:
                System.out.println("Unexpected term's second son in term evaluation in function " + funcName
                    + ": " + termSecondSon.toString());

                return "error";
        }
    }

    public String analyseFunctionCall(Node member, String funcName, String callerType)
    {
        boolean ownFunc = callerType.equals(className);
        String calledFuncName = member.getName() + "(" + member.jjtGetNumChildren() + ")";
        SymbolTable funcST = symbolTables.get(calledFuncName);

        if(callerType.equals("int") || callerType.equals("boolean"))
        {
            System.out.println("int and boolean types don't have any members: function " + funcName);
            return "error";
        }

        if(callerType.equals("int[]") && member.getName().equals("length"))
            return "int";

        if(funcST == null)
        {
            if(ownFunc)
            {
                if(extendingClass != null)
                    return "all";
                
                System.out.println("Couldn't find class function " + calledFuncName + " in function " + funcName);
                return "error";
            }
            else
                return "all"; //Assume the result is what we wanted
        }

        String[] argProtos = funcST.getArgsList();

        if(argProtos.length != member.jjtGetNumChildren())
        {
            System.out.println("Number of arguments mismatch in member call in function " + funcName + ": Function has " + argProtos.length + " arguments");
            return "error";
        }

        for(int i = 0; i < argProtos.length && i <  member.jjtGetNumChildren(); i++)
        {
            if(!argProtos[i].equals(evaluatesTo(member.jjtGetChild(i), funcName)))
            {
                System.out.println("Call to function " + calledFuncName + " in function " + funcName
                + " doesn't match function prototype");
                return "error";
            }
        }

        return funcST.getReturnType();
    }

    //Returns the type of variable
    public String identifierEvaluatesTo(Node identifier, String funcName, boolean mustBeInit, boolean baseTerm)
    {
        SymbolTable symbolTable = symbolTables.get(funcName);
        Symbol variable;
        boolean arg = false;
        String key;

        if(symbolTable != null)
        {
            variable = symbolTable.getTable().get(identifier.getName());

            if(variable == null)
            {
                variable = symbolTable.getArgs().get(identifier.getName());
                arg = true;

                if(variable == null)
                {
                    arg = false;
                    symbolTable = symbolTables.get(className);
                    key = className;

                    if(symbolTable != null)
                    {
                        variable = symbolTable.getTable().get(identifier.getName());

                        if(variable == null)
                        {
                            if(identifier.jjtGetNumChildren() > 0
                            && identifier.jjtGetChild(0).toString().equals("Member"))
                                return "all";

                            System.out.println("Couldn't find variable " + identifier.getName()
                                + " in function " + funcName);

                            return "error";
                        }
                    }
                    else
                    {
                        System.out.println("Couldn't find class " + className + " symbol table in function" + funcName);
                        return "error";
                    }
                }
                else
                    key = funcName;
            }
            else
                key = funcName;

            if(!variable.getInit())
            {
                if(mustBeInit)
                {
                    System.out.println("Variable " + variable.getName() + " was not initialized in function " + funcName);
                    return "error";
                }
                else
                {
                    variable.setInit(true);

                    if(arg)
                        symbolTable.putArg(variable);
                    else
                        symbolTable.putSymbol(variable);

                    symbolTables.put(key, symbolTable);
                }

            }

            if(!baseTerm && identifier.jjtGetNumChildren() > 0 && identifier.jjtGetChild(0).toString().equals("ArrayAccs")
                    && analyseArrayAccs(identifier.jjtGetChild(0), funcName))
                return "int";
            else
                return variable.getType();
        }
        else
        {
            System.out.println("Couldn't find function " + funcName + " in variable analysis");
            return "error";
        }
    }

    public boolean analyseArrayAccs(Node arrayAcs, String funcName)
    {
        if(arrayAcs.jjtGetNumChildren() != 1)
        {
            System.out.println("Array Access doesn't has expression associated");
            return false;
        }

        return evaluatesTo(arrayAcs.jjtGetChild(0), funcName).equals("int");
    }
}