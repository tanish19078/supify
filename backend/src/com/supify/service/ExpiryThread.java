package com.supify.service;

// Background daemon thread checking expired badges on the shared SupifyPlatform
public class ExpiryThread extends Thread {
    private final SupifyPlatform platform;
    private final int delayMs;
    private volatile boolean running = true;

    public ExpiryThread(SupifyPlatform platform) {
        this(platform, 4000);
    }

    public ExpiryThread(SupifyPlatform platform, int delayMs) {
        super("ExpiryThread");
        this.platform = platform;
        this.delayMs = delayMs;
        setDaemon(true);
    }

    @Override
    public void run() {
        while (running) {
            try {
                platform.checkExpiredAttestations();
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    public void stopMonitor() {
        running = false;
        interrupt();
    }

    public void printThreadInfo() {
        Thread current = Thread.currentThread();
        System.out.println("\n--- Active App Threads ---");
        System.out.println(current.getName() + " | " + current.getState() + " | daemon=" + current.isDaemon());
        System.out.println(getName() + " | " + getState() + " | daemon=" + isDaemon());
    }
}
