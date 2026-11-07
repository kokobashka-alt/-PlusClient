package ru.zov.common.util.other;

import com.google.common.collect.Maps;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.common.QuickImports;

public final class BufferUtil implements QuickImports {
   private static final Map<String, Integer> dynamicIdCounters = Maps.newHashMap();

   @Native
   public static NativeImageBackedTexture getImageFromURL(String url) throws IOException {
      return url != null && !url.isEmpty() ? new NativeImageBackedTexture(() -> "arizona-url", parseImage(NativeImage.read((new URL(url)).openStream()))) : null;
   }

   public static NativeImage parseImage(NativeImage image) {
      if (image == null) {
         return null;
      } else {
         int imageWidth = 22;
         int imageHeight = 22;
         int imageSrcWidth = image.getWidth();
         int srcHeight = image.getHeight();

         for(int imageSrcHeight = image.getHeight(); imageWidth < imageSrcWidth || imageHeight < imageSrcHeight; imageHeight *= 2) {
            imageWidth *= 2;
         }

         NativeImage imgNew = new NativeImage(imageWidth, imageHeight, true);

         for(int x = 0; x < imageSrcWidth; ++x) {
            for(int y = 0; y < srcHeight; ++y) {
               imgNew.setColorArgb(x, y, image.getColorArgb(x, y));
            }
         }

         image.close();
         return imgNew;
      }
   }

   public static void registerBufferedImageTexture(Identifier i, BufferedImage bi) {
      try {
         ByteArrayOutputStream baos = new ByteArrayOutputStream();
         ImageIO.write(bi, "png", baos);
         byte[] bytes = baos.toByteArray();
         registerTexture(i, bytes);
      } catch (Exception var4) {
      }

   }

   @Native
   public static void registerTexture(Identifier i, byte[] content) {
      try {
         ByteBuffer data = BufferUtils.createByteBuffer(content.length).put(content);
         data.flip();
          NativeImageBackedTexture tex = new NativeImageBackedTexture(() -> "arizona-dynamic", NativeImage.read(data));
         mc.execute(() -> mc.getTextureManager().registerTexture(i, tex));
      } catch (Exception var4) {
      }

   }

   public static void setWindowIcon(InputStream img16x16, InputStream img32x32) {
      try {
         MemoryStack memorystack = MemoryStack.stackPush();

         try {
            GLFWImage.Buffer buffer = GLFWImage.malloc(2, memorystack);
            List<InputStream> imgList = List.of(img16x16, img32x32);
            List<ByteBuffer> buffers = new ArrayList();

            for(int i = 0; i < imgList.size(); ++i) {
               NativeImage nativeImage = NativeImage.read((InputStream)imgList.get(i));
               ByteBuffer bytebuffer = MemoryUtil.memAlloc(nativeImage.getWidth() * nativeImage.getHeight() * 4);
               bytebuffer.asIntBuffer().put(nativeImage.copyPixelsAbgr());
               buffer.position(i);
               buffer.width(nativeImage.getWidth());
               buffer.height(nativeImage.getHeight());
               buffer.pixels(bytebuffer);
               buffers.add(bytebuffer);
            }

            if (GLFW.glfwGetPlatform() != 393219) {
               GLFW.glfwSetWindowIcon(MinecraftClient.getInstance().getWindow().getHandle(), buffer);
            }

            buffers.forEach(MemoryUtil::memFree);
         } catch (Throwable var10) {
            if (memorystack != null) {
               try {
                  memorystack.close();
               } catch (Throwable var9) {
                  var10.addSuppressed(var9);
               }
            }

            throw var10;
         }

         if (memorystack != null) {
            memorystack.close();
         }
      } catch (Exception var11) {
      }

   }

   public static Identifier registerDynamicTexture(String prefix, NativeImageBackedTexture texture) {
      int integer = (Integer)dynamicIdCounters.getOrDefault(prefix, 0) + 1;
      dynamicIdCounters.put(prefix, integer);
      Identifier identifier = Identifier.ofVanilla(String.format(Locale.ROOT, "dynamic/%s_%d", prefix, integer));
      mc.getTextureManager().registerTexture(identifier, texture);
      return identifier;
   }

   private BufferUtil() {
      throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
   }
}
