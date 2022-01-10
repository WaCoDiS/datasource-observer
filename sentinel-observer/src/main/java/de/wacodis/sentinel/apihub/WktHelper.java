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
package de.wacodis.sentinel.apihub;

import java.util.Locale;

/**
 *
 * @author matthes rieke
 */
public class WktHelper {

    /**
     * generate a WKT Polygon from bounding box coordinates
     * 
     * @param minX
     * @param minY
     * @param maxX
     * @param maxY
     * @return 
     */
    public String fromBoundingBox(double minX, double minY, double maxX, double maxY) {
        // "POLYGON((7.6071 50.9854,6.9315 50.9854,6.9315 51.3190,7.6071 51.3190,7.6071 50.9854,7.6071 50.9854))"
        return String.format(Locale.ROOT, "POLYGON((%.4f %.4f,%.4f %.4f,%.4f %.4f,%.4f %.4f,%.4f %.4f))",
                maxX, minY,
                minX, minY,
                minX, maxY,
                maxX, maxY,
                maxX, minY);
    }
    
}
