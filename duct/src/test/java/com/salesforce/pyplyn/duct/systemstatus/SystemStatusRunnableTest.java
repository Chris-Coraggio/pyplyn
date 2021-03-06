/*
 *  Copyright (c) 2016-2017, Salesforce.com, Inc.
 *  All rights reserved.
 *  Licensed under the BSD 3-Clause license.
 *  For full license text, see the LICENSE.txt file in repo root
 *    or https://opensource.org/licenses/BSD-3-Clause
 */

package com.salesforce.pyplyn.duct.systemstatus;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.stream.Collectors;

import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapFixtures;
import com.salesforce.pyplyn.duct.com.salesforce.pyplyn.test.AppBootstrapLatches;
import com.salesforce.pyplyn.duct.etl.configuration.ConfigurationUpdateManager;
import com.salesforce.pyplyn.model.StatusCode;
import com.salesforce.pyplyn.status.MeterType;
import com.salesforce.pyplyn.status.StatusMessage;
import com.salesforce.pyplyn.status.SystemStatusConsumer;

/**
 * Test class
 *
 * @author Mihai Bojin &lt;mbojin@salesforce.com&gt;
 * @since 5.0
 */
public class SystemStatusRunnableTest {
    private AppBootstrapFixtures fixtures;
    private ArgumentCaptor<MeterType> meterTypeArgumentCaptor;

    @BeforeMethod
    public void setUp() throws Exception {
        // ARRANGE
        fixtures = new AppBootstrapFixtures();
        meterTypeArgumentCaptor = ArgumentCaptor.forClass(MeterType.class);
    }

    /**
     * We can run an ETL flow with a real SystemStatusRunnable,
     * which publishes meter and metrics alerts
     */
    @Test
    public void testRun() throws Exception {
        // ARRANGE
        // app-config params
        fixtures.appConfigMocks()
                .enableAlerts()
                .runOnce()

                // we don't want the LoadFailure meter to fail in this test
                .checkMeter("RefocusLoadFailureWARN", 999.0)
                .checkMeter("RefocusLoadFailureERR", 999.0);

        try {
            // bootstrap
            fixtures.enableLatches()
                    .realSystemStatus()
                    .oneArgusToRefocusConfiguration()
                    .callRealRefocusLoadProcessor()
                    .initializeFixtures()
                    .returnMockedTransformationResultFromAllExtractProcessors();

            // init app and schedule system status thread
            SystemStatusConsumer systemStatusConsumer = fixtures.statusConsumer();
            ConfigurationUpdateManager manager = fixtures.configurationManager();


            // ACT
            manager.run();
            AppBootstrapLatches.holdOffBeforeExtractProcessorStarts().countDown();
            AppBootstrapLatches.holdOffBeforeLoadProcessorStarts().countDown();

            // await until finished
            fixtures.awaitUntilAllTasksHaveBeenProcessed(true);

            // process system status checks
            fixtures.systemStatus().run();


            // ASSERT
            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<StatusMessage>> argumentCaptor = ArgumentCaptor.forClass(List.class);

            // check that meters and timers are initialized
            verify(fixtures.systemStatus(), atLeastOnce()).meter(eq("Refocus"), meterTypeArgumentCaptor.capture());
            assert(meterTypeArgumentCaptor.getValue().processStatus().name().equals("LoadSuccess"));
            verify(fixtures.systemStatus(), atLeastOnce()).timer("Refocus", "upsert-samples-bulk-batched." + AppBootstrapFixtures.MOCK_CONNECTOR_NAME);

            // check that system status was reported to the consumers
            verify(systemStatusConsumer, atLeastOnce()).accept(argumentCaptor.capture());
            List<StatusMessage> messages = argumentCaptor.getValue();

            // check expected output
            assertThat("Expecting one message, status=OK", messages, hasSize(1));
            assertThat("The status should be OK", messages.get(0).level(), is(StatusCode.OK));

            // check that timer messages are being logged
            ArgumentCaptor<StatusMessage> timerMessageCaptor = ArgumentCaptor.forClass(StatusMessage.class);
            verify(fixtures.systemStatus(), atLeastOnce()).logStatusMessage(timerMessageCaptor.capture());
            List<String> timerMessages = timerMessageCaptor.getAllValues().stream().map(Object::toString).collect(Collectors.toList());

            assertThat("AppBootstrapFixtures should report timing data for the Refocus Load processor",
                    timerMessages, hasItem(containsString("Refocus.upsert-samples-bulk-batched.mock-connector")));

        } finally {
            AppBootstrapLatches.release();
        }
    }
}
