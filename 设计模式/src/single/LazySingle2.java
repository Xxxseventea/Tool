package single;

/**
 * 线程安全
 * 饿汉模式
 */
public class LazySingle2 {
    private static LazySingle2 lazySingle2;
    private LazySingle2(){}
    public static synchronized LazySingle2 getInstance(){
        if(lazySingle2 == null){
            lazySingle2 = new LazySingle2();
        }
        return lazySingle2;
    }
}
