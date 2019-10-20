package zeroenv;

public class Sleep {
    public static void blockingMillis(long m){
        try{Thread.sleep(m);}catch(Exception e){}
    }
}
