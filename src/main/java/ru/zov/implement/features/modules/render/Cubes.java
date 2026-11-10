package ru.zov.implement.features.modules.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tessellator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import ru.zov.api.event.EventHandler;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.other.Instance;
import ru.zov.implement.events.player.TickEvent;
import ru.zov.implement.events.render.WorldRenderEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Cubes extends Module {

    public static Cubes INSTANCE;
    private static final Identifier GLOW_TEX = Identifier.of("zov", "textures/particle/bloom.png");
    private static final float SPAWN_RADIUS = 12.0F;
    private static final float MAX_RENDER_DISTANCE_SQ = 900.0F;

    public final SelectSetting animation = new SelectSetting("Анимация", "Тип движения")
            .value("Разлет", "Падение").selected("Разлет");
    public final SelectSetting shape = new SelectSetting("Форма", "Форма частиц")
            .value("Кубы", "Треугольники").selected("Кубы");
    public final ValueSetting count = new ValueSetting("Количество", "Сколько частиц")
            .setValue(30.0f).range(5.0f, 100.0f);
    public final ValueSetting size = new ValueSetting("Размер", "Размер частиц")
            .setValue(1.0f).range(0.1f, 3.0f);
    public final ValueSetting speed = new ValueSetting("Скорость", "Скорость движения")
            .setValue(1.0f).range(0.1f, 5.0f);

    private final List<CubeParticle> cubes = new ArrayList<>();
    private final List<CubeParticle> visibleCubes = new ArrayList<>();
    private final Random random = new Random();
    private int updateCounter = 0;

    public Cubes() {
        super("Cubes", "3D Кубы по миру", ModuleCategory.RENDER);
        this.setup(new Setting[]{animation, shape, count, size, speed});
        INSTANCE = this;
    }

    public static Cubes getInstance() {
        return (Cubes) Instance.get(Cubes.class);
    }

    @Override
    public void activate() {
        cubes.clear();
    }

    @Override
    public void deactivate() {
        cubes.clear();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        
        // Обновляем частицы (каждые 2 тика для оптимизации)
        if (++updateCounter % 2 == 0) {
            updateCubes();
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (cubes.isEmpty()) return;

        renderCubes(e);
    }

    private void updateCubes() {
        int targetCount = (int) count.getValue();
        
        // Спавн новых
        if (cubes.size() < targetCount) {
            for (int i = 0; i < Math.min(targetCount - cubes.size(), 5); i++) {
                cubes.add(spawnCube());
            }
        } else if (cubes.size() > targetCount) {
            cubes.subList(targetCount, cubes.size()).clear();
        }

        float f = 0.25F * speed.getValue();
        boolean isFalling = animation.isSelected("Падение");
        Vec3d playerPos = mc.player.getPos();
        double maxDistSq = (SPAWN_RADIUS * SPAWN_RADIUS) * 6.25;

        for (int i = cubes.size() - 1; i >= 0; i--) {
            CubeParticle cube = cubes.get(i);

            // Физика
            if (isFalling) {
                cube.wobblePhase += 0.06F * f;
                cube.x += cube.vx * f + Math.sin(cube.wobblePhase + cube.wobbleOffset) * 0.0024F * f;
                cube.y += cube.vy * f;
                cube.z += cube.vz * f + Math.cos(cube.wobblePhase * 0.8F + cube.wobbleOffset) * 0.002F * f;
                cube.vy = Math.max(cube.vy - 8.0E-5F * f, -0.032F);
            } else {
                cube.x += cube.vx * f;
                cube.y += cube.vy * f;
                cube.z += cube.vz * f;
                cube.vx *= 0.995F;
                cube.vy *= 0.995F;
                cube.vz *= 0.995F;
            }

            cube.rotX += cube.rotSpeedX * 0.2F * f;
            cube.rotY += cube.rotSpeedY * 0.2F * f;
            cube.rotZ += cube.rotSpeedZ * 0.2F * f;

            cube.life--;

            // Проверка на удаление
            double dx = cube.x - playerPos.x;
            double dy = cube.y - playerPos.y;
            double dz = cube.z - playerPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (cube.life <= 0 || distSq > maxDistSq || (isFalling && cube.y < playerPos.y - 2.5)) {
                cubes.remove(i);
                cubes.add(spawnCube());
            }
        }
    }

    private void renderCubes(WorldRenderEvent e) {
        MatrixStack matrices = e.getMatrixStack();
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        
        float baseSize = 0.18F * size.getValue();
        int themeColor = ColorUtil.getClientColor();
        
        float r = ((themeColor >> 16) & 255) / 255.0F;
        float g = ((themeColor >> 8) & 255) / 255.0F;
        float b = (themeColor & 255) / 255.0F;

        // Собираем видимые частицы
        visibleCubes.clear();
        for (CubeParticle cube : cubes) {
            double dx = cube.x - camPos.x;
            double dy = cube.y - camPos.y;
            double dz = cube.z - camPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;

            if (distSq <= MAX_RENDER_DISTANCE_SQ) {
                cube.renderAlpha = getAlpha(cube);
                if (cube.renderAlpha >= 0.01F) {
                    visibleCubes.add(cube);
                }
            }
        }

        if (visibleCubes.isEmpty()) return;

        // Настройка рендера
        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        // --- Рендер граней (без текстур, просто цвет) ---
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, net.minecraft.client.render.VertexFormats.POSITION_COLOR);

        boolean isCube = shape.isSelected("Кубы");

        for (CubeParticle cube : visibleCubes) {
            float alpha = cube.renderAlpha * 0.4F;
            if (alpha < 0.01F) continue;

            matrices.push();
            matrices.translate(cube.x - camPos.x, cube.y - camPos.y, cube.z - camPos.z);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cube.rotX));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(cube.rotY));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(cube.rotZ));

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            if (isCube) {
                appendCubeFaces(buffer, matrix, baseSize, r, g, b, alpha);
            } else {
                appendTriangleFaces(buffer, matrix, baseSize, r, g, b, alpha);
            }

            matrices.pop();
        }

        // Завершаем батч
        net.minecraft.client.render.BufferRenderer.drawWithGlobalProgram(buffer.end());

        // Восстановление настроек
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private float getAlpha(CubeParticle cube) {
        float lifeFactor = MathHelper.clamp((float) cube.life / (float) cube.maxLife, 0.0F, 1.0F);
        float spawnFactor = Math.min(1.0F, (float) (cube.maxLife - cube.life) / 20.0F);
        return lifeFactor * spawnFactor;
    }

    private void appendCubeFaces(BufferBuilder buffer, Matrix4f matrix, float size, float r, float g, float b, float alpha) {
        // Рисуем 6 граней куба
        // Передняя
        buffer.vertex(matrix, -size, -size, size).color(r, g, b, alpha);
        buffer.vertex(matrix, size, -size, size).color(r, g, b, alpha);
        buffer.vertex(matrix, size, size, size).color(r, g, b, alpha);
        buffer.vertex(matrix, -size, size, size).color(r, g, b, alpha);
        // Задняя
        buffer.vertex(matrix, size, -size, -size).color(r, g, b, alpha);
        buffer.vertex(matrix, -size, -size, -size).color(r, g, b, alpha);
        buffer.vertex(matrix, -size, size, -size).color(r, g, b, alpha);
        buffer.vertex(matrix, size, size, -size).color(r, g, b, alpha);
        // Левая
        buffer.vertex(matrix, -size, -size, -size).color(r, g, b, alpha);
        buffer.vertex(matrix, -size, -size, size).color(r, g, b, alpha);
        buffer.vertex(matrix, -size, size, size).color(r, g, b, alpha);
        buffer.vertex(matrix, -size, size, -size).color(r, g, b, alpha);
        // Правая
        buffer.vertex(matrix, size, -size, size).color(r, g, b, alpha);
        buffer.vertex(matrix, size, -size, -size).color(r, g, b, alpha);
        buffer.vertex(matrix, size, size, -size).color(r, g, b, alpha);
        buffer.vertex(matrix, size, size, size).color(r, g, b, alpha);
        // Верхняя
        buffer.vertex(matrix, -size, size, size).color(r, g, b, alpha);
        buffer.vertex(matrix, size, size, size).color(r, g, b, alpha);
        buffer.vertex(matrix, size, size, -size).color(r, g, b, alpha);
        buffer.vertex(matrix, -size, size, -size).color(r, g, b, alpha);
        // Нижняя
        buffer.vertex(matrix, -size, -size, -size).color(r, g, b, alpha);
        buffer.vertex(matrix, size, -size, -size).color(r, g, b, alpha);
        buffer.vertex(matrix, size, -size, size).color(r, g, b, alpha);
        buffer.vertex(matrix, -size, -size, size).color(r, g, b, alpha);
    }

    private void appendTriangleFaces(BufferBuilder buffer, Matrix4f matrix, float size, float r, float g, float b, float alpha) {
        float f = -size;
        float f2 = size * 0.866F;
        // Верхушка и основание (упрощенно)
        buffer.vertex(matrix, 0, size, 0).color(r, g, b, alpha);
        buffer.vertex(matrix, -f2, f, f2).color(r, g, b, alpha);
        buffer.vertex(matrix, f2, f, f2).color(r, g, b, alpha);

        buffer.vertex(matrix, 0, size, 0).color(r, g, b, alpha);
        buffer.vertex(matrix, f2, f, f2).color(r, g, b, alpha);
        buffer.vertex(matrix, f2, f, -f2).color(r, g, b, alpha);

        buffer.vertex(matrix, 0, size, 0).color(r, g, b, alpha);
        buffer.vertex(matrix, f2, f, -f2).color(r, g, b, alpha);
        buffer.vertex(matrix, -f2, f, -f2).color(r, g, b, alpha);

        buffer.vertex(matrix, 0, size, 0).color(r, g, b, alpha);
        buffer.vertex(matrix, -f2, f, -f2).color(r, g, b, alpha);
        buffer.vertex(matrix, -f2, f, f2).color(r, g, b, alpha);
    }

    private CubeParticle spawnCube() {
        boolean isFalling = animation.isSelected("Падение");
        int life = isFalling ? 260 + random.nextInt(220) : 420 + random.nextInt(420);

        double x = mc.player.getX() + (random.nextDouble() * 2.0 - 1.0) * SPAWN_RADIUS;
        double y = mc.player.getY() + (isFalling ? 4.0 : 2.0) + random.nextDouble() * SPAWN_RADIUS * 0.8;
        double z = mc.player.getZ() + (random.nextDouble() * 2.0 - 1.0) * SPAWN_RADIUS;

        float spd = speed.getValue();
        float vx, vy, vz;
        if (isFalling) {
            vx = (random.nextFloat() - 0.5F) * 0.008F * spd;
            vy = (-0.012F - random.nextFloat() * 0.012F) * spd;
            vz = (random.nextFloat() - 0.5F) * 0.008F * spd;
        } else {
            float angle = random.nextFloat() * 360.0F;
            float speedVal = (0.01F + random.nextFloat() * 0.02F) * spd;
            vx = -MathHelper.sin((float) Math.toRadians(angle)) * speedVal;
            vz = MathHelper.cos((float) Math.toRadians(angle)) * speedVal;
            vy = (random.nextFloat() - 0.5F) * 0.01F * spd;
        }

        return new CubeParticle(x, y, z, vx, vy, vz,
                random.nextFloat() * 360.0F, random.nextFloat() * 360.0F, random.nextFloat() * 360.0F,
                (random.nextFloat() - 0.5F) * 1.5F, (random.nextFloat() - 0.5F) * 1.5F, (random.nextFloat() - 0.5F) * 1.5F,
                life, (float) (random.nextDouble() * Math.PI * 2.0), random.nextFloat() * 10.0F);
    }

    private static class CubeParticle {
        double x, y, z;
        float vx, vy, vz;
        float rotX, rotY, rotZ;
        float rotSpeedX, rotSpeedY, rotSpeedZ;
        int life, maxLife;
        float wobblePhase, wobbleOffset;
        float renderAlpha;

        public CubeParticle(double x, double y, double z, float vx, float vy, float vz,
                            float rotX, float rotY, float rotZ,
                            float rotSpeedX, float rotSpeedY, float rotSpeedZ,
                            int life, float wobblePhase, float wobbleOffset) {
            this.x = x; this.y = y; this.z = z;
            this.vx = vx; this.vy = vy; this.vz = vz;
            this.rotX = rotX; this.rotY = rotY; this.rotZ = rotZ;
            this.rotSpeedX = rotSpeedX; this.rotSpeedY = rotSpeedY; this.rotSpeedZ = rotSpeedZ;
            this.life = life;
            this.maxLife = life;
            this.wobblePhase = wobblePhase;
            this.wobbleOffset = wobbleOffset;
        }
    }
                      }
