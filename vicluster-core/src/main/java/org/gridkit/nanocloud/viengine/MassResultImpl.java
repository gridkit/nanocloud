package org.gridkit.nanocloud.viengine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

import org.gridkit.nanocloud.ViExecutor.MassResult;
import org.gridkit.util.concurrent.Box;
import org.gridkit.util.concurrent.FutureEx;

class MassResultImpl<T> implements MassResult<T> {

    private final List<FutureEx<T>> futures;
    private final List<FutureEx<T>> completed = new CopyOnWriteArrayList<>();
    private final CompletableFuture<Void> all = new CompletableFuture<>();
    private final Object mon = new Object();

    public MassResultImpl(List<FutureEx<T>> futures) {
        if (futures.isEmpty()) {
            throw new IllegalArgumentException("Empty future list");
        }
        this.futures = futures;
        for (FutureEx<T> f: futures) {
            final FutureEx<T> tf = f;
            f.addListener(new Box<T>() {

                @Override
                public void setData(T data) {
                    completed.add(tf);
                    if (completed.size() == futures.size()) {
                        all.complete(null);
                    }
                    fnotify();
                }

                @Override
                public void setError(Throwable e) {
                    completed.add(tf);
                    all.completeExceptionally(e);
                    fnotify();
                }
            });
        }
    }

    private void fnotify() {
        synchronized (mon) {
            mon.notify();
        }
    }

    private void fwait(int size) {
        synchronized (mon) {
            while((completed.size() < size)) {
                try {
                    mon.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public T first() {
        fwait(1);
        try {
            return completed.get(0).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<T> all() {
        try {
            all.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            // should never happen
        }
        try {
            List<T> result = new ArrayList<>();
            for (FutureEx<T> f: completed) {
                result.add(f.get());
            }
            return result;
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Iterable<T> results() {
        return new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                // TODO Auto-generated method stub
                return new Iterator<T>() {

                    int n = 0;

                    @Override
                    public boolean hasNext() {
                        return n < futures.size();
                    }

                    @Override
                    public T next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException();
                        }
                        fwait(n + 1);
                        FutureEx<T> r = completed.get(n);
                        n++;
                        try {
                            return r.get();
                        } catch (InterruptedException e) {
                            ExceptionHelper.throwUnchecked(e);
                        } catch (ExecutionException e) {
                            ExceptionHelper.throwUnchecked(e.getCause());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        throw new Error("Unreachable");
                    }
                };
            }
        };
    }

    @Override
    public int size() {
        return futures.size();
    }

    @Override
    public CompletableFuture<T> firstFuture() {
        CompletableFuture<T> cf = new CompletableFuture<T>();
        for (FutureEx<T> f: futures) {
            f.addListener(new Box<T>() {

                @Override
                public void setData(T data) {
                    cf.complete(data);
                }

                @Override
                public void setError(Throwable e) {
                    cf.completeExceptionally(e);
                }
            });
        }
        return cf;
    }

    @Override
    public CompletableFuture<Collection<T>> allFuture() {
        CompletableFuture<Collection<T>> result = all.handle(this::handleAll);
        return result;
    }

    private Collection<T> handleAll(Void value, Throwable e) {
        if (e != null) {
            ExceptionHelper.throwUnchecked(e);
        }
        return all();
    }
}
