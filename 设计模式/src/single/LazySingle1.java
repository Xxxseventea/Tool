package single;

/**
 * 线程不安全
 * 饿汉式，用到时才加载
 */
public class LazySingle1 {
    private static LazySingle1 lazySingle;
    private LazySingle1(){}
    public static LazySingle1 getInstance(){
            if(lazySingle == null){
                lazySingle = new LazySingle1();
            }
        return lazySingle;
    }
}
