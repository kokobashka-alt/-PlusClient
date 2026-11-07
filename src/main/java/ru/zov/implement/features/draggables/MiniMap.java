package ru.zov.implement.features.draggables;

import com.mojang.blaze3d.systems.RenderSystem;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.Set;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.MapColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Vector2i;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.common.util.render.Render2DUtil;
import ru.zov.common.util.render.ScissorManager;
import ru.zov.common.util.world.ServerUtil;
import ru.zov.implement.events.block.BlockUpdateEvent;
import ru.zov.implement.features.modules.render.Hud;

public class MiniMap extends AbstractDraggable {
   private final ExecutorService executor = Executors.newScheduledThreadPool(1);
   private final Map<Vector2i, Vector2i> map = new ConcurrentHashMap();
   private final Set<Long> pendingColumns = ConcurrentHashMap.newKeySet();
   private final Identifier id = Identifier.of("arizona-minimap");
   private final List<Block> ignore;
   private NativeImage image;
   private NativeImageBackedTexture texture;
   private volatile boolean dirty = true;
   private int textureStartX = Integer.MIN_VALUE;
   private int textureStartZ = Integer.MIN_VALUE;
   private long lastTextureUpdateNanos;

   public static MiniMap getInstance() {
      return (MiniMap)Instance.getDraggable(MiniMap.class);
   }

   public MiniMap() {
      super("Mini Map", 200, 200, 80, 80, true);
      this.ignore = List.of(Blocks.SHORT_GRASS, Blocks.TALL_GRASS);
   }

   public void tick() {
      if (Hud.getInstance().isState() && Hud.getInstance().interfaceSettings.isSelected(this.getName())) {
         int viewDistance = (int)(mc.gameRenderer.getViewDistanceBlocks() + 48.0F) * 2;
         int startX = mc.player.getBlockX() - viewDistance / 2;
         int startZ = mc.player.getBlockZ() - viewDistance / 2;
         boolean resized = this.image == null || this.image.getWidth() != viewDistance || this.image.getHeight() != viewDistance;
         if (!resized && !this.dirty && startX == this.textureStartX && startZ == this.textureStartZ) {
            return;
         }
         long now = System.nanoTime();
         if (!resized && now - this.lastTextureUpdateNanos < 200_000_000L) {
            return;
         }

         if (resized) {
            this.image = new NativeImage(viewDistance, viewDistance, true);
            this.texture = new NativeImageBackedTexture(() -> "arizona-minimap", this.image);
            mc.getTextureManager().registerTexture(this.id, this.texture);
         }

         int width = this.image.getWidth();
         int height = this.image.getHeight();
         this.dirty = false;

         for(int x = 0; x < width; ++x) {
            for(int y = 0; y < height; ++y) {
               this.image.setColorArgb(x, y, ColorUtil.BLACK);
            }
         }

         this.map.forEach((vec2i, vector2i) -> {
            int x = vec2i.x - startX;
            int y = vec2i.y - startZ;
            if (x >= 0 && y >= 0 && x < width && y < height) {
               this.image.setColorArgb(x, y, vector2i.y);
            }

         });
         this.texture.upload();
         this.textureStartX = startX;
         this.textureStartZ = startZ;
         this.lastTextureUpdateNanos = now;
      }

   }

   public void drawDraggable(DrawContext context) {
      org.joml.Matrix3x2fStack matrix = context.getMatrices();
      blur.render(ShapeProperties.create(matrix, (double)this.getX() - (double)2.5F, (double)this.getY() - (double)2.5F, (double)(this.getWidth() + 5), (double)(this.getHeight() + 5)).thickness(2.0F).softness(1.0F).round(4.5F).outlineColor(ColorUtil.getOutline()).color(ColorUtil.getRect(0.5F)).build());
      if (mc.player != null) {
         Vec3d pos = mc.player.getEntityPos().subtract(MathUtil.interpolate(mc.player));
         ScissorManager scissor = main.getScissorManager();
         scissor.push(matrix, (float)this.getX(), (float)this.getY(), (float)this.getWidth(), (float)this.getHeight());
         rectangle.render(ShapeProperties.create(matrix, (double)this.getX(), (double)this.getY(), (double)this.getWidth(), (double)this.getHeight()).round(4.0F).color(ColorUtil.getColor(120, 0.5F)).build());
         matrix.pushMatrix();
         matrix.translate((float)(this.getX() + 40), (float)(this.getY() + 40));
         matrix.rotate((float)Math.toRadians(180.0F + mc.getEntityRenderDispatcher().camera.getYaw()));
         matrix.translate((float)pos.getX(), (float)pos.getZ());
         com.mojang.blaze3d.opengl.GlStateManager._enableBlend();
         com.mojang.blaze3d.opengl.GlStateManager._blendFuncSeparate(772, 773, 1, 0);
         Render2DUtil.drawTexture(matrix, this.id, -80, -80, 160.0F, 160.0F, 160.0F, 160.0F, 160, 160, 160, 160, -1);
         com.mojang.blaze3d.opengl.GlStateManager._disableBlend();
         matrix.popMatrix();
         scissor.pop();
      }

      rectangle.render(ShapeProperties.create(matrix, (double)((float)this.getX() + (float)this.getWidth() / 2.0F - 1.0F), (double)((float)this.getY() + (float)this.getHeight() / 2.0F - 1.0F), (double)2.0F, (double)2.0F).round(1.0F).softness(0.5F).color(ColorUtil.getText()).build());
   }

   public void onPlayerLoad() {
      this.map.clear();
      this.dirty = true;
   }

   public void onBlockUpdate(WorldChunk chunk, BlockUpdateEvent.Type type, BlockPos pos) {
      int startX = chunk.getPos().getStartX();
      int startZ = chunk.getPos().getStartZ();
      switch (type) {
         case LOAD:
            this.scanChunk(chunk, startX, startZ, ServerUtil.getWorldType().equals("the_nether"));
            break;
         case UPDATE:
            Vector2i current = this.map.get(new Vector2i(pos.getX(), pos.getZ()));
            if (current == null || pos.getY() >= current.y) {
               BlockPos startPos = chunk.getPos().getStartPos();
               this.scanY(chunk, startPos.getX(), startPos.getZ(), pos.getX() - startPos.getX(), pos.getZ() - startPos.getZ(), ServerUtil.getWorldType().equals("the_nether"));
            }
            break;
         case UNLOAD:
            for(int x = 0; x < 16; ++x) {
               for(int z = 0; z < 16; ++z) {
                  this.map.remove(new Vector2i(startX | x, startZ | z));
               }
            }
            this.dirty = true;
      }

   }

   private void scanChunk(WorldChunk chunk, int startX, int startZ, boolean nether) {
      ChunkSection[] sections = chunk.getSectionArray().clone();
      int highestSection = chunk.getHighestNonEmptySection();
      int bottomSectionY = chunk.getBottomY() >> 4;
      this.executor.execute(() -> {
         for(int x = 0; x < 16; ++x) {
            for(int z = 0; z < 16; ++z) {
               Vector2i column = new Vector2i(startX | x, startZ | z);
               if (!this.map.containsKey(column)) {
                  float waterBlend = 1.0F;
                  boolean netherAirFound = false;
                  boolean completed = false;

                  for(int sectionIndex = highestSection; sectionIndex >= 0 && !completed; --sectionIndex) {
                     ChunkSection section = sections[sectionIndex];
                     int bottomY = sectionIndex + bottomSectionY << 4;

                  for(int y = 15; y >= 0; --y) {
                     BlockPos pos = new BlockPos(startX | x, bottomY | y, startZ | z);
                        MapResult result = this.putMap(section.getBlockState(x, y, z), pos, column, nether, netherAirFound, waterBlend);
                        netherAirFound = result.netherAirFound;
                        waterBlend = result.waterBlend;
                        if (result.completed) {
                           completed = true;
                           break;
                        }
                     }
                  }
               }
            }
         }
         this.dirty = true;
      });
   }

   private void scanY(WorldChunk chunk, int startX, int startZ, int x, int z, boolean nether) {
      long pendingKey = (long)(startX + x) << 32 ^ (startZ + z) & 0xFFFFFFFFL;
      if (!this.pendingColumns.add(pendingKey)) {
         return;
      }
      ChunkSection[] sections = chunk.getSectionArray().clone();
      int highestSection = chunk.getHighestNonEmptySection();
      int bottomSectionY = chunk.getBottomY() >> 4;
      this.executor.execute(() -> {
         try {
            Vector2i column = new Vector2i(startX + x, startZ + z);
            float waterBlend = 1.0F;
            boolean netherAirFound = false;

            for(int sectionIndex = highestSection; sectionIndex >= 0; --sectionIndex) {
               ChunkSection section = sections[sectionIndex];
               int bottomY = sectionIndex + bottomSectionY << 4;

               for(int y = 15; y >= 0; --y) {
                  BlockPos pos = new BlockPos(startX + x, bottomY + y, startZ + z);
                  MapResult result = this.putMap(section.getBlockState(x, y, z), pos, column, nether, netherAirFound, waterBlend);
                  netherAirFound = result.netherAirFound;
                  waterBlend = result.waterBlend;
                  if (result.completed) {
                     this.dirty = true;
                     return;
                  }
               }
            }
            this.map.remove(column);
            this.dirty = true;
         } finally {
            this.pendingColumns.remove(pendingKey);
         }
      });
   }

   private MapResult putMap(BlockState state, BlockPos pos, Vector2i column, boolean nether, boolean netherAirFound, float waterBlend) {
      if (nether && !netherAirFound) {
         if (pos.getY() < 128 && state.isAir()) {
            netherAirFound = true;
         }
      } else {
         MapColor mapColor = state.getMapColor(mc.world, pos);
         if (mapColor.equals(MapColor.WATER_BLUE)) {
            waterBlend *= 0.75F;
            if (waterBlend <= 0.005F) {
               this.map.put(column, new Vector2i(pos.getY(), ColorUtil.replAlpha(mapColor.color, 1.0F)));
               return new MapResult(true, netherAirFound, waterBlend);
            }
         } else if (!this.ignore.contains(state.getBlock()) && !mapColor.equals(MapColor.CLEAR)) {
            this.map.put(column, new Vector2i(pos.getY(), ColorUtil.replAlpha(ColorUtil.overCol(MapColor.WATER_BLUE.color, mapColor.color, waterBlend), 1.0F)));
            return new MapResult(true, netherAirFound, waterBlend);
         }
      }

      return new MapResult(false, netherAirFound, waterBlend);
   }

   private record MapResult(boolean completed, boolean netherAirFound, float waterBlend) {
   }
}
