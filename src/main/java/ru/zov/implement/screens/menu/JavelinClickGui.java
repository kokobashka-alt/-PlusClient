package ru.zov.implement.screens.menu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleCategory;
import ru.zov.api.system.font.Fonts;
import ru.zov.api.system.shape.ShapeProperties;
import ru.zov.common.QuickImports;
import ru.zov.common.util.color.ColorUtil;
import ru.zov.common.util.math.MathUtil;
import ru.zov.core.Main;

import java.util.ArrayList;
import java.util.List;

/**
 * JavelinClickGui: five-column category layout inspired by the supplied reference.
 * Module values and actions are sourced from this client's ModuleRepository.
 */
public final class JavelinClickGui extends Screen implements QuickImports {
    public static final JavelinClickGui INSTANCE = new JavelinClickGui();
    private static final int PANEL = 0xE9141720;
    private static final int ROW = 0xB91D2230;
    private static final int ROW_HOVER = 0xD92A3042;
    private static final int TEXT = 0xFFE7EAF2;
    private static final int MUTED = 0xFF9AA4B8;
    private static final int ACCENT = 0xFF8B5CF6;
    private String search = "";
    private int scroll;
    private int panelX, panelY, panelW, panelH;

    private JavelinClickGui() { super(Text.literal("JavelinClickGui")); }

    @Override public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, width, height, 0x70000000);
        panelW = Math.min(1000, width - 24);
        panelH = Math.min(470, height - 24);
        panelX = (width-panelW)/2; panelY = (height-panelH)/2;
        rectangle.render(ShapeProperties.create(ctx.getMatrices(), panelX, panelY, panelW, panelH)
                .round(12).color(PANEL).build());
        Fonts.getSize(18, Fonts.Type.BOLD).drawString(ctx.getMatrices(), "Javelin", panelX+18, panelY+14, TEXT);
        Fonts.getSize(12).drawString(ctx.getMatrices(), "CLICKGUI", panelX+88, panelY+18, ACCENT);
        Fonts.getSize(12).drawString(ctx.getMatrices(), "Right Shift  •  Search below", panelX+panelW-180, panelY+18, MUTED);

        ModuleCategory[] cats = {ModuleCategory.COMBAT, ModuleCategory.MOVEMENT, ModuleCategory.RENDER, ModuleCategory.PLAYER, ModuleCategory.MISC};
        int gap=10, left=14, top=42, colW=(panelW-left*2-gap*4)/5;
        for (int c=0;c<cats.length;c++) {
            int x=panelX+left+c*(colW+gap), y=panelY+top;
            rectangle.render(ShapeProperties.create(ctx.getMatrices(), x,y,colW,panelH-top-48)
                    .round(8).color(0xA510131B).build());
            Fonts.getSize(14, Fonts.Type.BOLD).drawString(ctx.getMatrices(), cats[c].getReadableName(), x+10,y+10,TEXT);
            int yy=y+32;
            List<Module> mods=new ArrayList<>();
            for(Module m:Main.getInstance().getModuleRepository().modules())
                if(m.getCategory()==cats[c] && (search.isBlank() || m.getVisibleName().toLowerCase().contains(search.toLowerCase()))) mods.add(m);
            for(Module m:mods) {
                int ry=yy-scroll;
                if(ry>=y+27 && ry<y+panelH-top-30) {
                    boolean hover=mouseX>=x+5&&mouseX<=x+colW-5&&mouseY>=ry&&mouseY<ry+22;
                    ctx.fill(x+5,ry,x+colW-5,ry+22,hover?ROW_HOVER:ROW);
                    ctx.fill(x+8,ry+7,x+11,ry+15,m.isState()?ACCENT:0xFF626B7B);
                    Fonts.getSize(12).drawString(ctx.getMatrices(),m.getVisibleName(),x+17,ry+7,TEXT);
                    Fonts.getSize(11).drawString(ctx.getMatrices(),m.isState()?"ON":"OFF",x+colW-31,ry+7,m.isState()?0xFFB9A0FF:MUTED);
                }
                yy+=25;
            }
        }
        int sy=panelY+panelH-34;
        ctx.fill(panelX+14,sy,panelX+panelW-14,sy+23,0xD91B202B);
        Fonts.getSize(12).drawString(ctx.getMatrices(),"Search: "+(search.isEmpty()?"type to filter":search),panelX+24,sy+7,MUTED);
        super.render(ctx,mouseX,mouseY,delta);
    }

    @Override public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubleClick) {
        int mx=(int)click.x(), my=(int)click.y();
        if(click.button()==0) {
            ModuleCategory[] cats={ModuleCategory.COMBAT,ModuleCategory.MOVEMENT,ModuleCategory.RENDER,ModuleCategory.PLAYER,ModuleCategory.MISC};
            int gap=10,left=14,top=42,colW=(panelW-left*2-gap*4)/5;
            for(int c=0;c<cats.length;c++){
                int x=panelX+left+c*(colW+gap), y=panelY+top, yy=y+32;
                for(Module m:Main.getInstance().getModuleRepository().modules()) {
                    if(m.getCategory()!=cats[c] || (!search.isBlank()&&!m.getVisibleName().toLowerCase().contains(search.toLowerCase())))continue;
                    int ry=yy-scroll;
                    if(mx>=x+5&&mx<=x+colW-5&&my>=ry&&my<ry+22){m.switchState();return true;}
                    yy+=25;
                }
            }
        }
        return super.mouseClicked(click,doubleClick);
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if(input.key()==256){close();return true;}
        if(input.key()==259&&!search.isEmpty()){search=search.substring(0,search.length()-1);return true;}
        if(input.key()==261){search="";return true;}
        return super.keyPressed(input);
    }
    @Override public boolean charTyped(net.minecraft.client.input.CharInput input) {
        char ch=(char)input.codepoint();
        if(!Character.isISOControl(ch)){search+=ch;return true;}
        return super.charTyped(input);
    }
    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontal,double vertical){
        scroll=Math.max(0,Math.min(600,scroll-(int)(vertical*25)));return true;
    }
    @Override public boolean shouldPause(){return false;}
}
