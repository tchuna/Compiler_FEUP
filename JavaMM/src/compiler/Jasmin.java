package compiler;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.Set;
import java.util.Map.Entry;

import symbol.Symbol;
import symbol.SymbolTable;
import syntax.Node;
import syntax.SimpleNode;

public class Jasmin extends Compiler
{
    private static PrintWriter jWriter;

    public Jasmin(String outputFile)
    {
        jWriter = getJFile(outputFile);
    }

    public void parse(SimpleNode root)
    {
        if(jWriter == null)
            return;

        toJVM(root);

        System.out.println("JVM file generated");
        jWriter.close();
    }

    public PrintWriter getJFile(String outputFile)
    {
        try
        {
            PrintWriter print_writer;

            File jasmin_file = new File(outputFile);

            if(!jasmin_file.exists())
            {
                System.out.println("Couldn't find file, creating new one...");

                jasmin_file = new File(outputFile);
                jasmin_file.createNewFile();
            }
                
            print_writer = new PrintWriter(jasmin_file);

            return print_writer;
        }
        catch(IOException e)
        {
            System.err.println("Couldn't open .j file");
            return null;
        }
    }

    public void toJVM(SimpleNode root)
    {
        Node classNode = root.jjtGetChild(0);
        String extension;

        if(classNode.jjtGetNumChildren() > 0 && classNode.jjtGetChild(0).toString().equals("Extends"))
            extension = classNode.jjtGetChild(0).getName();
        else
            extension = "java/lang/Object";

        jWriter.println(".class public " + className);
        jWriter.println(".super " + extension + "\n");

        Set<Entry<String, Symbol>> varsSet;
        Iterator<Entry<String, Symbol>> varsIterator;
        Entry<String, Symbol> var;

        varsSet = symbolTables.get(className).getTable().entrySet();
        varsIterator = varsSet.iterator();

        while(varsIterator.hasNext())
        {
            var = varsIterator.next();

            jWriter.println(".field '" + var.getKey() + "' " + getJVMType(var.getValue().getType()));
        }


        jWriter.println("\n.method public <init>()V");
        jWriter.println("\taload_0");
        jWriter.println("\tinvokenonvirtual " + extension + "/<init>()V");
        jWriter.println("\treturn");
        jWriter.println(".end method\n");

        for (int i = 0; i < classNode.jjtGetNumChildren(); i++)
        {
            Node child = classNode.jjtGetChild(i);

            switch (child.toString())
            {
                case "Main":
                case "Method":
                    label = 0;
                    functionToJVM(child);
                    break;

                default:
                    continue;
            }

        }
    }

    public int stackNum(Node classNode) {
        // Node child = classNode.jjtGetChild(i);
        int countStack = 1;
        for(int i=0; i<classNode.jjtGetNumChildren(); i++) {

            switch (classNode.jjtGetChild(i).toString())
                {
                    case "Var":
                        countStack++;
                        break;
                    case "ADD":
                        countStack--;
                        break;
                    case "SUB":
                        countStack--;
                        break;
                    case "MUL":
                        countStack--;
                        break;
                    case "DIV":
                        countStack--;
                        break;
                    default:
                        break;
                }
        }
        return countStack*8;
    }

    public void functionToJVM(Node function)
    {
        int children = function.jjtGetNumChildren();

        String funcName = getFunctionName(function);

        int i = 0;

        jWriter.print(".method public ");

        if(funcName.equals("main(1)"))
            jWriter.println("static main([Ljava/lang/String;)V");
        else
        {
            jWriter.print(function.getName() + "(");

            for(; i < children; i++)
            {
                Node arg = function.jjtGetChild(i);

                if(arg.toString().equals("Arg"))
                    jWriter.print(getJVMType(arg.getType()));
                else
                    break;            
            }

            jWriter.print(")" + getJVMType(function.getReturnType()) + "\n");
        }

        SymbolTable function_table = symbolTables.get(funcName);

        if(function_table != null)
        {
            int nLocals = function_table.getTable().size() + function_table.getArgs().size();

            jWriter.println("\t.limit stack " +  999);

            if(!function_table.getReturnType().equals("void"))
                nLocals++;

            jWriter.println("\t.limit locals " + nLocals + "\n");
        }

        for(; i < children; i++)
        {
            Node child = function.jjtGetChild(i);

            switch(child.toString())
            {
                case "Var":
                case "Arg":
                    break;

                case "Return":
                    returnToJVM(child, funcName);
                    break;

                default: //Statement
                    statementToJVM(child, funcName);
            }
        }

        switch (function_table.getReturnType()) 
        {
            case "int":
            case "boolean":
                jWriter.println("\tireturn");
                break;

            case "int[]":
                jWriter.println("\tareturn");
                break;

            case "void":
                jWriter.println("\treturn");
        }

        jWriter.println(".end method\n");
    }

    public void returnToJVM(Node returnNode, String funcName)
    {
        expressionToJVM(returnNode.jjtGetChild(0), funcName, null, false);
    }

    public void getJVMInt(int constant)
    {
        if(constant <= 5)
        {
            jWriter.println("\ticonst_" + constant);
            return;
        }

        if(constant < 128)
        {
            jWriter.println("\tbipush " + constant);
            return;
        }

        if(constant < 32768)
        {
            jWriter.println("\tsipush " + constant);
            return;
        }

        jWriter.println("\tldc " + constant);       
    }

    public String getJVMType(String type)
    {
        switch(type)
        {
            case "int":
                return "I";

            case "boolean":
                return "Z";

            case "int[]":
                return "[I";

            default:
                return type;
        }
    }

    public void statementToJVM(Node statement, String funcName)
    {
        switch (statement.toString())
        {
            case "If":
                ifToJVM(statement, funcName);
                break;

            case "While":
                whileToJVM(statement, funcName);
                break;

            case "TERM":
                termToJVM(statement, funcName, false, true, null, false);
                break;

            case "EQUALS":
                equalsToJVM(statement, funcName);
                break;

            case "Else":
            case "Then":
                break;

            default:
                System.out.println("Unexpected statement type in JVM parsing: " + statement.toString());
        }
    }

    public void ifToJVM(Node ifNode, String funcName)
    {
        String endLabel = generateRandomLabel(funcName), elseLabel = generateRandomLabel(funcName);

        expressionToJVM(ifNode.jjtGetChild(0), funcName, elseLabel, false);

        Node then = ifNode.jjtGetChild(1);

        for(int i = 0; i < then.jjtGetNumChildren(); i++)
            statementToJVM(then.jjtGetChild(i), funcName);

        jWriter.println("\tgoto " + endLabel);
        jWriter.println("\n" + elseLabel + ":");

        Node elseNode = ifNode.jjtGetChild(2);

        for(int i = 0; i < elseNode.jjtGetNumChildren(); i++)
            statementToJVM(elseNode.jjtGetChild(i), funcName);

        jWriter.println("\n" + endLabel + ":");
    }

    public void whileToJVM(Node whileNode, String funcName)
    {
        String conditionLabel = generateRandomLabel(funcName), endLabel = generateRandomLabel(funcName);

        jWriter.println("\n" + conditionLabel + ":");
        expressionToJVM(whileNode.jjtGetChild(0), funcName, endLabel, false);

        Node then = whileNode.jjtGetChild(1);

        for(int i = 0; i < then.jjtGetNumChildren(); i++)
            statementToJVM(then.jjtGetChild(i), funcName);

        jWriter.println("\tgoto " + conditionLabel);
        jWriter.println("\n" + endLabel + ":");
    }

    public void termToJVM(Node term, String funcName, boolean store, boolean loadClass, String conditionalLabel, boolean not)
    {
        String termName = term.getName();
        String value = "";

        if(termName != null)
        {
            switch(termName)
            {
                case "true":
                    jWriter.println("\ticonst_1");
                    value = "boolean";
                    break;

                case "false":
                    jWriter.println("\ticonst_0");
                    value = "boolean";
                    break;

                case "this":
                    jWriter.println("\taload_0");
                    value = className;
                    break;

                default:

                    try
                    {
                        int i = Integer.parseInt(termName); //Integer

                        value = "int";
                        getJVMInt(i);
                    } //Variable
                    catch(NumberFormatException nfe)
                    {
                        value = semantic.identifierEvaluatesTo(term, funcName, true, false);
                        variableToJVM(term, funcName, store, loadClass);
                    }
            }
        }

        if(term.jjtGetNumChildren() == 0)
            return;

        Node termSon = term.jjtGetChild(0);
        boolean noNewNorEnclosedExpr = false, newObject = false;

        switch(termSon.toString())
        {
            case "ENCLOSED_EXPR":
                if(termSon.jjtGetNumChildren() != 0)
                {
                    value = semantic.evaluatesTo(termSon.jjtGetChild(0), funcName);
                    expressionToJVM(termSon.jjtGetChild(0), funcName, conditionalLabel, not);
                    break;
                }

            case "NEW":
                if(termSon.jjtGetNumChildren() == 0) //New object
                {
                    value = termSon.getType();
                    jWriter.println("\tnew " + termSon.getType());
                    jWriter.println("\tdup");
                    jWriter.print("\tinvokenonvirtual ");

                    if(termSon.getType().equals(className))
                        jWriter.println(className + "/<init>()V");
                    else
                        jWriter.println(termSon.getType() + "/" + termSon.getType() + "()V");

                    newObject = true;
                }
                else
                {
                    value = "int[]";

                    expressionToJVM(termSon.jjtGetChild(0).jjtGetChild(0), funcName, null, false);
                    jWriter.println("\tnewarray int");
                }

                break;

            default:
                noNewNorEnclosedExpr = true;
        }

        int childIndex;

        if(term.jjtGetNumChildren() == 1)
        {
            if(!noNewNorEnclosedExpr)
            {
                /*
                if(newObject)
                    jWriter.println("\tpop"); */

                return;
            }
                
            else
                childIndex = 0;
        }
        else
        {
            if(noNewNorEnclosedExpr)
            {
                if(newObject)
                    jWriter.println("\tpop");

                return;
            }
            else
                childIndex = 1;
        }

        Node termSecondSon = term.jjtGetChild(childIndex);

        switch(termSecondSon.toString())
        {
            case "ArrayAccs":
                expressionToJVM(termSecondSon.jjtGetChild(0), funcName, null, false);

                if(!store) 
                {
                    if(term.jjtGetParent().toString().equals("EQUALS") && term.jjtGetParent().jjtGetChild(0) == term)
                        break;
                    else
                        jWriter.println("\tiaload");
                }
                    

                break;

            case "Member":

                boolean staticMember;

                if(value.equals("all"))
                {
                    value = term.getName();
                    staticMember = true;
                }
                else
                    staticMember = false;
                    
                if(termSecondSon.getName().equals("length"))
                    jWriter.println("\tarraylength");
                else
                {
                    functionCallToJVM(termSecondSon, funcName, value, staticMember);

                    /*
                    if(conditionalLabel != null)
                        jWriter.println("\tifeq " + conditionalLabel); */
                }
                    
                

                break;

            default:
                System.out.println("Unexpected term's second son in JVM term evaluation in function " + funcName
                    + ": " + termSecondSon.toString());
        }
    }

    public void functionCallToJVM(Node member, String funcName, String caller, boolean staticMember)
    {
        String[] argTypes;
        boolean localFunc;
        String returnType;

        if(caller.equals(className) || caller.equals(extendingClass))
        {
            SymbolTable funcTable = symbolTables.get(member.getName() + "(" + member.jjtGetNumChildren() + ")");

            if(funcTable != null)
            {
                argTypes = funcTable.getArgsList();
                localFunc = true;
                returnType = getJVMType(funcTable.getReturnType());
            }
            else
            {
                if(caller.equals(className) && extendingClass != null)
                    caller = extendingClass;

                argTypes = new String[member.jjtGetNumChildren()];
                localFunc = false;
                returnType = getExpectedType(member, funcName);
            }
        }
        else
        {
            argTypes = new String[member.jjtGetNumChildren()];
            localFunc = false;
            returnType = getExpectedType(member, funcName);
        }
       
        for(int i = 0; i < member.jjtGetNumChildren(); i++)
        {
            if(!localFunc)
                argTypes[i] = semantic.evaluatesTo(member.jjtGetChild(i), funcName);

            expressionToJVM(member.jjtGetChild(i), funcName, null, false);
        }

        String cmd = "invoke";

        if(staticMember)
            cmd += "static ";
        else
            cmd += "virtual ";

        cmd += caller + "/" + member.getName() + "(";

        for(int i = 0; i < argTypes.length; i++)
        {
            cmd += getJVMType(argTypes[i]);
        }
            

        jWriter.print("\t" + cmd + ")" + returnType + "\n");

        if(returnType.equals("I") || returnType.equals("Z") || returnType.equals("A"))
            pop(member.jjtGetParent());
    }

    public void pop(Node node)
    {
        switch(node.jjtGetParent().toString())
        {
            case "Then":
            case "Else":
            case "Method":
            case "Main":
                jWriter.println("\tpop");
        }
    }

    public String getExpectedType(Node member, String funcName)
    {
        Node memberParent; 
        
        if(member.toString().equals("ENCLOSED_EXPR"))
            memberParent = member.jjtGetParent();
        else
            memberParent = member.jjtGetParent().jjtGetParent();

        switch(memberParent.toString())
        {
            case "ENCLOSED_EXPR":
                return getExpectedType(memberParent, funcName);

            case "MUL":
            case "DIV":
            case "ADD":
            case "SUB":
            case "LOWER":
            case "ArrayAccs":
                return "I";

            case "Member":
                if(symbolTables.get(getFunctionName(memberParent)) != null)
                    for(int i = 0; i < memberParent.jjtGetNumChildren(); i++)
                    {
                        if(memberParent.jjtGetChild(i) == member)
                            return getJVMType(symbolTables.get(getFunctionName(memberParent)).getArgsList()[i]);   
                    }      
                else
                    return "I"; //??

            case "TERM":
                    if(memberParent.jjtGetParent().toString().equals("EQUALS"))
                        return getJVMType(semantic.evaluatesTo(memberParent.jjtGetParent().jjtGetChild(1), funcName));
                    else 
                        return "V";

            case "EQUALS":
                    return getJVMType(semantic.evaluatesTo(memberParent.jjtGetChild(0), funcName));

            case "While":
            case "If":
            case "AND":
                return "Z";

            case "Return":
                    return getJVMType(symbolTables.get(funcName).getReturnType());

            default:
                return "V";
        }
    }

    public void expressionToJVM(Node expression, String funcName, String conditionalLabel, boolean not)
    {
        String label1, label2;

        switch(expression.toString())
        {
            case "ADD":
                expressionToJVM(expression.jjtGetChild(0), funcName, conditionalLabel, false);
                expressionToJVM(expression.jjtGetChild(1), funcName, conditionalLabel, false);
                jWriter.println("\tiadd");
                break;

            case "SUB":
                expressionToJVM(expression.jjtGetChild(0), funcName, conditionalLabel, false);
                expressionToJVM(expression.jjtGetChild(1), funcName, conditionalLabel, false);
                jWriter.println("\tisub");
                break;

            case "DIV":
                expressionToJVM(expression.jjtGetChild(0), funcName, conditionalLabel, false);
                expressionToJVM(expression.jjtGetChild(1), funcName, conditionalLabel, false);
                jWriter.println("\tidiv");
                break;

            case "MUL":
                expressionToJVM(expression.jjtGetChild(0), funcName, conditionalLabel, false);
                expressionToJVM(expression.jjtGetChild(1), funcName, conditionalLabel, false);
                jWriter.println("\timul");
                break;

            case "AND":

                if(conditionalLabel != null)
                    label1 = conditionalLabel;
                else
                    label1 = generateRandomLabel(funcName); 
                
                expressionToJVM(expression.jjtGetChild(0), funcName, conditionalLabel, not);

                if(not)
                    jWriter.println("\tifne " + label1);
                else
                    jWriter.println("\tifeq " + label1);

                expressionToJVM(expression.jjtGetChild(1), funcName, conditionalLabel, not);

                if(not)
                    jWriter.println("\tifne " + label1);
                else
                    jWriter.println("\tifeq " + label1);

                if(conditionalLabel == null)
                {
                    label2 = generateRandomLabel(funcName);

                    jWriter.println("\ticonst_1");
                    jWriter.println("\tgoto " + label2);
                    jWriter.println("\n" + label1 + ":");
                    jWriter.println("\ticonst_0");
                    jWriter.println("\n" + label2 + ":");
                }
                
                break;


            case "LOWER":

                if(conditionalLabel != null)
                    label1 = conditionalLabel;
                else
                    label1 = generateRandomLabel(funcName);  
                
                expressionToJVM(expression.jjtGetChild(0), funcName, conditionalLabel, not);
                expressionToJVM(expression.jjtGetChild(1), funcName, conditionalLabel, not);

                if(not)
                    jWriter.println("\tif_icmplt " + label1);
                else
                    jWriter.println("\tif_icmpge " + label1);

                if(conditionalLabel == null)
                {
                    label2 = generateRandomLabel(funcName);

                    jWriter.println("\ticonst_1");
                    jWriter.println("\tgoto " + label2);
                    jWriter.println("\n" + label1 + ":");
                    jWriter.println("\ticonst_0");
                    jWriter.println("\n" + label2 + ":");
                }
                
                break;

            case "NOT":

                if(conditionalLabel != null)
                    label1 = conditionalLabel;
                else
                    label1 = generateRandomLabel(funcName);

                expressionToJVM(expression.jjtGetChild(0), funcName, conditionalLabel, true);

                if(expression.jjtGetChild(0).toString().equals("TERM"))
                {
                    if(expression.jjtGetChild(0).jjtGetNumChildren() > 0 
                        && expression.jjtGetChild(0).jjtGetChild(0).toString().equals("ENCLOSED_EXPR"))
                    {
                        if(expression.jjtGetChild(0).jjtGetChild(0).jjtGetChild(0).jjtGetNumChildren() <= 1)
                            jWriter.println("\tifne " + label1);  
                    }
                    else
                        jWriter.println("\tifne " + label1);  
                }
                            
                if(conditionalLabel == null)
                {
                    label2 = generateRandomLabel(funcName);

                    jWriter.println("\ticonst_1");
                    jWriter.println("\tgoto " + label2);
                    jWriter.println("\n" + label1 + ":");
                    jWriter.println("\ticonst_0");
                    jWriter.println("\n" + label2 + ":");
                }
                
                break;

            case "TERM":
                
                termToJVM(expression, funcName, false, true, conditionalLabel, not);

                if(conditionalLabel != null && !not 
                    && (expression.jjtGetParent().toString().equals("If") 
                        || expression.jjtGetParent().toString().equals("While")))
                    jWriter.println("\tifeq " + conditionalLabel);
                
                break;

            default:
                System.out.println("Unexpected token to parse to JVM in function " + funcName + ": " + expression.toString());
                return;
        }
    }

    public void equalsToJVM(Node equals, String funcName)
    {
        Node lhs = equals.jjtGetChild(0), expression = equals.jjtGetChild(1), lhsChild;

        //aload_0 if lhs is class member
        //if lhs is ArrayAccs then switch lhs with expression (1st lhs then expression) 

        if(symbolTables.get(className).getTable().get(lhs.getName()) != null)
            jWriter.println("\taload_0");

        if(lhs.jjtGetNumChildren() > 0 && lhs.jjtGetChild(0).toString().equals("ArrayAccs"))
        {
            termToJVM(lhs, funcName, false, false, null, false);
            expressionToJVM(expression, funcName, null, false);
            jWriter.println("\tiastore");
        }
        else
        {
            expressionToJVM(expression, funcName, null, false);
            termToJVM(lhs, funcName, true, false, null, false);
        }
            
        if(lhs.jjtGetNumChildren() > 0)
        {
            lhsChild  = lhs.jjtGetChild(0);
            
            switch(lhsChild.toString())
            {
                case "Member":
                    SymbolTable funcST = symbolTables.get(funcName);
                    Symbol caller;

                    caller = funcST.getTable().get(lhs.getName());

                    if(caller == null)
                        caller = funcST.getArgs().get(lhs.getName());

                    if(caller == null)
                    {
                        funcST = symbolTables.get(className);

                        caller = funcST.getTable().get(lhs.getName());
                    }

                    if(caller != null)
                        functionCallToJVM(lhsChild, funcName, caller.getType(), false);
                    else
                        functionCallToJVM(lhsChild, funcName, lhs.getName(), true);
            }
        }

        jWriter.print("\n");
    }

    public String generateRandomLabel(String funcName)
    {
        label++;

        return funcName + label;
    }

    public void variableToJVM(Node identifier, String funcName, boolean store, boolean loadClass)
    {
        SymbolTable symbolTable = symbolTables.get(funcName);
        Symbol variable;
        String cmd;

        if(symbolTable != null)
        {
            variable = symbolTable.getTable().get(identifier.getName());

            if(variable == null)
            {
                variable = symbolTable.getArgs().get(identifier.getName());

                if(variable == null)
                {
                    symbolTable = symbolTables.get(className);

                    if(symbolTable != null)
                    {
                        variable = symbolTable.getTable().get(identifier.getName());

                        if(variable != null)
                        {
                            if(loadClass)
                                jWriter.println("\taload_0");

                            if(store && !(identifier.jjtGetNumChildren() > 0 
                                && identifier.jjtGetChild(0).toString().equals("ArrayAccs")))
                                jWriter.println("\tputfield " + className + "/" + variable.getName() 
                                    + " " + getJVMType(variable.getType()));
                            else
                                jWriter.println("\tgetfield " + className + "/" + variable.getName() 
                                    + " " + getJVMType(variable.getType()));
                        }
                    }

                    return;
                }
                else
                {
                    if(store)
                        cmd = "store_" + variable.getIndex();
                    else
                        cmd = "load_" + variable.getIndex();
                }
            }
            else
            {
                if(store)
                    cmd = "store " + variable.getIndex();
                else
                    cmd = "load " + variable.getIndex();
            }

            switch(variable.getType())
            {
                case "int":
                case "boolean":
                    cmd = "\ti" + cmd;
                    break;

                default:
                    cmd = "\ta" + cmd;
            }

            jWriter.println(cmd);
        }
    }
}