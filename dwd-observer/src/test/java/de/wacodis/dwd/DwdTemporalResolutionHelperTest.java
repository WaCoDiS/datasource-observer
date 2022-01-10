/*
 * Copyright 2018-2022 52°North Initiative for Geospatial Open Source
 * Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.wacodis.dwd;

import java.util.List;

import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DwdTemporalResolutionHelperTest {

	private static DwdTemporalResolutionHelper resolutionHelper;

	@BeforeAll
	static void init(){
		resolutionHelper = new DwdTemporalResolutionHelper();
	}


    @Test
    void testCalculateRequestIntervalsForHourlyResolution() {
		DateTime startDate = DateTime.parse("2019-09-01T00:00Z");
		DateTime endDate = DateTime.parse("2019-09-30T00:00Z");
        List<DateTime[]> intervals = resolutionHelper.calculateRequestIntervalsForResolution(startDate, endDate,
                DwdTemporalResolutionHelper.LayerTimeResolution.HOURLY_RESOLUTION);

        Assertions.assertEquals(29, intervals.size());

        Assertions.assertEquals(startDate.toString(), intervals.get(0)[0].toString());
		Assertions.assertEquals(startDate.plusDays(1).toString(), intervals.get(0)[1].toString());

		Assertions.assertEquals(intervals.get(0)[1].toString(), intervals.get(1)[0].toString());

		Assertions.assertEquals(endDate.toString(), intervals.get(intervals.size()-1)[1].toString());
    }

	@Test
	void testCalculateRequestIntervalsForDailyResolution() {
		DateTime startDate = DateTime.parse("2019-09-01T00:00Z");
		DateTime endDate = DateTime.parse("2019-10-01T00:00Z");
		List<DateTime[]> intervals = resolutionHelper.calculateRequestIntervalsForResolution(startDate, endDate,
				DwdTemporalResolutionHelper.LayerTimeResolution.DAILY_RESOLUTION);

		Assertions.assertEquals(5, intervals.size());

		Assertions.assertEquals(startDate.toString(), intervals.get(0)[0].toString());
		Assertions.assertEquals(startDate.plusWeeks(1).toString(), intervals.get(0)[1].toString());

		Assertions.assertEquals(intervals.get(0)[1].toString(), intervals.get(1)[0].toString());

		Assertions.assertEquals(endDate.toString(), intervals.get(intervals.size()-1)[1].toString());
	}

	@Test
	void testCalculateRequestIntervalsIfResolutionIsGreaterThanTargetTimeFrame() {
		DateTime startDate = DateTime.parse("2019-09-01T00:00Z");
		DateTime endDate = DateTime.parse("2019-09-02T00:00Z");
		List<DateTime[]> intervals = resolutionHelper.calculateRequestIntervalsForResolution(startDate, endDate,
				DwdTemporalResolutionHelper.LayerTimeResolution.DAILY_RESOLUTION);

		Assertions.assertEquals(1, intervals.size());

		Assertions.assertEquals(startDate.toString(), intervals.get(0)[0].toString());
		Assertions.assertEquals(endDate.toString(), intervals.get(intervals.size()-1)[1].toString());
	}

}
