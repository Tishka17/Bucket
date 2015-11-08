package com.github.simonpercic.bucket;

import android.content.Context;

import com.github.simonpercic.bucket.utils.StringUtils;
import com.google.gson.Gson;

import java.io.IOException;
import java.lang.reflect.Type;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * @author Simon Percic <a href="https://github.com/simonpercic">https://github.com/simonpercic</a>
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
public final class BucketCache {

    static final String CACHE_DIR = "/Bucket";

    final SimpleDiskCache cache;
    final Gson gson;
    final Scheduler subscribeScheduler;
    final Scheduler observeScheduler;

    private BucketCache(SimpleDiskCache cache, Gson gson, Scheduler subscribeScheduler, Scheduler observeScheduler) {
        this.cache = cache;
        this.gson = gson;
        this.subscribeScheduler = subscribeScheduler;
        this.observeScheduler = observeScheduler;
    }

    // region synchronous methods

    public <T> T get(String key, Type typeOfT) throws IOException {
        checkStringArgumentEmpty(key, "key");
        checkObjectArgumentNull(typeOfT, "typeOfT");

        String json = cache.get(key);

        if (!StringUtils.isEmpty(json)) {
            return gson.fromJson(json, typeOfT);
        }

        return null;
    }

    public void put(String key, Object object) throws IOException {
        checkStringArgumentEmpty(key, "key");
        checkObjectArgumentNull(object, "object");

        String json = gson.toJson(object);
        cache.put(key, json);
    }

    public boolean contains(String key) throws IOException {
        checkStringArgumentEmpty(key, "key");

        return cache.contains(key);
    }

    public void remove(String key) throws IOException {
        checkStringArgumentEmpty(key, "key");

        cache.remove(key);
    }

    public void clear() throws IOException {
        cache.clear();
    }

    // endregion synchronous methods

    // region Reactive methods

    public <T> Observable<T> getRx(final String key, final Type typeOfT) {
        checkStringArgumentEmpty(key, "key");
        checkObjectArgumentNull(typeOfT, "typeOfT");

        return Observable.create(new OnSubscribe<T>() {
            @Override public void call(Subscriber<? super T> subscriber) {
                try {
                    T object = get(key, typeOfT);
                    subscriber.onNext(object);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(subscribeScheduler).observeOn(observeScheduler);
    }

    public Observable<Boolean> putRx(final String key, final Object object) {
        checkStringArgumentEmpty(key, "key");
        checkObjectArgumentNull(object, "object");

        return Observable.create(new OnSubscribe<Boolean>() {
            @Override public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    put(key, object);
                    subscriber.onNext(true);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(subscribeScheduler).observeOn(observeScheduler);
    }

    public Observable<Boolean> containsRx(final String key) {
        checkStringArgumentEmpty(key, "key");

        return Observable.create(new OnSubscribe<Boolean>() {
            @Override public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    boolean contains = contains(key);
                    subscriber.onNext(contains);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(subscribeScheduler).observeOn(observeScheduler);
    }

    public Observable<Boolean> removeRx(final String key) {
        checkStringArgumentEmpty(key, "key");

        return Observable.create(new OnSubscribe<Boolean>() {
            @Override public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    remove(key);
                    subscriber.onNext(true);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(subscribeScheduler).observeOn(observeScheduler);
    }

    public Observable<Boolean> clearRx() {
        return Observable.create(new OnSubscribe<Boolean>() {
            @Override public void call(Subscriber<? super Boolean> subscriber) {
                try {
                    clear();
                    subscriber.onNext(true);
                    subscriber.onCompleted();
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(subscribeScheduler).observeOn(observeScheduler);
    }

    // endregion Reactive methods

    // region private helpers

    private static void checkStringArgumentEmpty(String value, String name) {
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException(name + " is null or empty");
        }
    }

    private static void checkObjectArgumentNull(Object value, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " is null");
        }
    }

    // endregion private helpers

    // region Builder

    public static Builder builder(Context context, long maxSizeBytes) {
        checkObjectArgumentNull(context, "context");

        return new Builder(context.getApplicationContext(), maxSizeBytes);
    }

    public static final class Builder {

        private final Context context;
        private final long maxSizeBytes;

        private Gson gson;

        private Builder(Context context, long maxSizeBytes) {
            this.context = context;
            this.maxSizeBytes = maxSizeBytes;
        }

        public Builder withGson(Gson gson) {
            this.gson = gson;
            return this;
        }

        public synchronized BucketCache build() throws IOException {
            String cachePath = context.getCacheDir() + CACHE_DIR;

            SimpleDiskCache cache = SimpleDiskCache.create(cachePath, maxSizeBytes);

            if (gson == null) {
                gson = new Gson();
            }

            Scheduler subscribeScheduler = Schedulers.io();
            Scheduler observeScheduler = AndroidSchedulers.mainThread();

            return new BucketCache(cache, gson, subscribeScheduler, observeScheduler);
        }
    }

    // endregion Builder
}
