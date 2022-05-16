/*
 * Copyright (C) 2022 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.blowdryer;


import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Response;

public class RateLimitInterceptor implements Interceptor {

	private static int RETRY_MAX_ATTEMPTS = 100;
	private static long RETRY_MS = 100;
	private static long RETRX_MAX_MS = 90_000;

	private int retryAttempts = 0;

	@Override
	public Response intercept(Chain chain) throws IOException {
		Response response = chain.proceed(chain.request());
		if (response.code() == 429 && retryAttempts < RETRY_MAX_ATTEMPTS) {
			long retryAfter = RETRY_MS;
			try {
				retryAfter = Long.parseLong(response.header("Retry-After", "0")) * 1000;
			} catch (NumberFormatException e) {
				// ignore a non parsable header, it might be a date (https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Retry-After)
				// currently only gitlab is tested wich should send seconds in the header
			}
			if (retryAfter <= 0) {
				retryAfter = RETRY_MS;
			}
			if (retryAfter > RETRX_MAX_MS) {
				retryAfter = RETRX_MAX_MS;
			}

			response.close();
			try {
				System.out.println("Blowdryer request to " + chain.request().url() + " has been rate-limited, retrying in " + retryAfter + " milliseconds");
				Thread.sleep(retryAfter);
			} catch (InterruptedException e) {
				throw new IllegalStateException("interrupted while waiting due to rate limiting", e);
			}
			retryAttempts++;
			response = chain.proceed(chain.request());
		}

		return response;
	}
}
