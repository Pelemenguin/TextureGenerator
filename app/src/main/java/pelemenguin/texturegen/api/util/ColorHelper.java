package pelemenguin.texturegen.api.util;

public class ColorHelper {
    
    public static int hsvToRgb(int h, int s, int v) {
        double hue = (h % 360) / 360.0;
        double saturation = Math.max(0, Math.min(100, s)) / 100.0;
        double value = Math.max(0, Math.min(100, v)) / 100.0;

        int r, g, b;

        if (saturation == 0) {
            // 灰色
            r = g = b = (int) Math.round(value * 255);
        } else {
            double hi = Math.floor(hue * 6);
            double f = hue * 6 - hi;
            double p = value * (1 - saturation);
            double q = value * (1 - f * saturation);
            double t = value * (1 - (1 - f) * saturation);

            int region = (int) hi % 6;
            switch (region) {
                case 0:
                    r = (int) Math.round(value * 255);
                    g = (int) Math.round(t * 255);
                    b = (int) Math.round(p * 255);
                    break;
                case 1:
                    r = (int) Math.round(q * 255);
                    g = (int) Math.round(value * 255);
                    b = (int) Math.round(p * 255);
                    break;
                case 2:
                    r = (int) Math.round(p * 255);
                    g = (int) Math.round(value * 255);
                    b = (int) Math.round(t * 255);
                    break;
                case 3:
                    r = (int) Math.round(p * 255);
                    g = (int) Math.round(q * 255);
                    b = (int) Math.round(value * 255);
                    break;
                case 4:
                    r = (int) Math.round(t * 255);
                    g = (int) Math.round(p * 255);
                    b = (int) Math.round(value * 255);
                    break;
                case 5:
                default:
                    r = (int) Math.round(value * 255);
                    g = (int) Math.round(p * 255);
                    b = (int) Math.round(q * 255);
                    break;
            }
        }

        r = Math.max(0, Math.min(255, r));
        g = Math.max(0, Math.min(255, g));
        b = Math.max(0, Math.min(255, b));

        return (r << 16) | (g << 8) | b;
    }

    public static int[] rgbToHsv(int r, int g, int b, int[] result) {
        if (result == null) {
            result = new int[3];
        }

        double rf = r / 255.0;
        double gf = g / 255.0;
        double bf = b / 255.0;

        double max = Math.max(rf, Math.max(gf, bf));
        double min = Math.min(rf, Math.min(gf, bf));
        double delta = max - min;

        double h = 0.0;
        double s = 0.0;
        double v = max;

        if (delta != 0) {
            s = delta / max;

            if (max == rf) {
                h = 60 * ((gf - bf) / delta);
                if (gf < bf) {
                    h += 360;
                }
            } else if (max == gf) {
                h = 60 * ((bf - rf) / delta + 2);
            } else { // max == b
                h = 60 * ((rf - gf) / delta + 4);
            }
        }

        int hInt = (int) Math.round(h);
        if (hInt == 360) hInt = 0;
        int sInt = (int) Math.round(s * 100);
        int vInt = (int) Math.round(v * 100);

        sInt = Math.max(0, Math.min(100, sInt));
        vInt = Math.max(0, Math.min(100, vInt));

        result[0] = hInt;
        result[1] = sInt;
        result[2] = vInt;
        return result;
    }

}
