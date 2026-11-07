package ru.zov.api.system.shape.implement.blur;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.client.texture.GlTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.textures.GpuTexture;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class BlurCapture {
    private GpuTexture captureGpuTexture;
    private int captureGpuW = 0;
    private int captureGpuH = 0;
    private int captureColorGlId = 0;
    private static DownsampleBlur SHARED_BLUR;
    private SimpleFramebuffer normalOutputFb;
    private SimpleFramebuffer strongOutputFb;
    private long captureVersion;
    private long normalBlurVersion = -1L;
    private long strongBlurVersion = -1L;
    private int readFbo;
    private int drawFbo;

    private static DownsampleBlur getSharedBlur() {
        if (SHARED_BLUR == null) {
            SHARED_BLUR = new DownsampleBlur(32856, 5121); // GL_RGBA8, GL_UNSIGNED_BYTE
        }
        return SHARED_BLUR;
    }

    public BlurCapture() {
    }

    // Call this ONCE per frame (e.g., at the start of HUD rendering or menu rendering)
    public void captureScreen() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        Framebuffer fb = client.getFramebuffer();
        if (fb == null) return;
        
        GpuTexture src = fb.getColorAttachment();
        if (src == null) return;
        
        int w = src.getWidth(0);
        int h = src.getHeight(0);
        if (w <= 0 || h <= 0) return;

        try {
            if (this.captureGpuTexture == null || this.captureGpuW != w || this.captureGpuH != h) {
                if (this.captureGpuTexture != null) {
                    this.captureGpuTexture.close();
                }
                this.captureGpuTexture = RenderSystem.getDevice().createTexture(
                    "arizona-blur-capture",
                    GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_SRC,
                    TextureFormat.RGBA8,
                    w,
                    h,
                    1,
                    1
                );
                
                if (this.normalOutputFb != null) {
                    this.normalOutputFb.delete();
                }
                if (this.strongOutputFb != null) {
                    this.strongOutputFb.delete();
                }
                this.normalOutputFb = new SimpleFramebuffer("arizona_blur_normal", w, h, false);
                this.strongOutputFb = new SimpleFramebuffer("arizona_blur_strong", w, h, false);
                
                this.captureGpuW = w;
                this.captureGpuH = h;
            }

            CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
            encoder.copyTextureToTexture(src, this.captureGpuTexture, 0, 0, 0, 0, 0, w, h);
            
            if (this.captureGpuTexture instanceof GlTexture glTex) {
                this.captureColorGlId = glTex.getGlId();
            }
            ++this.captureVersion;
        } catch (Exception e) {
            System.err.println("[Overdozz-Blur] Capture failed: " + e);
            e.printStackTrace();
        }
    }

    // Call this per-widget to get the blurred framebuffer. It caches the result for the same radius.
    public Framebuffer getBlurredFramebuffer(float radiusPx) {
        if (this.captureColorGlId == 0 || this.normalOutputFb == null || this.strongOutputFb == null) {
            return null;
        }

        boolean strong = radiusPx > 60.0F;
        // 30px selects the full-resolution Gaussian path (up to 66 texture
        // reads per pixel). 32px is visually equivalent here but selects the
        // much cheaper downsample/upsample pyramid.
        float effectiveRadius = strong ? 90.0F : 32.0F;
        SimpleFramebuffer outputFb = strong ? this.strongOutputFb : this.normalOutputFb;
        long blurredVersion = strong ? this.strongBlurVersion : this.normalBlurVersion;
        if (blurredVersion == this.captureVersion) {
            return outputFb;
        }

        try {
            int blurredTex = getSharedBlur().blurFromColorTexture(this.captureColorGlId, this.captureGpuW, this.captureGpuH, effectiveRadius, true);
            
            if (blurredTex > 0 && outputFb.getColorAttachment() instanceof GlTexture outGlTex) {
                int outGlId = outGlTex.getGlId();
                
                int oldRead = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
                int oldDraw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
                
                if (this.readFbo == 0) {
                    this.readFbo = GL30.glGenFramebuffers();
                }
                if (this.drawFbo == 0) {
                    this.drawFbo = GL30.glGenFramebuffers();
                }

                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, this.readFbo);
                GL30.glFramebufferTexture2D(GL30.GL_READ_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, blurredTex, 0);
                
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, this.drawFbo);
                GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, outGlId, 0);
                
                GL30.glBlitFramebuffer(0, 0, this.captureGpuW, this.captureGpuH, 0, 0, this.captureGpuW, this.captureGpuH, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
                
                GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, oldRead);
                GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, oldDraw);
                
                if (strong) {
                    this.strongBlurVersion = this.captureVersion;
                } else {
                    this.normalBlurVersion = this.captureVersion;
                }
            }
            return outputFb;
        } catch (Exception e) {
            System.err.println("[Overdozz-Blur] Blur failed: " + e);
            e.printStackTrace();
            return null;
        }
    }

    public void destroy() {
        if (this.captureGpuTexture != null) {
            this.captureGpuTexture.close();
            this.captureGpuTexture = null;
        }
        if (this.normalOutputFb != null) {
            this.normalOutputFb.delete();
            this.normalOutputFb = null;
        }
        if (this.strongOutputFb != null) {
            this.strongOutputFb.delete();
            this.strongOutputFb = null;
        }
        if (this.readFbo != 0) {
            GL30.glDeleteFramebuffers(this.readFbo);
            this.readFbo = 0;
        }
        if (this.drawFbo != 0) {
            GL30.glDeleteFramebuffers(this.drawFbo);
            this.drawFbo = 0;
        }
    }
}
