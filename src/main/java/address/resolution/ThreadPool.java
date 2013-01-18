package address.resolution;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * <pre>
 * 线程池（:池由一个数组简单实现）。 
 * 对池中的所有线程来说，它们都共享池的工作队列。
 * 如果队列不为空的话，那么线程将反复的执行，来完成工作队列中的每个工作，直到工作队列为空。
 * </pre>
 */
public class ThreadPool {
    
    protected Logger log = Logger.getLogger(this.getClass());
    
    private static ThreadPool threadpool;
    
    static int THREAD_INIT_NUM = 10;
    
    public static ThreadPool getInstance() {
        if( threadpool == null ) {
            threadpool = new ThreadPool(THREAD_INIT_NUM); 
        }
        return threadpool;
    }
    
    static int COUNT = 0; // 使用线程池时，用于多线程环境下的计数
    static int FAULT_COUNT = 0;
    
    public static synchronized void addCount() {
        COUNT ++;
    }
    
    public static synchronized void addFaultCount() {
        FAULT_COUNT ++;
    }

    private final ThreadPoolWorker[] workers;
    private final List<Task> workQueue;

    private ThreadPool(int nThreads) {
        workQueue = Collections.synchronizedList(new LinkedList<Task>());
        
        workers = new ThreadPoolWorker[nThreads];
        for (int i = 0; i < nThreads; i++) {
            workers[i] = new ThreadPoolWorker("worker " + i);
            workers[i].start();
        }
    }

    public void excute(Task task) {
        synchronized (workQueue) {
            workQueue.add(task);
            workQueue.notifyAll();
        }
    }

    public class ThreadPoolWorker extends Thread {
        public ThreadPoolWorker(String name){
            super(name);
        }
        
        public void run() {
            Task task;
            while (true) {
                synchronized (workQueue) {
                    while (workQueue.isEmpty()) {
                        try {
                            workQueue.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                    task = workQueue.remove(0); //将任务从队列中移除
                }
                // 执行任务队列中的任务，由ThreadPool中的一个Worker来执行
                try {
                    if (task != null) {
                        task.excute();
                    }
                } catch (RuntimeException e) {
                } 
            }
        }
    }
}
