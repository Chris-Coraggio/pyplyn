/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.client;

import com.salesforce.pyplyn.configuration.EndpointConnector;

import retrofit2.Call;
import retrofit2.http.GET;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 3.0
 */
public class AbstractRemoteClientImpl extends AbstractRemoteClient<AbstractRemoteClientImpl.RetroService> {
    private boolean isAuth;

    public AbstractRemoteClientImpl(EndpointConnector connector, Class<RetroService> cls) {
        super(connector, cls);
    }

    public void setAuth(boolean auth) {
        isAuth = auth;
    }

    @Override
    protected boolean isAuthenticated() {
        return isAuth;
    }

    @Override
    protected boolean auth() throws UnauthorizedException {
        return isAuth;
    }

    @Override
    protected void resetAuth() {
        isAuth = false;
    }

    /**
     * Retrofit interface
     */
    public interface RetroService {
        @GET("/")
        Call<String> get();
    }
}
