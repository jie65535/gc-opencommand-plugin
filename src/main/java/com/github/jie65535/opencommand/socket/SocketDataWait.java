/*
 * gc-opencommand
 * Copyright (C) 2022  jie65535
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
