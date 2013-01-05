/**
 * Copyright (C) 2012, 2013 Philip W. Sorst <philip@sorst.net>
 * and individual contributors as indicated
 * by the @authors tag.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.dontdrinkandroot.cache;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;
import net.dontdrinkandroot.cache.Cache;
import net.dontdrinkandroot.utils.lang.math.RandomUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class SimulationRunner {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());


	public void runLoadTest(
			final Cache<Serializable, Serializable> cache,
			final int numThreads,
			final int iterations,
			final double alpha) throws Throwable {

		final Set<LoadTestThread> threads = new HashSet<LoadTestThread>();

		for (int threadNumber = 0; threadNumber < numThreads; threadNumber++) {
			final LoadTestThread t = new LoadTestThread(threadNumber, cache, iterations, alpha);
			threads.add(t);
			t.start();
		}

		for (final LoadTestThread thread : threads) {
			thread.join();
			if (thread.getError() != null) {
				throw thread.getError();
			}
		}

	}


	protected void loadTestPostIterationHook(final Cache<Serializable, Serializable> cache) {

	}


	public Set<Long> runKnownTest(
			final Cache<Serializable, Serializable> cache,
			final int numThreads,
			final int iterations,
			final double alpha,
			final Set<Long> known) throws Throwable {

		final Set<Long> synchronizedKnown = Collections.synchronizedSet(known);

		final Set<KnownTestThread> threads = new HashSet<KnownTestThread>();

		for (int tn = 0; tn < numThreads; tn++) {

			final KnownTestThread t = new KnownTestThread(tn, cache, iterations, alpha, synchronizedKnown);
			threads.add(t);
			t.start();
		}

		for (final KnownTestThread t : threads) {
			t.join();
			if (t.getError() != null) {
				throw t.getError();
			}
		}

		return known;

	}


	protected void knownTestpostIterationHook(final Cache<Serializable, Serializable> cache) {

	}


	private String longToKey(final long l) {

		final StringBuffer s = new StringBuffer();

		s.append(Long.toString(l));

		for (int i = 0; i < l % 10; i++) {
			s.append(Long.toString(l));
		}

		return s.toString();
	}


	class LoadTestThread extends Thread {

		private final int num;

		private final Cache<Serializable, Serializable> cache;

		private final int iterations;

		private final double alpha;

		private Throwable t;


		public LoadTestThread(int num, Cache<Serializable, Serializable> cache, int iterations, double alpha) {

			this.num = num;
			this.cache = cache;
			this.iterations = iterations;
			this.alpha = alpha;
		}


		Throwable getError() {

			return this.t;
		}


		@Override
		public void run() {

			try {

				for (int i = 0; i < this.iterations; i++) {

					long id = (int) Math.round(RandomUtils.pareto(this.alpha));
					while (id < 0) {
						id = (int) Math.round(RandomUtils.pareto(this.alpha));
					}
					final int action = (int) Math.round(Math.random() * 10);

					if (action < 9) {

						SimulationRunner.this.logger.info(this.num + ":" + i + " Do get: " + id);
						final String key = SimulationRunner.this.longToKey(id);
						final ExampleObject eo = (ExampleObject) this.cache.get(key);
						if (eo == null) {
							SimulationRunner.this.logger.info(this.num + ":" + i + " Do put: " + id);
							this.cache.put(key, new ExampleObject(id));
						}

					} else {

						SimulationRunner.this.logger.info(this.num + ":" + i + " Do delete: " + id);
						this.cache.delete(SimulationRunner.this.longToKey(id));

					}

					SimulationRunner.this.loadTestPostIterationHook(this.cache);
				}

			} catch (Throwable t) {
				this.t = t;
			}
		}
	}

	class KnownTestThread extends Thread {

		private final int iterations;

		private final Set<Long> known;

		private final int num;

		private final Cache<Serializable, Serializable> cache;

		private final double alpha;

		private Throwable t;


		public KnownTestThread(
				int num,
				Cache<Serializable, Serializable> cache,
				int iterations,
				double alpha,
				Set<Long> known) {

			this.num = num;
			this.cache = cache;
			this.iterations = iterations;
			this.known = known;
			this.alpha = alpha;
		}


		Throwable getError() {

			return this.t;
		}


		@Override
		public void run() {

			for (int i = 0; i < this.iterations; i++) {

				long id = (int) Math.round(RandomUtils.pareto(this.alpha));
				while (id < 0) {
					id = (int) Math.round(RandomUtils.pareto(this.alpha));
				}
				final String key = SimulationRunner.this.longToKey(id);

				try {

					if (this.known.contains(id)) {

						if (Math.random() < .33) {

							SimulationRunner.this.logger.info(this.num + ":" + i + ": Deleting " + id);
							this.known.remove(id);
							this.cache.delete(key);

						} else {

							SimulationRunner.this.logger.info(this.num + ":" + i + ": Getting " + id);
							Assert.assertEquals(new ExampleObject(id), this.cache.get(key));

						}

					} else {

						final ExampleObject eo = new ExampleObject(id);
						SimulationRunner.this.logger.info(this.num + ":" + i + ": Putting " + id);
						this.known.add(id);
						this.cache.put(key, eo);

					}

					SimulationRunner.this.knownTestpostIterationHook(this.cache);

				} catch (final Throwable t) {
					this.t = t;
					return;
				}
			}
		}
	}

}
