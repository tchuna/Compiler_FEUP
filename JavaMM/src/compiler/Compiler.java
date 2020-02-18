package compiler;

import syntax.*;
import symbol.*;
import java.util.Hashtable;
import java.io.FileInputStream;
import java.util.Set;

class Compiler
{
    protected static Hashtable<String, SymbolTable> symbolTables;
    protected static String className;
    protected static int label;
    protected static String extendingClass;
    protected static Semantic semantic;

    public static void main(String[] args) throws Exception
    {
        if (args.length < 2)
        {
            System.out.println("Usage: JavaMMMain <input_filename> <output_filename>");
            System.exit(1);
        }

        symbolTables = new Hashtable<String, SymbolTable>();
        label = 0;

        JavaMM parser = new JavaMM(new FileInputStream(args[0]));
        SimpleNode root = parser.Program();

        System.out.println("Syntax analisys done");

        if(buildSymbolTables(root))
        {
            System.out.println("Symbol tables built");
            //printSymbolTables();

            semantic = new Semantic();

            if(semantic.analyse(root))
                System.out.println("Semantic analysis complete");
            else
            {
                System.out.println("Semantic error");
                return;
            }
                
            Jasmin jasmin = new Jasmin(args[1]);
            jasmin.parse(root);
        }
        else
            System.out.println("Couldn't build symbol tables");
    }

    public static boolean buildSymbolTables(SimpleNode root)
    {
        Node classNode;

        if(root.toString().equals("Program"))
        {
            if(root.jjtGetNumChildren() != 0 && (classNode = root.jjtGetChild(0)).toString().equals("Class"))
            {
                if(classNode.toString().equals("Class"))
                {
                    boolean builtSymbolTable;

                    symbolTables.put(classNode.getName(), new SymbolTable());
                    className = classNode.getName();

                    for(int i = 0; i < classNode.jjtGetNumChildren(); i++)
                    {
                        Node child = classNode.jjtGetChild(i);

                        switch(child.toString())
                        {
                            case "Var":
                                builtSymbolTable = buildLocalSymbolTable(child, classNode, false, i);
                                break;

                            case "Main":
                                builtSymbolTable = buildMainSymbolTable(child, i);
                                break;

                            case "Method":
                                builtSymbolTable = buildFunctionSymbolTable(child);
                                break;

                            case "Extends":
                                extendingClass = child.getName();
                                builtSymbolTable = true;
                                break;

                            default:
                                continue;
                        }

                        if(!builtSymbolTable)
                        {
                            String name = "";

                            if(child.getName() != null)
                                name = child.getName();

                            System.out.println("Error in building symbol table for " + child.toString() + " " + name);
                            return false;
                        }
                            
                    }

                }

            }
            else
            {
                System.out.println("Program doesn't have class");
                return false;
            }

        }
        else
        {
            System.out.println("Root node doesn't qualify as program: " + root.toString());
            return false;
        }

        return true;
    }

    public static boolean buildLocalSymbolTable(Node var, Node parentNode, boolean main, int index)
    {
        SymbolTable classST;
        String name;
        boolean classVar = false;
        
        if(parentNode.toString().equals("Class"))
        {
            name = className;
            classVar = true;
        } 
        else
            name  = getFunctionName(parentNode);

        if(!main)
            classST = symbolTables.get(name);
        else
            classST = symbolTables.get("main(1)");

        if(classST == null)
        {
            System.out.println("Couldn't build local symbol table for " + name);
            return false;
        }

        Symbol newSymbol;

        newSymbol = new Symbol(var.getName(), var.getType(), index, classVar);

        if(var.toString().equals("Arg"))
        {
            if (!classST.putArg(newSymbol))
            {
                System.out.println("Duplicate argument " + var.getName() + " of type " + var.getType());
                return false;
            }
            else
            {
                if(main)
                    symbolTables.put("main(1)", classST);
                else
                    symbolTables.put(getFunctionName(parentNode), classST);

                return true;
            }
        }
        else
        {
            if (!classST.putSymbol(newSymbol))
            {
                System.out.println("Duplicate local variable " + var.getName() + " of type " + var.getType());
                return false;
            }
            else
            {
                if(main)
                    symbolTables.put("main(1)", classST);
                else
                    symbolTables.put(getFunctionName(parentNode), classST);

                return true;
            }
        }
    }

    public static String getFunctionName(Node function)
    {
        String funcName = function.getName() + "(";
        int nArgs = 0;

        if(function.getName().equals("main"))
            return funcName + 1 + ")";

        for(int i = 0; i < function.jjtGetNumChildren(); i++)
            if(function.jjtGetChild(i).toString().equals("Arg"))
                nArgs++;

        return funcName + nArgs + ")";
    }

    public static boolean buildFunctionSymbolTable(Node func)
    {
        String funcName = getFunctionName(func);
        SymbolTable funcTable = symbolTables.get(funcName);

        if(funcTable != null)
        {
            System.out.println("Duplicate function " + funcName);
            return false;
        }
        else
            funcTable = new SymbolTable();

        funcTable.setReturnType(func.getReturnType());
        symbolTables.put(funcName, funcTable);

        for(int i = 0; i < func.jjtGetNumChildren(); i++)
        {
            if(func.jjtGetChild(i).toString().equals("Arg") || func.jjtGetChild(i).toString().equals("Var"))
                if(!buildLocalSymbolTable(func.jjtGetChild(i), func, false, i + 1))
                    return false;
        }

        return true;
    }

    public static boolean buildMainSymbolTable(Node main, int index)
    {
        SymbolTable funcTable = symbolTables.get("main(1)");

        if(funcTable != null)
            return false;
        else
            funcTable = new SymbolTable();

        funcTable.setReturnType("void");
        funcTable.putArg(new Symbol(main.getType(), "String[]", index, false)); //Type in main = argument identifier
        symbolTables.put("main(1)", funcTable);

        for(int i = 0; i < main.jjtGetNumChildren(); i++)
        {
            if(main.jjtGetChild(i).toString().equals("Var"))
                if(!buildLocalSymbolTable(main.jjtGetChild(i), main, true, i + 1))
                    return false;
        }

        return true;
    }

    public static void printSymbolTables()
    {
        Set<String> keys = symbolTables.keySet();
        SymbolTable table;

        for(String key: keys)
        {
            System.out.println("--- " + key + " symbol table ---\n");
            table = symbolTables.get(key);
            table.printArgs();
            table.printTable();
            System.out.println("\n");
        }
    }
}
