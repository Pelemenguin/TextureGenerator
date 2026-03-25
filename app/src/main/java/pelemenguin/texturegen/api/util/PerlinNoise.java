package pelemenguin.texturegen.api.util;

import java.util.Random;

public class PerlinNoise {

    private static final Random INITIAL_SEED_RANDOM = new Random();
    private long seed;

    private int[] p;

    public PerlinNoise() {
        this(INITIAL_SEED_RANDOM.nextLong());
    }

    public PerlinNoise(long seed) {
        this.seed(seed);
    }

    private void update() {
        Random random = new Random(this.seed);
        int[] perm = new int[512];
        for (int i = 0; i < 256; i++) {
            perm[i] = i;
        }
        for (int i = 0; i < 256; i++) {
            int j = random.nextInt(256);
            int temp = perm[i];
            perm[i] = perm[j];
            perm[j] = temp;
        }
        for (int i = 0; i < 256; i++) {
            perm[i + 256] = perm[i];
        }
        this.p = perm;
    }

    private float fade(float t) {
        return t * t * t * (t * (t * 6 - 15) + 10);
    }

    private float lerp(float a, float b, float t) {
        return a + t * (b - a);
    }

    private float grad(int hash, float x, float y) {
        int h = hash & 3;
        float u = h < 2 ? x : y;
        float v = h < 2 ? y : x;
        return ((h & 1) == 0 ? u : -u) + ((h & 2) == 0 ? v : -v);
    }

    public float rawNoiseValue(float x, float y) {
        int X = (int) Math.floor(x) & 255;
        int Y = (int) Math.floor(y) & 255;

        x -= Math.floor(x);
        y -= Math.floor(y);

        float u = fade(x);
        float v = fade(y);

        int aa = p[p[X] + Y];
        int ab = p[p[X] + Y + 1];
        int ba = p[p[X + 1] + Y];
        int bb = p[p[X + 1] + Y + 1];

        return lerp(lerp(grad(aa, x, y), grad(ba, x - 1, y), u),
                    lerp(grad(ab, x, y - 1), grad(bb, x - 1, y - 1), u), v);
    }

    public float getNoiseValueAt(float x, float y, Iterable<Octave> octaves) {
        float total = 0;
        for (Octave octave : octaves) {
            total += octave.getNoiseValue(this, x, y);
        }
        return total;
    }

    public PerlinNoise seed(long seed) {
        this.seed = seed;
        this.update();
        return this;
    }

    public static class Octave {
        public float amplitude;
        public float frequency;

        public Octave() {
            this(1, 1);
        }

        public Octave(float amplitude, float frequency) {
            this.amplitude = amplitude;
            this.frequency = frequency;
        }

        public float getNoiseValue(PerlinNoise noise, float x, float y) {
            return noise.rawNoiseValue(x * frequency, y * frequency) * amplitude;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + Float.floatToIntBits(amplitude);
            result = prime * result + Float.floatToIntBits(frequency);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Octave other = (Octave) obj;
            if (Float.floatToIntBits(amplitude) != Float.floatToIntBits(other.amplitude))
                return false;
            if (Float.floatToIntBits(frequency) != Float.floatToIntBits(other.frequency))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Octave [amplitude=" + amplitude + ", frequency=" + frequency + "]";
        }

    }

}
