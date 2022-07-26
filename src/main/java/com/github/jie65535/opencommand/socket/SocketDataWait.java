package com.github.jie65535.opencommand.socket;

// 异步等待数据返回
public abstract class SocketDataWait<T> extends Thread {
    public T data;
    public long timeout;
    public long time;
    public String uid;

    /**
     * 异步等待数据返回
     * @param timeout 超时时间
     */
    public SocketDataWait(long timeout) {
        this.timeout = timeout;
        start();
    }

    public abstract void run();

    /**
     * 数据处理
     * @param data 数据
     * @return 处理后的数据
     */
    public abstract T initData(T data);

    /**
     * 超时回调
     */
    public abstract void timeout();

    /**
     * 异步设置数据
     * @param data 数据
     */
    public void setData(Object data) {
        this.data = initData((T) data);
    }

    /**
     * 获取异步数据（此操作会一直堵塞直到获取到数据）
     * @return 数据
     */
    public T getData() {
        while (data == null) {
            try {
                time += 100;
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (time > timeout) {
                timeout();
                return null;
            }
        }
        return data;
    }
}
