package ru.zov.implement.features.modules.combat.killaura.type;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Procedural model of a human aiming hand. Degrees and client ticks (20 Hz), no Minecraft types.
 * It is not a trained network: there is no recorded human data in the project, so every constant
 * below is an uncalibrated assumption that has to be tuned against the target server.
 *
 * Movement is a sum of minimum-jerk submovements. A ballistic submovement with Fitts-law duration
 * and signal-dependent endpoint noise goes first, then intermittent corrective submovements fire
 * after a feedback delay once the perceived error leaves a deadband. The target is perceived with a
 * latency and led by a partial velocity prediction. Reaction time, perception lag, speed and noise
 * are re-rolled per target, so repeated fights do not produce the same trajectory.
 */
public final class NeuroAimModel {
   public record Input(float yaw, float pitch, float aimYaw, float aimPitch, boolean engaged, boolean newTarget, boolean hit) {
   }

   public record Output(float yaw, float pitch) {
   }

   private static final int HISTORY = 4;
   private static final float PEAK_STEP = 24.0F;
   private static final float MAX_STEP = 40.0F;
   private static final float BALLISTIC_MIN = 4.0F;
   private static final float RETARGET_MIN = 8.0F;
   private static final float TRACK_CONTINUOUS = 2.0F;
   private static final float YAW_DEADBAND = 0.35F;
   private static final float PITCH_DEADBAND = 0.6F;
   private static final float RETURN_FINISH = 0.4F;
   private static final float PITCH_NOISE_RATIO = 1.25F;

   private static final class Sub {
      final float ay;
      final float ap;
      final int duration;
      int age;

      Sub(float ay, float ap, int duration) {
         this.ay = ay;
         this.ap = ap;
         this.duration = duration;
      }
   }

   private final Random rnd;
   private final List<Sub> subs = new ArrayList<>(4);
   private final float[] histYaw = new float[HISTORY];
   private final float[] histPitch = new float[HISTORY];
   private int histCount;
   private int waitTicks;
   private int holdTicks;
   private int sinceStart = 99;
   private boolean wasEngaged;
   private float driftYaw;
   private float driftPitch;
   private float speedScale = 1.0F;
   private float noiseScale = 1.0F;
   private int latency = 1;
   private float predictionGain = 0.5F;

   public NeuroAimModel(long seed) {
      this.rnd = new Random(seed);
   }

   public void reset() {
      this.subs.clear();
      this.histCount = 0;
      this.waitTicks = 0;
      this.holdTicks = 0;
      this.sinceStart = 99;
      this.wasEngaged = false;
      this.driftYaw = 0.0F;
      this.driftPitch = 0.0F;
   }

   public Output step(Input in) {
      if (!finite(in.yaw()) || !finite(in.pitch()) || !finite(in.aimYaw()) || !finite(in.aimPitch())) {
         return new Output(in.yaw(), in.pitch());
      }

      boolean engaged = in.engaged();
      if (engaged != this.wasEngaged || in.newTarget()) {
         this.beginPhase(in, engaged);
      }

      this.wasEngaged = engaged;
      this.push(in.aimYaw(), in.aimPitch());
      this.sinceStart++;
      if (this.waitTicks > 0) {
         this.waitTicks--;
      }

      if (this.holdTicks > 0) {
         this.holdTicks--;
      }

      if (in.hit() && engaged) {
         this.onHit();
      }

      if (this.waitTicks == 0) {
         this.plan(in, engaged);
      }

      float dy = 0.0F;
      float dp = 0.0F;
      Iterator<Sub> it = this.subs.iterator();
      while (it.hasNext()) {
         Sub s = it.next();
         float from = smooth((float)s.age / (float)s.duration);
         s.age++;
         float to = smooth((float)s.age / (float)s.duration);
         dy += s.ay * (to - from);
         dp += s.ap * (to - from);
         if (s.age >= s.duration) {
            it.remove();
         }
      }

      if (engaged) {
         float oldY = this.driftYaw;
         float oldP = this.driftPitch;
         this.driftYaw = this.driftYaw * 0.85F + gauss(0.025F * this.noiseScale);
         this.driftPitch = this.driftPitch * 0.85F + gauss(0.02F * this.noiseScale);
         dy += this.driftYaw - oldY + gauss(0.01F);
         dp += this.driftPitch - oldP + gauss(0.008F);
      }

      dy = clamp(dy, -MAX_STEP, MAX_STEP);
      dp = clamp(dp, -MAX_STEP, MAX_STEP);
      return new Output(in.yaw() + dy, clamp(in.pitch() + dp, -90.0F, 90.0F));
   }

   private void beginPhase(Input in, boolean engaged) {
      float err = (float)Math.hypot(wrap(in.aimYaw() - in.yaw()), in.aimPitch() - in.pitch());
      this.histCount = 0;
      this.holdTicks = 0;
      if (engaged) {
         this.rerollPersonality();
         this.waitTicks = err < 2.5F ? 0 : this.reaction(2.5F);
      } else {
         this.waitTicks = this.reaction(3.0F);
      }
   }

   private void rerollPersonality() {
      this.speedScale = (float)Math.exp(0.6 * Math.log(this.speedScale) + 0.4 * 0.10 * this.rnd.nextGaussian());
      this.noiseScale = (float)Math.exp(0.6 * Math.log(this.noiseScale) + 0.4 * 0.25 * this.rnd.nextGaussian());
      this.speedScale = clamp(this.speedScale, 0.8F, 1.25F);
      this.noiseScale = clamp(this.noiseScale, 0.6F, 1.8F);
      this.latency = this.rnd.nextFloat() < 0.7F ? 1 : 2;
      this.predictionGain = 0.6F + 0.3F * this.rnd.nextFloat();
   }

   private int reaction(float medianTicks) {
      float v = medianTicks * (float)Math.exp(0.35 * this.rnd.nextGaussian());
      return Math.max(1, Math.min(7, Math.round(v)));
   }

   private void onHit() {
      if (this.rnd.nextFloat() < 0.45F) {
         this.subs.add(new Sub(gauss(0.25F * this.noiseScale), gauss(0.2F * this.noiseScale), 2));
      }

      this.holdTicks = Math.max(this.holdTicks, 1 + this.rnd.nextInt(2));
   }

   private void push(float aimYaw, float aimPitch) {
      if (this.histCount == 0) {
         for (int i = 0; i < HISTORY; i++) {
            this.histYaw[i] = aimYaw;
            this.histPitch[i] = aimPitch;
         }

         this.histCount = HISTORY;
         return;
      }

      for (int i = HISTORY - 1; i > 0; i--) {
         this.histYaw[i] = this.histYaw[i - 1];
         this.histPitch[i] = this.histPitch[i - 1];
      }

      this.histYaw[0] = aimYaw;
      this.histPitch[0] = aimPitch;
   }

   private void plan(Input in, boolean engaged) {
      float remY = 0.0F;
      float remP = 0.0F;
      for (Sub s : this.subs) {
         float rest = 1.0F - smooth((float)s.age / (float)s.duration);
         remY += s.ay * rest;
         remP += s.ap * rest;
      }

      int lag = engaged ? Math.min(this.latency, this.histCount - 1) : 0;
      float velY = 0.0F;
      float velP = 0.0F;
      if (engaged) {
         velY = clamp(wrap(this.histYaw[0] - this.histYaw[1]), -8.0F, 8.0F);
         velP = clamp(this.histPitch[0] - this.histPitch[1], -4.0F, 4.0F);
      }

      float horizon = lag + 2.0F;
      float errY = wrap(this.histYaw[lag] + this.predictionGain * velY * horizon - (in.yaw() + remY));
      float errP = this.histPitch[lag] + this.predictionGain * velP * horizon - (in.pitch() + remP);
      float dist = (float)Math.hypot(errY, errP);
      boolean busy = !this.subs.isEmpty();

      if (dist >= BALLISTIC_MIN) {
         if (!busy || dist >= RETARGET_MIN && this.sinceStart >= 2) {
            this.startBallistic(errY, errP, dist, engaged);
         }

         return;
      }

      boolean overlap = engaged && dist >= TRACK_CONTINUOUS && this.sinceStart >= 2;
      if (!overlap && (busy || this.holdTicks > 0)) {
         return;
      }

      if (!engaged) {
         if (dist > 0.02F) {
            this.subs.add(new Sub(errY, errP, dist < RETURN_FINISH ? 2 : 3));
            this.sinceStart = 0;
         }

         return;
      }

      float db = this.noiseScale;
      if (Math.abs(errY) > YAW_DEADBAND * db || Math.abs(errP) > PITCH_DEADBAND * db) {
         this.startCorrection(errY, errP, dist);
      }
   }

   private void startBallistic(float errY, float errP, float dist, boolean engaged) {
      float gain;
      float sigma;
      if (engaged) {
         float mean = 1.0F - 0.04F * Math.min(1.0F, dist / 60.0F);
         gain = clamp(mean + gauss(0.035F), 0.88F, 1.08F);
         sigma = 0.04F * dist * this.noiseScale + 0.1F;
      } else {
         gain = 1.0F + gauss(0.01F);
         sigma = 0.01F * dist;
      }

      float ay = errY * gain + gauss(sigma);
      float ap = errP * gain + gauss(sigma * PITCH_NOISE_RATIO);
      float fitts = (2.0F + 1.1F * log2(1.0F + dist / 4.0F)) / this.speedScale * (float)Math.exp(0.12 * this.rnd.nextGaussian());
      int duration = Math.max(3, Math.round(Math.max(fitts, 1.875F * dist / PEAK_STEP)));
      this.subs.add(new Sub(ay, ap, duration));
      this.sinceStart = 0;
      this.holdTicks = duration + this.latency + this.rnd.nextInt(2);
   }

   private void startCorrection(float errY, float errP, float dist) {
      float gain = 0.75F + 0.25F * this.rnd.nextFloat();
      float sigma = 0.04F * dist * this.noiseScale + 0.03F;
      int duration = (dist < 1.5F ? 2 : 3) + this.rnd.nextInt(2);
      this.subs.add(new Sub(errY * gain + gauss(sigma), errP * gain + gauss(sigma * PITCH_NOISE_RATIO), duration));
      this.sinceStart = 0;
      this.holdTicks = duration + 1 + this.rnd.nextInt(3);
   }

   private float gauss(float sigma) {
      return (float)this.rnd.nextGaussian() * sigma;
   }

   static float smooth(float t) {
      float x = Math.max(0.0F, Math.min(1.0F, t));
      return x * x * x * (10.0F - 15.0F * x + 6.0F * x * x);
   }

   static float wrap(float degrees) {
      float d = degrees % 360.0F;
      if (d >= 180.0F) {
         d -= 360.0F;
      }

      if (d < -180.0F) {
         d += 360.0F;
      }

      return d;
   }

   private static float log2(float v) {
      return (float)(Math.log(v) / Math.log(2.0));
   }

   private static float clamp(float v, float lo, float hi) {
      return v < lo ? lo : Math.min(v, hi);
   }

   private static boolean finite(float v) {
      return !Float.isNaN(v) && !Float.isInfinite(v);
   }
}
