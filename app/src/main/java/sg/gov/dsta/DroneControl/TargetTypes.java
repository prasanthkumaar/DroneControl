package sg.gov.dsta.DroneControl;

import java.util.HashMap;

public class TargetTypes {

    //dimensions are in metres

    public enum TargetType {
        Tank(0, "Tank", 3.0, 9.97, 3.75),
        Personnel(1, "Personnel", 1.7, .5, .5),
        Unknown(100, "Unknown", 0.0, 0.0, 0.0);




        public byte value;
        public String text;
        public double height;
        public double length;
        public double width;




        TargetType(int value, String text, Double height, Double length, Double width) {
            this.value = (byte) value;
            this.text = text;
            this.height = height;
            this.length = length;
            this.width = width;

        }

        public String getText() {
            return text;
        }

        public Double getHeight() {
            return height;
        }

        public Double getLength() {
            return length;
        }

        public Double getWidth() {
            return width;
        }

    }


}
