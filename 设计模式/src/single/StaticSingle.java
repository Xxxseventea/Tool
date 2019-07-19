package single;

public class StaticSingle {
    private StaticSingle(){}
    public static StaticSingle getInstance(){
        return SingletoInstance.innstance;
    }
    private static class SingletoInstance{
        static StaticSingle innstance = new StaticSingle();
    }
}
