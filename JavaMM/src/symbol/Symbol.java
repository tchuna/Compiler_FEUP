package symbol;

public class Symbol
{
    private String name;
    private String type;
    private Boolean init = false;
    private int variableIndex;

    public Symbol(String name, String type, int index, boolean init)
    {
        this.name = name;
        this.type = type;
        this.variableIndex = index;
        this.init = init;
    }

    public String getName()
    {
        return name;
    }

    public String getType()
    {
        return type;
    }

    public int getIndex()
    {
        return variableIndex;
    }

    public Boolean getInit()
    {
        return init;
    }

    public void setIndex(int index)
    {
        this.variableIndex = index;
    }

    public void setInit(boolean init)
    {
        this.init = init;
    }
}