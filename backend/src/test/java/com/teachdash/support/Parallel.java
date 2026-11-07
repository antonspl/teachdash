package com.teachdash.support;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import io.restassured.response.Response;

public final class Parallel {

	private Parallel() {
	}

	/** Runs all calls at the same moment and returns their responses in the order of the calls. */
	public static List<Response> run(List<Callable<Response>> calls) throws Exception {
		CountDownLatch ready = new CountDownLatch(calls.size());
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(calls.size());
		try {
			List<Future<Response>> futures = new ArrayList<>();
			for (Callable<Response> call : calls) {
				futures.add(executor.submit(() -> {
					ready.countDown();
					start.await();
					return call.call();
				}));
			}
			ready.await();
			start.countDown();
			List<Response> results = new ArrayList<>();
			for (Future<Response> future : futures) {
				results.add(future.get());
			}
			return results;
		} finally {
			executor.shutdownNow();
		}
	}
}
