package single;

/**
 * 双锁模式
 * 线程安全且节能
 */

public class DoubleLockSIngle {
    private static DoubleLockSIngle doubleLockSIngle;
    private DoubleLockSIngle(){}
    public static DoubleLockSIngle getInstance(){
        if(doubleLockSIngle == null){
            synchronized (DoubleLockSIngle.class){
                if(doubleLockSIngle == null){
                    doubleLockSIngle = new DoubleLockSIngle();
                }
            }
        }
        return  doubleLockSIngle;
    }
}
