package io.github.vimahk;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

class SynchronousMap<K, V> {
    private final ConcurrentHashMap<K, SynchronousQueue<V>> map = new ConcurrentHashMap<>();

    public V get(K key, long timeout, TimeUnit timeUnit) throws InterruptedException {
        SynchronousQueue<V> queue = map.computeIfAbsent(key, k -> new SynchronousQueue<>());
        try {
            return queue.poll(timeout, timeUnit);
        } finally {
            map.remove(key);
        }
    }

    public void insert(K key, V value, long timeout, TimeUnit timeUnit) throws InterruptedException {
        SynchronousQueue<V> queue = map.computeIfAbsent(key, k -> new SynchronousQueue<>());
        try {
            queue.offer(value, timeout, timeUnit);
        } finally {
            map.remove(key);
        }
    }
}