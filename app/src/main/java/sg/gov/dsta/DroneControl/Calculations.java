package sg.gov.dsta.DroneControl;

import com.google.android.gms.maps.model.LatLng;
import static java.lang.Math.PI;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;


public class Calculations {

    //LAT LONG - MGR
    public LatLng convertMGRToCoord(String query) {

        String eastingAsString = query.substring(0, Math.min(query.length(), 4));
        Integer easting = Integer.valueOf(eastingAsString);
        String northingAsString = query.substring(query.length() - 4);
        Integer northing = Integer.valueOf(northingAsString);

        double toLatitude = (((northing) * 10 + 100000 - 130000) / 1000.0 * 0.00904468) + 1.175448228;

        double toLongitude = (((easting) * 10 + 600000 - 620000) / 1000.0 * 0.00898654) + 103.5750001;


        return (new LatLng(toLatitude, toLongitude));

    }

    //MGR - LAT LONG
    public String convertCoordToMGR(double latitude, double longitude) {


        double latToNorthing = ((latitude - 1.175448228) / (0.00904468 / 1000) + 30000) / 10;

        String northingAsString = Integer.toString((int) latToNorthing);

        double longToEasting = ((longitude - 103.5750001) / (0.00898654 / 1000) + 20000) / 10;

        String eastingAsString = Integer.toString((int) longToEasting);

        return (eastingAsString + " " + northingAsString);


    }

    public Double getDistanceFromDroneCamera(Double realHeight, Double imageHeight, Double objectHeight){

        //distance of object (m) = (focal length(m) * real height(m) * image height(pixels)) / (object height (pixels) * sensor height (m))

        Double focalLength = 26.0/1000;
        Double sensorHeight= 4.7/1000;

        return (focalLength * realHeight * imageHeight) / (objectHeight * sensorHeight);
    }

    /**

     * @param aBearing Angle to enemy and Distance and current Lat Lng is used to determine the enemy lat lng
     * @return Lat Long of the enemy location
     */
    LatLng getNewLocation(Double aDistance, Double aBearing, Double currentLat, Double currentLong) {
        double bearingRad = toRadians(aBearing);
        double latRad = toRadians(currentLat);
        double lonRad = toRadians(currentLong);
        int earthRadiusInMetres = 6371000;
        float distFraction = (float) (aDistance / earthRadiusInMetres);

        System.out.println("Bearing Rad: " + bearingRad + " latRad: " + latRad + " lonRad: " + lonRad + " distFrac: " + distFraction);

        double latitudeResult = toDegrees(asin(sin(latRad) * cos(distFraction) + cos(latRad) * sin(distFraction) * cos(bearingRad)));
        double a = atan2(sin(bearingRad) * sin(distFraction) * cos(latRad), cos(distFraction) - sin(latRad) * sin(latitudeResult));
        double longitudeResult = toDegrees((lonRad + a + 3 * PI) % (2 * PI) - PI);

        System.out.println("lat: " + latitudeResult + " long: " + longitudeResult);

        return new LatLng(latitudeResult, longitudeResult);
    }

    /**
     *
     * @param trueHeight
     * @param trueLength
     * @param trueWidth
     * @param cameraPitch provide negative value as per DJI provides
     * @return
     */
    double getPerspectiveHeight(double trueHeight, double trueLength, double trueWidth, double cameraPitch) {

        double pitchRad = toRadians(abs(cameraPitch));

        double a = cos(pitchRad)*trueHeight;
        double b = cos((PI/2)-pitchRad)*(sqrt(trueLength*trueLength*trueWidth*trueWidth));

        return a+b;
    }

    double getGroundDistancefromLOSDistance(double altitude, double cameraPitch, double LOSDistance) {

        double angleRad = toRadians(90+cameraPitch);
        return LOSDistance * sin(angleRad);

    }


}
