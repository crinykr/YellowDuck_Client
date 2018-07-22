package crinysoft.yellowduck;

public class MyRunnable implements Runnable {
    Object o1;
    Object o2;
    Object o3;

    public MyRunnable() {
    }

    public MyRunnable(Object a) {
        o1 = a;
    }

    public MyRunnable(Object a, Object b) {
        o1 = a;
        o2 = b;
    }

    public MyRunnable(Object a, Object b, Object c) {
        o1 = a;
        o2 = b;
        o3 = c;
    }

    @Override
    public void run() {

    }
}