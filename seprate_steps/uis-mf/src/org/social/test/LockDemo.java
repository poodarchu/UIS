package org.social.test;

public class LockDemo {
    public static void main(String[] args) {
        MyRunnerVarLock runnerVarLock = new MyRunnerVarLock(new Integer(0));
        MyRunnerFuncLock runnerFuncLock = new MyRunnerFuncLock();
        MyRunnerNoLock runnerNoLock = new MyRunnerNoLock(); 
         
        // 对共享对象进行加锁，线程会依次打印0-99的数，每一次运行的结果都一样
        for(int i = 0; i < 10; i++) {
            Thread thread = new Thread(runnerVarLock);
            thread.start();
        }
         
        // 对共享函数进行加锁，线程会依次打印0-99的数，每一次运行的结果都一样
        for(int i = 0; i < 10; i++) {
            Thread thread = new Thread(runnerFuncLock);
            thread.start();
        }
         
        // 未加锁，会因为线程调用的时序不同而发生变化，每一次运行的结果不一定相同
        for(int i = 0; i < 10; i++) {
            Thread thread = new Thread(runnerNoLock);
            thread.start();
        }
    }
}
 
// 对共享对象进行加锁
class MyRunnerVarLock implements Runnable {
    private Object lock;
 
    public MyRunnerVarLock(Object lock) {
        this.lock = lock;
    }
 
    public void run() {
        synchronized (lock) {
            for (int i = 0; i < 100; i++) {
                System.out.println("Lock: " + i);
            }
        }
    }
}
 
// 对共享函数进行加锁
class MyRunnerFuncLock implements Runnable {
    public synchronized void run() {
        for (int i = 0; i < 100; i++) {
            System.out.println("Func lock: " + i);
        }
    }
}
 
// 没有加锁
class MyRunnerNoLock implements Runnable {
    public void run() {
        for (int i = 0; i < 100; i++) {
            System.out.println("No lock: " + i);
        }
    }
}