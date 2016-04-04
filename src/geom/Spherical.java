/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package geom;

/**
 *
 * @author alvarolb
 */
public class Spherical {

    public static final double R = 6371;
    
    public static double getTurnAngle(LatLng startPoint, LatLng turnPoint, LatLng endPoint, boolean greatCirclePath)
    {
        double turnAngle = 0;
        double startingHeading;
        double targetHeading;
        
        // calculate the track heading based on current position and destination point
        if (greatCirclePath) {
            // this order is ok, final bearing for destination waypoint, start for the next
            startingHeading = Spherical.getFinalBearing(startPoint, turnPoint);
            targetHeading = Spherical.getInitialBearing(turnPoint, endPoint);
        } else {
            startingHeading = Spherical.getRhumbBearing(startPoint, turnPoint);
            targetHeading = Spherical.getRhumbBearing(turnPoint, endPoint);
        }
        turnAngle = targetHeading - startingHeading;
        
        if (turnAngle > 180.0) {
            turnAngle -= 360.0;
        } else if (turnAngle < -180) {
            turnAngle += 360;
        }

        return turnAngle;
    }
    
    public static double getAbsoluteAngle(LatLng startPoint, LatLng turnPoint, LatLng endPoint, boolean greatCirclePath)
    {
         double turnAngle = 0;
       
        double startingHeading;
        double targetHeading;
        
        // calculate the track heading based on current position and destination point
        if (greatCirclePath) {
            // this order is ok, final bearing for destination waypoint, start for the next
            startingHeading = Spherical.getFinalBearing(startPoint, turnPoint);
            targetHeading = Spherical.getInitialBearing(turnPoint, endPoint);
        } else {
            startingHeading = Spherical.getRhumbBearing(startPoint, turnPoint);
            targetHeading = Spherical.getRhumbBearing(turnPoint, endPoint);
        }
        turnAngle = targetHeading - startingHeading;
       
        return 180 - Math.abs(turnAngle%180);
    }

    public static TurnPoints calculateTurnPoints(LatLng startPoint, LatLng turnPoint, LatLng endPoint, double turnRadius)
    {   // calculate turn angle
        double turnAngle = Spherical.getTurnAngle(startPoint, turnPoint, endPoint, false);
        
        // round turnAngle to 6th decimal. Straight lines turn is in order of 10^-12
        turnAngle = Math.round(turnAngle*1000000.0)/1000000.0;
        
        // protect against undesired behavior 
        if(turnRadius==0){
            TurnPoints points = new TurnPoints(turnPoint, turnPoint, turnPoint);
            points.setTurnAngle(turnAngle);
            points.setTurnStartDistance(0);
            points.setTurnEndDistance(0);
            return points;
        }
        
        LatLngLine paral1; 
        LatLngLine paral2;
        
        // check if the turn is to left or right >0 right <0 left
        if(turnAngle>0){
            paral1 = Spherical.getRigthParalelLine(startPoint, turnPoint, turnRadius);
            paral2 = Spherical.getRigthParalelLine(turnPoint, endPoint, turnRadius);
        }
        else
        {
            paral1 = Spherical.getLeftParalelLine(startPoint, turnPoint, turnRadius);
            paral2 = Spherical.getLeftParalelLine(turnPoint, endPoint, turnRadius);
        }
        
        // get the bearing of parallel
        double paralBearing1 = Spherical.getRhumbBearing(paral1.getEndPoint(), paral1.getStartPoint());
        double paralBearing2 = Spherical.getRhumbBearing(paral2.getStartPoint(), paral2.getEndPoint());
        
        // calculate the intersection between parallels
        LatLng turnCenter = Spherical.getIntersectionPoint(paral1.getEndPoint(), paralBearing1, paral2.getStartPoint(), paralBearing2);
        if(turnCenter!=null)
        {
            double intersectionBearing1;
            double intersectionBearing2;
            
            if(turnAngle>0){
                // calculate the intersection points with the original path
                intersectionBearing1 = (paralBearing1 + 90.0) % 360.0;
                intersectionBearing2 = (paralBearing2 + 270.0) % 360.0;    
            }else{
                // calculate the intersection points with the original path
                intersectionBearing1 = (paralBearing1 + 270.0) % 360.0;
                intersectionBearing2 = (paralBearing2 + 90.0) % 360.0;    
            }
            
            // get the start/end turn points
            LatLng startTurn = Spherical.getIntersectionPoint(turnCenter, intersectionBearing1, turnPoint, paralBearing1);
            LatLng endTurn = Spherical.getIntersectionPoint(turnCenter, intersectionBearing2, turnPoint, paralBearing2);
            
            // calculate the distance for start/end turn points
            double startTurnDistance = Spherical.getRhumbDistance(turnPoint, startTurn);
            double endTurnDistance = Spherical.getRhumbDistance(turnPoint, endTurn);
            
            // check that the distance between turn point and turn start doest not return a NaN
            // it may happen when the turn is of 0º
            startTurnDistance = Double.isNaN(startTurnDistance) ? 0 : startTurnDistance;
            endTurnDistance = Double.isNaN(endTurnDistance) ? 0 : endTurnDistance;
            
            // return intersections
            TurnPoints turnPoints = new TurnPoints(turnCenter, startTurn, endTurn);
            turnPoints.setTurnAngle(turnAngle);
            turnPoints.setTurnStartDistance(startTurnDistance);
            turnPoints.setTurnEndDistance(endTurnDistance);
            
            return turnPoints;
        }
        return null;
    }

    public static LatLngLine getRigthParalelLine(LatLng start, LatLng end, double distance) {
        double lineBearing = Spherical.getRhumbBearing(start, end);
        double angle = (lineBearing + 90) % 360;
        LatLng pointStart = Spherical.getDestinationPoint(start, distance, angle);
        LatLng pointEnd = Spherical.getDestinationPoint(end, distance, angle);
        return new LatLngLine(pointStart, pointEnd);
    }

    public static LatLngLine getLeftParalelLine(LatLng start, LatLng end, double distance) {
        double lineBearing = Spherical.getRhumbBearing(start, end);
        double angle = (lineBearing + 270) % 360;
        LatLng pointStart = Spherical.getDestinationPoint(start, distance, angle);
        LatLng pointEnd = Spherical.getDestinationPoint(end, distance, angle);
        return new LatLngLine(pointStart, pointEnd);
    }

    /**
     * Returns the point of intersection of two paths defined by point and
     * bearing
     *
     * see http://williams.best.vwh.net/avform.htm#Intersection
     *
     * @param {LatLon} p1: First point
     * @param {Number} brng1: Initial bearing from first point
     * @param {LatLon} p2: Second point
     * @param {Number} brng2: Initial bearing from second point
     * @returns {LatLon} Destination point (null if no unique intersection
     * defined)
     */
    public static LatLng getIntersectionPoint(LatLng p1, double brng1, LatLng p2, double brng2) {
        double lat1 = Math.toRadians(p1.lat());
        double lon1 = Math.toRadians(p1.lon());
        double lat2 = Math.toRadians(p2.lat());
        double lon2 = Math.toRadians(p2.lon());

        double brng13 = Math.toRadians(brng1);
        double brng23 = Math.toRadians(brng2);

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double dist12 = 2 * Math.asin(Math.sqrt(Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2)));

        if (dist12 == 0) {
            return null;
        }

        // initial/final bearings between points
        double brngA = Math.acos((Math.sin(lat2) - Math.sin(lat1) * Math.cos(dist12)) / (Math.sin(dist12) * Math.cos(lat1)));

        if (Double.isNaN(brngA)) {
            brngA = 0;  // protect against rounding
        }

        double brngB = Math.acos((Math.sin(lat1) - Math.sin(lat2) * Math.cos(dist12)) / (Math.sin(dist12) * Math.cos(lat2)));

        double brng12;
        double brng21;
        if (Math.sin(lon2 - lon1) > 0) {
            brng12 = brngA;
            brng21 = 2 * Math.PI - brngB;
        } else {
            brng12 = 2 * Math.PI - brngA;
            brng21 = brngB;
        }

        double alpha1 = (brng13 - brng12 + Math.PI) % (2 * Math.PI) - Math.PI;  // angle 2-1-3
        double alpha2 = (brng21 - brng23 + Math.PI) % (2 * Math.PI) - Math.PI;  // angle 1-2-3

        if (Math.sin(alpha1) == 0 && Math.sin(alpha2) == 0) {
            return null;  // infinite intersections
        }
        if (Math.sin(alpha1) * Math.sin(alpha2) < 0) {
            return null;       // ambiguous intersection
        }
        //alpha1 = Math.abs(alpha1);
        //alpha2 = Math.abs(alpha2);
        // ... Ed Williams takes abs of alpha1/alpha2, but seems to break calculation?

        double alpha3 = Math.acos(-Math.cos(alpha1) * Math.cos(alpha2)
                + Math.sin(alpha1) * Math.sin(alpha2) * Math.cos(dist12));

        double dist13 = Math.atan2(Math.sin(dist12) * Math.sin(alpha1) * Math.sin(alpha2), Math.cos(alpha2) + Math.cos(alpha1) * Math.cos(alpha3));

        double lat3 = Math.asin(Math.sin(lat1) * Math.cos(dist13) + Math.cos(lat1) * Math.sin(dist13) * Math.cos(brng13));
        double dLon13 = Math.atan2(Math.sin(brng13) * Math.sin(dist13) * Math.cos(lat1), Math.cos(dist13) - Math.sin(lat1) * Math.sin(lat3));
        double lon3 = lon1 + dLon13;
        lon3 = (lon3 + 3 * Math.PI) % (2 * Math.PI) - Math.PI;  // normalise to -180..+180º

        return new LatLng(Math.toDegrees(lat3), Math.toDegrees(lon3));
    }

    // haversine formula
    public static double getGreatCircleDistance(LatLng pos1, LatLng pos2) {
        double dLat = Math.toRadians(pos2.lat() - pos1.lat());
        double dLon = Math.toRadians(pos2.lon() - pos1.lon());

        double lat1 = Math.toRadians(pos1.lat());
        double lat2 = Math.toRadians(pos2.lat());

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0) + Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double d = R * c;

        return d * 1000;
    }

    /*
     public static double getBearing(LatLng pos1, LatLng pos2){
     double dLon = Math.toRadians(pos2.lon()-pos1.lon());

     double lat1 = Math.toRadians(pos1.lat());
     double lat2 = Math.toRadians(pos2.lat());

     double y = Math.sin(dLon) * Math.cos(lat2);
     double x = Math.cos(lat1)*Math.sin(lat2) - Math.sin(lat1)*Math.cos(lat2)*Math.cos(dLon);
     double brng = Math.toDegrees(Math.atan2(y, x));

     return brng;
     }*/
    public static double getFinalBearing(LatLng pos2, LatLng pos1) {
        double dLon = Math.toRadians(pos2.lon() - pos1.lon());

        double lat1 = Math.toRadians(pos1.lat());
        double lat2 = Math.toRadians(pos2.lat());

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double brng = Math.toDegrees(Math.atan2(y, x));

        return (brng + 180) % 360.0;
    }

    /**
     * This formula is for the initial bearing (sometimes referred to as forward
     * azimuth) which if followed in a straight line along a great-circle arc
     * will take you from the start point to the end point
     *
     * @param pos1
     * @param pos2
     * @return
     */
    public static double getInitialBearing(LatLng pos1, LatLng pos2) {
        double dLon = Math.toRadians(pos2.lon() - pos1.lon());

        double lat1 = Math.toRadians(pos1.lat());
        double lat2 = Math.toRadians(pos2.lat());

        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double brng = Math.toDegrees(Math.atan2(y, x));

        return (brng + 360) % 360.0;
    }

    /**
     * Given a start point, initial bearing, and distance, this will calculate
     * the destination point and final bearing travelling along a (shortest
     * distance) great circle arc.
     *
     * @param pos1
     * @param distance
     * @param bearing
     * @return
     */
    public static LatLng getDestinationPoint(LatLng pos1, double distance, double bearing) {

        double lat1 = Math.toRadians(pos1.lat());
        double lon1 = Math.toRadians(pos1.lon());
        double d = distance / 1000.0;
        double brng = Math.toRadians(bearing);

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / R) + Math.cos(lat1) * Math.sin(d / R) * Math.cos(brng));
        double lon2 = lon1 + Math.atan2(Math.sin(brng) * Math.sin(d / R) * Math.cos(lat1), Math.cos(d / R) - Math.sin(lat1) * Math.sin(lat2));

        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    /**
     * Given a start point and a distance d along constant bearing θ, this will
     * calculate the destination point. If you maintain a constant bearing along
     * a rhumb line, you will gradually spiral in towards one of the poles.
     *
     * @param startPoint lat/lon start point
     * @param distance in meteres
     * @param bearing in degrees
     * @return
     */
    public static LatLng getRhumbDestinationPoint(LatLng startPoint, double distance, double bearing) {
        double brng = Math.toRadians(bearing);
        double d = (distance / 1000.0) / R;
        double lat1 = Math.toRadians(startPoint.lat());
        double lon1 = Math.toRadians(startPoint.lon());
        double dLat = d * Math.cos(brng);

        double lat2 = lat1 + dLat;
        double dPhi = Math.log(Math.tan(lat2 / 2 + Math.PI / 4) / Math.tan(lat1 / 2 + Math.PI / 4));
        double q = dPhi != 0 ? dLat / dPhi : Math.cos(lat1);
        double dLon = d * Math.sin(brng) / q;

        // check for some daft bugger going past the pole, normalise latitude if so
        if (Math.abs(lat2) > Math.PI / 2) {
            lat2 = lat2 > 0 ? Math.PI - lat2 : -Math.PI - lat2;
        }

        double lon2 = (lon1 + dLon + Math.PI) % (2 * Math.PI) - Math.PI;

        return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    /**
     * These formula give the (constant) bearing between two points.
     *
     * @param startPoint
     * @param endPoint
     * @return
     */
    public static double getRhumbBearing(LatLng startPoint, LatLng endPoint) {
        double lat1 = Math.toRadians(startPoint.lat());
        double lat2 = Math.toRadians(endPoint.lat());

        double dLat = Math.toRadians(endPoint.lat() - startPoint.lat());
        double dLon = Math.toRadians(endPoint.lon() - startPoint.lon());

        double dPhi = Math.log(Math.tan(Math.PI / 4 + lat2 / 2) / Math.tan(Math.PI / 4 + lat1 / 2));
        double q = dPhi != 0 ? dLat / dPhi : Math.cos(lat1);

        // if dLon over 180° take shorter rhumb across anti-meridian:
        if (Math.abs(dLon) > Math.PI) {
            dLon = dLon > 0 ? -(2 * Math.PI - dLon) : (2 * Math.PI + dLon);
        }

        double d = Math.sqrt(dLat * dLat + q * q * dLon * dLon) * R;
        double brng = Math.toDegrees(Math.atan2(dLon, dPhi));
        return (brng + 360) % 360.0;
    }

    /**
     * These formule give the distance and (constant) bearing between two
     * points.
     *
     * @param startPoint
     * @param endPoint
     * @return
     */
    public static double getRhumbDistance(LatLng startPoint, LatLng endPoint) {
        double lat1 = Math.toRadians(startPoint.lat());
        double lat2 = Math.toRadians(endPoint.lat());

        double dLat = Math.toRadians(endPoint.lat() - startPoint.lat());
        double dLon = Math.toRadians(endPoint.lon() - startPoint.lon());

        double dPhi = Math.log(Math.tan(Math.PI / 4 + lat2 / 2) / Math.tan(Math.PI / 4 + lat1 / 2));
        double q = dPhi != 0 ? dLat / dPhi : Math.cos(lat1);

        // if dLon over 180° take shorter rhumb across anti-meridian:
        if (Math.abs(dLon) > Math.PI) {
            dLon = dLon > 0 ? -(2 * Math.PI - dLon) : (2 * Math.PI + dLon);
        }

        double d = Math.sqrt(dLat * dLat + q * q * dLon * dLon) * R;
        return d * 1000;
    }

    /**
     * Return the distance to horizon in meters with a given altitude
     * @param altitude
     * @return distance to horizon in meters
     */
    public static double getDistanceHorizon(double altitude)
    {
        return Math.sqrt(2*R*1000*altitude/0.8279);
    }
}
