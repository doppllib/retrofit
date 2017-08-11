package retrofit2.ios;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Response;
import okhttp3.internal.Util;

/**
 * Created by kgalligan on 12/20/16.
 */

public class IosDispatcher
{
    private int maxRequests = 64;
    private int maxRequestsPerHost = 5;
    private Runnable idleCallback;

    /** Executes calls. Created lazily. */
    private ExecutorService executorService;

    /** Ready async calls in the order they'll be run. */
    private final Deque<IosCall.AsyncCall> readyAsyncCalls = new ArrayDeque<>();

    /** Running asynchronous calls. Includes canceled calls that haven't finished yet. */
    private final Deque<IosCall.AsyncCall> runningAsyncCalls = new ArrayDeque<>();

    /** Running synchronous calls. Includes canceled calls that haven't finished yet. */
    private final Deque<IosCall> runningSyncCalls = new ArrayDeque<>();

    public IosDispatcher(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public IosDispatcher() {
    }

    public synchronized ExecutorService executorService() {
        if (executorService == null) {
            executorService = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), Util.threadFactory("OkHttp Dispatcher", false));
        }
        return executorService;
    }

    /**
     * Set the maximum number of requests to execute concurrently. Above this requests queue in
     * memory, waiting for the running calls to complete.
     *
     * <p>If more than {@code maxRequests} requests are in flight when this is invoked, those requests
     * will remain in flight.
     */
    public synchronized void setMaxRequests(int maxRequests) {
        if (maxRequests < 1) {
            throw new IllegalArgumentException("max < 1: " + maxRequests);
        }
        this.maxRequests = maxRequests;
        promoteCalls();
    }

    public synchronized int getMaxRequests() {
        return maxRequests;
    }

    /**
     * Set the maximum number of requests for each host to execute concurrently. This limits requests
     * by the URL's host name. Note that concurrent requests to a single IP address may still exceed
     * this limit: multiple hostnames may share an IP address or be routed through the same HTTP
     * proxy.
     *
     * <p>If more than {@code maxRequestsPerHost} requests are in flight when this is invoked, those
     * requests will remain in flight.
     */
    public synchronized void setMaxRequestsPerHost(int maxRequestsPerHost) {
        if (maxRequestsPerHost < 1) {
            throw new IllegalArgumentException("max < 1: " + maxRequestsPerHost);
        }
        this.maxRequestsPerHost = maxRequestsPerHost;
        promoteCalls();
    }

    public synchronized int getMaxRequestsPerHost() {
        return maxRequestsPerHost;
    }

    /**
     * Set a callback to be invoked each time the dispatcher becomes idle (when the number of running
     * calls returns to zero).
     *
     * <p>Note: The time at which a {@linkplain okhttp3.Call call} is considered idle is different depending
     * on whether it was run {@linkplain okhttp3.Call#enqueue(okhttp3.Callback) asynchronously} or
     * {@linkplain okhttp3.Call#execute() synchronously}. Asynchronous calls become idle after the
     * {@link okhttp3.Callback#onResponse onResponse} or {@link okhttp3.Callback#onFailure onFailure} callback has
     * returned. Synchronous calls become idle once {@link okhttp3.Call#execute() execute()} returns. This
     * means that if you are doing synchronous calls the network layer will not truly be idle until
     * every returned {@link Response} has been closed.
     */
    public synchronized void setIdleCallback(Runnable idleCallback) {
        this.idleCallback = idleCallback;
    }

    synchronized void enqueue(IosCall.AsyncCall call) {
        if (runningAsyncCalls.size() < maxRequests && runningCallsForHost(call) < maxRequestsPerHost) {
            runningAsyncCalls.add(call);
            executorService().execute(call);
        } else {
            readyAsyncCalls.add(call);
        }
    }

    /**
     * Cancel all calls currently enqueued or executing. Includes calls executed both {@linkplain
     * okhttp3.Call#execute() synchronously} and {@linkplain okhttp3.Call#enqueue asynchronously}.
     */
    public synchronized void cancelAll() {
        for (IosCall.AsyncCall call : readyAsyncCalls) {
            call.get().cancel();
        }

        for (IosCall.AsyncCall call : runningAsyncCalls) {
            call.get().cancel();
        }

        for (IosCall call : runningSyncCalls) {
            call.cancel();
        }
    }

    private void promoteCalls() {
        if (runningAsyncCalls.size() >= maxRequests) return; // Already running max capacity.
        if (readyAsyncCalls.isEmpty()) return; // No ready calls to promote.

        for (Iterator<IosCall.AsyncCall> i = readyAsyncCalls.iterator(); i.hasNext(); ) {
            IosCall.AsyncCall call = i.next();

            if (runningCallsForHost(call) < maxRequestsPerHost) {
                i.remove();
                runningAsyncCalls.add(call);
                executorService().execute(call);
            }

            if (runningAsyncCalls.size() >= maxRequests) return; // Reached max capacity.
        }
    }

    /** Returns the number of running calls that share a host with {@code call}. */
    private int runningCallsForHost(IosCall.AsyncCall call) {
        int result = 0;
        for (IosCall.AsyncCall c : runningAsyncCalls) {
            if (c.host().equals(call.host())) result++;
        }
        return result;
    }

    /** Used by {@code Call#execute} to signal it is in-flight. */
    synchronized void executed(IosCall call) {
        runningSyncCalls.add(call);
    }

    /** Used by {@code AsyncCall#run} to signal completion. */
    void finished(IosCall.AsyncCall call) {
        finished(runningAsyncCalls, call, true);
    }

    /** Used by {@code Call#execute} to signal completion. */
    void finished(IosCall call) {
        finished(runningSyncCalls, call, false);
    }

    private <T> void finished(Deque<T> calls, T call, boolean promoteCalls) {
        int runningCallsCount;
        Runnable idleCallback;
        synchronized (this) {
            if (!calls.remove(call)) throw new AssertionError("Call wasn't in-flight!");
            if (promoteCalls) promoteCalls();
            runningCallsCount = runningCallsCount();
            idleCallback = this.idleCallback;
        }

        if (runningCallsCount == 0 && idleCallback != null) {
            idleCallback.run();
        }
    }

    /** Returns a snapshot of the calls currently awaiting execution. */
    public synchronized List<okhttp3.Call> queuedCalls() {
        List<okhttp3.Call> result = new ArrayList<>();
        for (IosCall.AsyncCall asyncCall : readyAsyncCalls) {
            result.add(asyncCall.get());
        }
        return Collections.unmodifiableList(result);
    }

    /** Returns a snapshot of the calls currently being executed. */
    public synchronized List<okhttp3.Call> runningCalls() {
        List<Call> result = new ArrayList<>();
        result.addAll(runningSyncCalls);
        for (IosCall.AsyncCall asyncCall : runningAsyncCalls) {
            result.add(asyncCall.get());
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized int queuedCallsCount() {
        return readyAsyncCalls.size();
    }

    public synchronized int runningCallsCount() {
        return runningAsyncCalls.size() + runningSyncCalls.size();
    }
}
