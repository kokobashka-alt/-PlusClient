package ru.zov.common.util.render;

public final class FogParticleField {
   public static final int MAX_COUNT = 4000;
   public static final float SPREAD = 14.0F;
   public static final float HEIGHT_SPAN = 20.0F;
   public static final float HEIGHT_TOP = 10.0F;

   private static final int SEED_START = 1336530526;
   private static final int SEED_STEP = 374761393;
   private static final int OFFSET_X = -668265263;
   private static final int OFFSET_CURL = 668265263;
   private static final int OFFSET_PHASE = 1336530526;
   private static final int OFFSET_LENGTH = 2004795789;
   private static final int HASH_MUL = 1274126177;
   private static final float HASH_MASK_MAX = 1.6777215E7F;
   private static final float CURL_SCALE = 0.6F;
   private static final float CURL_BIAS = 0.7F;
   private static final float WRAP_SCALE = 0.85F;
   private static final float LENGTH_SCALE = 0.5F;
   private static final float LENGTH_BIAS = 0.55F;

   @FunctionalInterface
   public interface Sink {
      void accept(double x, double yTop, double yBottom, double z);
   }

   private FogParticleField() {
   }

   public static void generate(double cameraX, double cameraY, double cameraZ, int count, float timeSeed, Sink sink) {
      int clamped = Math.max(0, Math.min(MAX_COUNT, count));
      double baseX = Math.floor(cameraX);
      double baseY = Math.floor(cameraY);
      double baseZ = Math.floor(cameraZ);
      int seed = SEED_START;

      for (int i = 0; i < clamped; i++) {
         int hX = hash(seed + OFFSET_X);
         int hZ = (hash(seed) ^ seed) * HASH_MUL;
         int hCurl = hash(seed + OFFSET_CURL);
         int hPhase = hash(seed + OFFSET_PHASE);
         int hLength = hash(seed + OFFSET_LENGTH);

         float rx = unit(hX) - 0.5F;
         double x = (double)((rx + rx) * SPREAD) + baseX;
         float rz = unit(hZ) - 0.5F;
         double z = (double)((rz + rz) * SPREAD) + baseZ;
         float curl = (float)Math.IEEEremainder((double)((unit(hCurl) * CURL_SCALE + CURL_BIAS) * timeSeed * WRAP_SCALE + unit(hPhase)), 1.0);
         double yTop = (double)HEIGHT_TOP - (double)curl * (double)HEIGHT_SPAN + baseY;
         double yBottom = yTop - (double)(unit(hLength) * LENGTH_SCALE + LENGTH_BIAS);

         sink.accept(x, yTop, yBottom, z);
         seed += SEED_STEP;
      }
   }

   static int hash(int value) {
      return (value >>> 13 ^ value) * HASH_MUL;
   }

   static float unit(int hashed) {
      return (float)((hashed >>> 16 ^ hashed) & 16777215) / HASH_MASK_MAX;
   }
}
