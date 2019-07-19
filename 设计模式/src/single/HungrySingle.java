package single;

/**
 * 易产生垃圾对象
 */

public class HungrySingle {
    private static HungrySingle hungrySingle = new HungrySingle();
    private HungrySingle(){}
    public static HungrySingle getInstance(){

              return hungrySingle;
    }
}