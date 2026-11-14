package ru.zov.implement.features.modules.combat.killaura.type;

import java.util.Random;

/**
 * Port of the nexwear "FunTime" rotation (Rotation027) without Minecraft types. The aim point orbits the target's
 * upper body (radius 0.14 to 0.26 blocks, a full turn every 2 to 3 seconds, slow drift, reseeded every 1 to 2
 * seconds). Each tick the look direction moves towards that point with a speed that grows with the angular distance
 * (6 to 95 degrees per tick for yaw, 5 to 75 for pitch), a small random wobble on 30% of the steps, and a 0.65
 * damping once the look direction is within 0.2 of the point. The order of random draws matches the original.
 */
public final class FuntimeOrbitAim {
   public record Frame(
      double playerX, double playerY, double playerZ, double eyeY,
      double targetX, double targetY, double targetZ, double targetHeight,
      float yaw, float pitch, long nowMs
   ) {
   }

   public record Result(float yaw, float pitch) {
   }

   private static final double FALL_MIN = 0.02;
   private static final double FALL_MAX = 0.14;
   private static final double MIN_RADIUS = 0.14;
   private static final double MAX_RADIUS = 0.26;
   private static final float RELEASE_YAW_SPEED = 70.0F;
   private static final float RELEASE_PITCH_SPEED = 50.0F;

   private final Random random;
   private double orbitPhase;
   private double orbitSpeed;
   private double orbitRadius;
   private double heightFactor = 0.85;
   private double verticalBias;
   private long lastReseedTime;
   private double driftX;
   private double driftZ;
   private long lastDriftTime;
   private boolean hasState;

   public FuntimeOrbitAim(long seed) {
      this.random = new Random(seed);
   }

   public void reset() {
      this.orbitPhase = 0.0;
      this.orbitSpeed = 0.0;
      this.orbitRadius = 0.0;
      this.heightFactor = 0.85;
      this.verticalBias = 0.0;
      this.driftX = 0.0;
      this.driftZ = 0.0;
      this.hasState = false;
      this.lastReseedTime = 0;
      this.lastDriftTime = 0;
   }

   public Result aim(Frame f) {
      this.ensureOrbit(f.nowMs());
      this.advancePhase(f);

      double centerX = f.targetX() + this.driftX;
      double centerZ = f.targetZ() + this.driftZ;
      double centerY = f.targetY() + f.targetHeight() * this.heightFactor + this.verticalBias;
      double cos = Math.cos(this.orbitPhase);
      double sin = Math.sin(this.orbitPhase);
      double px = centerX + cos * this.orbitRadius;
      double pz = centerZ + sin * this.orbitRadius;
      double py = centerY + sin * 0.005;

      float desYaw = yawTo(f.playerX(), f.playerZ(), px, pz);
      float desPitch = pitchTo(f.playerX(), f.eyeY(), f.playerZ(), px, py, pz);

      double lookDist = lookDistance(f.playerX(), f.eyeY(), f.playerZ(), px, py, pz, f.yaw(), f.pitch());
      float yawSpeed = speedFromDistance(lookDist, 6.0F, 95.0F);
      float pitchSpeed = speedFromDistance(lookDist, 5.0F, 75.0F);

      yawSpeed *= this.jitter(0.95F, 1.05F);
      pitchSpeed *= this.jitter(0.95F, 1.05F);

      float outYaw = this.step(f.yaw(), desYaw, yawSpeed);
      float outPitch = this.step(f.pitch(), desPitch, pitchSpeed);

      outYaw = wrapDegrees(outYaw);
      outPitch = clamp(outPitch, -89.5F, 89.5F);
      if (lookDistance(f.playerX(), f.eyeY(), f.playerZ(), px, py, pz, outYaw, outPitch) < 0.2) {
         outYaw = soften(f.yaw(), outYaw, 0.65F);
         outPitch = soften(f.pitch(), outPitch, 0.65F);
      }

      this.hasState = true;
      return new Result(outYaw, outPitch);
   }

   /**
    * Target lost: step back towards the camera rotation with the original release speeds (70 and 50 degrees per
    * tick) and forget the orbit once the camera is reached. The original held the last rotation here and left the
    * return to its rotation manager, which this client does not have.
    */
   public Result release(float yaw, float pitch, float cameraYaw, float cameraPitch) {
      float outYaw = wrapDegrees(this.step(yaw, cameraYaw, RELEASE_YAW_SPEED));
      float outPitch = clamp(this.step(pitch, cameraPitch, RELEASE_PITCH_SPEED), -89.5F, 89.5F);
      if (Math.abs(wrapDegrees(cameraYaw - outYaw)) < 0.5F && Math.abs(cameraPitch - outPitch) < 0.5F) {
         this.reset();
      }

      return new Result(outYaw, outPitch);
   }

   private void ensureOrbit(long now) {
      boolean needReseed = this.orbitRadius == 0.0 || now - this.lastReseedTime > this.randomInt(1000, 2000);
      if (!needReseed) {
         return;
      }

      this.heightFactor = 0.78 + this.random.nextDouble() * 0.14;
      this.verticalBias = (this.random.nextDouble() - 0.5) * 0.04;
      this.orbitRadius = MIN_RADIUS + this.random.nextDouble() * (MAX_RADIUS - MIN_RADIUS);

      double periodTicks = 40.0 + this.random.nextDouble() * 20.0;
      this.orbitSpeed = (Math.PI * 2.0) / periodTicks;
      if (this.random.nextBoolean()) {
         this.orbitSpeed = -this.orbitSpeed;
      }

      this.orbitPhase = this.random.nextDouble() * Math.PI * 2.0;
      this.lastReseedTime = now;
      this.driftX = 0.0;
      this.driftZ = 0.0;
      this.lastDriftTime = now;
   }

   private void advancePhase(Frame f) {
      double dx = f.targetX() - f.playerX();
      double dy = f.targetY() - f.playerY();
      double dz = f.targetZ() - f.playerZ();
      double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
      double distFactor = clamp(0.8 + dist * 0.05, 0.8, 1.15);
      this.orbitPhase += this.orbitSpeed * distFactor;

      if (f.nowMs() - this.lastDriftTime > this.randomInt(700, 1300)) {
         this.driftX += (this.random.nextDouble() - 0.5) * 0.02;
         this.driftZ += (this.random.nextDouble() - 0.5) * 0.02;
         this.driftX = clamp(this.driftX, -0.05, 0.05);
         this.driftZ = clamp(this.driftZ, -0.05, 0.05);
         this.lastDriftTime = f.nowMs();
      }

      if (this.random.nextFloat() < 0.12F) {
         this.orbitPhase += (this.random.nextDouble() - 0.5) * 0.05;
      }

      if (this.orbitPhase > Math.PI * 2.0) {
         this.orbitPhase -= Math.PI * 2.0;
      }

      if (this.orbitPhase < 0.0) {
         this.orbitPhase += Math.PI * 2.0;
      }
   }

   static double lookDistance(double eyeX, double eyeY, double eyeZ, double pointX, double pointY, double pointZ, float yaw, float pitch) {
      double dx = pointX - eyeX;
      double dy = pointY - eyeY;
      double dz = pointZ - eyeZ;
      double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
      if (len < 1.0E-4) {
         return 0.0;
      }

      double nx = dx * (1.0 / len);
      double ny = dy * (1.0 / len);
      double nz = dz * (1.0 / len);

      double yawRad = Math.toRadians(yaw);
      double pitchRad = Math.toRadians(pitch);
      double cp = Math.cos(pitchRad);
      double lx = -Math.sin(yawRad) * cp;
      double ly = -Math.sin(pitchRad);
      double lz = Math.cos(yawRad) * cp;
      double llen = Math.sqrt(lx * lx + ly * ly + lz * lz);
      if (llen > 1.0E-6) {
         double inv = 1.0 / llen;
         lx *= inv;
         ly *= inv;
         lz *= inv;
      }

      double ex = lx - nx;
      double ey = ly - ny;
      double ez = lz - nz;
      return Math.sqrt(ex * ex + ey * ey + ez * ez);
   }

   static float speedFromDistance(double dist, float near, float far) {
      double k = 1.0 - Math.exp(-dist * 4.2);
      float base = near + (far - near) * (float)k;
      double mid = Math.exp(-Math.pow(dist - 0.6, 2.0) * 8.0);
      return base + (float)(mid * (far - near) * 0.15);
   }

   private float step(float current, float target, float maxSpeed) {
      float delta = wrapDegrees(target - current);
      if (Math.abs(delta) < 1.0E-4F) {
         return current;
      }

      float step = Math.signum(delta) * Math.min(Math.abs(delta), maxSpeed);
      double fall = FALL_MIN + this.random.nextDouble() * (FALL_MAX - FALL_MIN);
      if (this.random.nextFloat() < 0.3F) {
         double noise = (this.random.nextDouble() * 2.0 - 1.0) * fall;
         step += (float)noise;
         if (Math.abs(step) > Math.abs(delta)) {
            step = delta;
         } else if (Math.signum(step) != Math.signum(delta)) {
            step = 0;
         }
      }

      return current + step;
   }

   private static float soften(float current, float target, float factor) {
      return current + wrapDegrees(target - current) * factor;
   }

   static float yawTo(double fromX, double fromZ, double pointX, double pointZ) {
      return (float)(Math.toDegrees(Math.atan2(pointZ - fromZ, pointX - fromX)) - 90.0);
   }

   static float pitchTo(double fromX, double eyeY, double fromZ, double pointX, double pointY, double pointZ) {
      double dx = pointX - fromX;
      double dy = pointY - eyeY;
      double dz = pointZ - fromZ;
      return (float)(-Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz))));
   }

   private float jitter(float min, float max) {
      return min + this.random.nextFloat() * (max - min);
   }

   private int randomInt(int min, int max) {
      return min + this.random.nextInt(max - min + 1);
   }

   static float wrapDegrees(float degrees) {
      float f = degrees % 360.0F;
      if (f >= 180.0F) {
         f -= 360.0F;
      }

      if (f < -180.0F) {
         f += 360.0F;
      }

      return f;
   }

   private static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : Math.min(v, hi);
   }

   private static double clamp(double v, double lo, double hi) {
      return v < lo ? lo : Math.min(v, hi);
   }
}
