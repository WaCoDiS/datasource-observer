package de.wacodis.dwd.cdc;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import de.wacodis.dwd.cdc.model.DwdRequestParamsEncoder;
import de.wacodis.dwd.cdc.model.DwdWfsRequestParams;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DwdWfsRequestParamsEncoderTest {

	private static String version;
	private static String typeName;
	private static List<Float> extent = new ArrayList<Float>();
	private static DateTime startDate;
	private static DateTime endDate;
	private static DwdRequestParamsEncoder encoder;

	@BeforeAll
	static void setup() throws ParseException {
		version = "2.0.0";
		typeName = "CDC:VGSL_FX_MN003";

		extent.add(6.966f);
		extent.add(51.402f);
		extent.add(6.969f);
		extent.add(51.405f);
		
		startDate = DateTime.parse("2019-04-24T01:00:00Z", DwdWfsRequestorBuilder.FORMATTER);
		endDate = DateTime.parse("2019-04-25T10:00:00Z", DwdWfsRequestorBuilder.FORMATTER);
		encoder = new DwdRequestParamsEncoder();

	}

	@DisplayName("Test DWD Params Encoder Method")
	@Test
	void testEncodeParams() throws Exception {
		DwdWfsRequestParams params = encoder.encode(version, typeName, extent, startDate, endDate);

		Assertions.assertEquals(version, params.getVersion());
		Assertions.assertEquals(typeName, params.getTypeName());
		Assertions.assertEquals(startDate, params.getStartDate());
		Assertions.assertEquals(endDate, params.getEndDate());
		Assertions.assertEquals(extent.get(0), params.getEnvelope().getMinLon());
		Assertions.assertEquals(extent.get(1), params.getEnvelope().getMinLat());
		Assertions.assertEquals(extent.get(2), params.getEnvelope().getMaxLon());
		Assertions.assertEquals(extent.get(3), params.getEnvelope().getMaxLat());
	}


}