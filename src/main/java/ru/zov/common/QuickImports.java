package ru.zov.common;

import com.google.gson.Gson;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.util.Window;
import ru.zov.api.system.draw.DrawEngine;
import ru.zov.api.system.draw.DrawEngineImpl;
import ru.zov.api.system.shape.implement.Arc;
import ru.zov.api.system.shape.implement.Blur;
import ru.zov.api.system.shape.implement.Image;
import ru.zov.api.system.shape.implement.Rectangle;
import ru.zov.core.Main;
import ru.zov.implement.screens.menu.components.implement.window.WindowManager;

public interface QuickImports extends QuickLogger {
   Main main = Main.getInstance();
   MinecraftClient mc = MinecraftClient.getInstance();
   RenderTickCounter tickCounter = mc.getRenderTickCounter();
   Window window = mc.getWindow();
   Tessellator tessellator = Tessellator.getInstance();
   DrawEngine drawEngine = new DrawEngineImpl();
   Rectangle rectangle = new Rectangle();
   Blur blur = new Blur();
   Arc arc = new Arc();
   Image image = new Image();
   Gson gson = new Gson();
   WindowManager windowManager = new WindowManager();
}
