package ru.zov.api.file.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.feature.draggable.AbstractDraggable;
import ru.zov.api.feature.draggable.DraggableRepository;
import ru.zov.api.feature.module.Module;
import ru.zov.api.feature.module.ModuleRepository;
import ru.zov.api.feature.module.setting.Setting;
import ru.zov.api.feature.module.setting.implement.BindSetting;
import ru.zov.api.feature.module.setting.implement.BooleanSetting;
import ru.zov.api.feature.module.setting.implement.ColorSetting;
import ru.zov.api.feature.module.setting.implement.GroupSetting;
import ru.zov.api.feature.module.setting.implement.MultiSelectSetting;
import ru.zov.api.feature.module.setting.implement.SelectSetting;
import ru.zov.api.feature.module.setting.implement.TextSetting;
import ru.zov.api.feature.module.setting.implement.ValueSetting;
import ru.zov.api.file.ClientFile;
import ru.zov.api.file.exception.FileLoadException;
import ru.zov.api.file.exception.FileSaveException;
import ru.zov.core.Main;

public class ModuleFile extends ClientFile {
   private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
   private final ModuleRepository moduleRepository;
   private final DraggableRepository draggableRepository;

   public ModuleFile(Main main) {
      super("autoCfg");
      this.moduleRepository = main.getModuleRepository();
      this.draggableRepository = main.getDraggableRepository();
   }

   @Native
   @Override
   public void saveToFile(File path) throws FileSaveException {
      this.saveToFile(path, this.getName() + ".json");
   }

   @Native
   @Override
   public void loadFromFile(File path) throws FileLoadException {
      this.loadFromFile(path, this.getName() + ".json");
   }

   @Native
   @Override
   public void saveToFile(File path, String fileName) throws FileSaveException {
      JsonObject functionObject = this.createJsonObjectFromModules();
      File file = new File(path, fileName);
      this.writeJsonToFile(functionObject, file);
      super.saveToFile(path, fileName);
   }

   @Native
   @Override
   public void loadFromFile(File path, String fileName) throws FileLoadException {
      File file = new File(path, fileName);
      JsonObject functionObject = this.readJsonFromFile(file);
      if (functionObject != null) {
         this.updateModulesFromJsonObject(functionObject);
      }

      super.loadFromFile(path, fileName);
   }

   @Native
   private JsonObject createJsonObjectFromModules() {
      JsonObject functionObject = new JsonObject();

      for (Module module : this.moduleRepository.modules()) {
         JsonObject moduleObject = new JsonObject();
         moduleObject.addProperty("bind", module.getKey());
         moduleObject.addProperty("state", module.isState());
         module.settings().forEach(setting -> this.addSettingToJsonObject(moduleObject, setting));
         functionObject.add(module.getName().toLowerCase(), moduleObject);
      }

      for (AbstractDraggable draggable : this.draggableRepository.draggable()) {
         JsonObject draggableObject = new JsonObject();
         draggableObject.addProperty("posX", draggable.getX());
         draggableObject.addProperty("posY", draggable.getY());
         draggableObject.addProperty("size", draggable.getSize());
         functionObject.add(draggable.getName().toLowerCase(), draggableObject);
      }

      return functionObject;
   }

   @Native
   private void addSettingToJsonObject(JsonObject moduleObject, Setting setting) {
      if (setting instanceof BooleanSetting booleanSetting) {
         moduleObject.addProperty(setting.getName(), booleanSetting.isValue());
      }

      if (setting instanceof ValueSetting valueSetting) {
         moduleObject.addProperty(setting.getName(), valueSetting.getValue());
      }

      if (setting instanceof ColorSetting colorSetting) {
         moduleObject.addProperty(setting.getName(), colorSetting.getColor());
      }

      if (setting instanceof BindSetting bindSetting) {
         moduleObject.addProperty(setting.getName(), bindSetting.getKey());
      }

      if (setting instanceof TextSetting textSetting) {
         moduleObject.addProperty(setting.getName(), textSetting.getText());
      }

      if (setting instanceof SelectSetting selectSetting) {
         moduleObject.addProperty(setting.getName(), selectSetting.getSelected());
      }

      if (setting instanceof MultiSelectSetting multiSelectSetting) {
         List<String> selected = multiSelectSetting.getSelected();
         String selectedAsString = String.join(",", selected);
         moduleObject.addProperty(setting.getName(), selectedAsString);
      }

      if (setting instanceof GroupSetting groupSetting) {
         JsonObject groupObject = new JsonObject();
         groupObject.addProperty("state", groupSetting.isValue());

         for (Setting subSetting : groupSetting.getSubSettings()) {
            this.addSettingToJsonObject(groupObject, subSetting);
         }

         moduleObject.add(setting.getName(), groupObject);
      }
   }

   @Native
   private void writeJsonToFile(JsonObject functionObject, File file) throws FileSaveException {
      try (FileWriter writer = new FileWriter(file)) {
         this.GSON.toJson(functionObject, writer);
      } catch (IOException e) {
         throw new FileSaveException("Failed to save module to file", e);
      }
   }

   @Native
   private JsonObject readJsonFromFile(File file) throws FileLoadException {
      try (FileReader reader = new FileReader(file)) {
         return JsonParser.parseReader(reader).getAsJsonObject();
      } catch (IOException e) {
         throw new FileLoadException("Failed to load module from file", e);
      } catch (JsonSyntaxException | JsonIOException e) {
         throw new FileLoadException("Failed to parse JSON from file", e);
      }
   }

   @Native
   private void updateModulesFromJsonObject(JsonObject functionObject) {
      for (Module module : this.moduleRepository.modules()) {
         JsonObject moduleObject = functionObject.getAsJsonObject(module.getName().toLowerCase());
         if (moduleObject != null) {
            if (moduleObject.has("bind") && moduleObject.has("state")) {
               module.setKey(moduleObject.get("bind").getAsInt());
               module.setState(moduleObject.get("state").getAsBoolean());
            }

            module.settings().forEach(setting -> this.updateSettingFromJsonObject(moduleObject, setting));
         }
      }

      for (AbstractDraggable draggable : this.draggableRepository.draggable()) {
         JsonObject draggableObject = functionObject.getAsJsonObject(draggable.getName().toLowerCase());
         if (draggableObject != null && draggableObject.has("posX") && draggableObject.has("posY") && draggableObject.has("size")) {
            draggable.setX(draggableObject.get("posX").getAsInt());
            draggable.setY(draggableObject.get("posY").getAsInt());
            draggable.setSize(draggableObject.get("size").getAsDouble());
         }
      }
   }

   @Native
   private void updateSettingFromJsonObject(JsonObject moduleObject, Setting setting) {
      JsonElement settingElement = moduleObject.get(setting.getName());
      if (settingElement != null && !settingElement.isJsonNull()) {
         if (setting instanceof BooleanSetting booleanSetting) {
            booleanSetting.setValue(settingElement.getAsBoolean());
         }

         if (setting instanceof ValueSetting valueSetting) {
            valueSetting.setValue(settingElement.getAsFloat());
         }

         if (setting instanceof ColorSetting colorSetting) {
            colorSetting.setColor(settingElement.getAsInt());
         }

         if (setting instanceof BindSetting bindSetting) {
            bindSetting.setKey(settingElement.getAsInt());
         }

         if (setting instanceof TextSetting textSetting) {
            textSetting.setText(settingElement.getAsString());
         }

         if (setting instanceof SelectSetting selectSetting) {
            selectSetting.setSelected(settingElement.getAsString());
         }

         if (setting instanceof MultiSelectSetting multiSelectSetting) {
            String asString = settingElement.getAsString();
            List<String> selectedList = new ArrayList<>(Arrays.asList(asString.split(",")));
            selectedList.removeIf(s -> !multiSelectSetting.getList().contains(s));
            multiSelectSetting.setSelected(selectedList);
         }

         if (setting instanceof GroupSetting groupSetting) {
            JsonObject groupObject = settingElement.getAsJsonObject();
            if (groupObject.has("state")) {
               groupSetting.setValue(groupObject.get("state").getAsBoolean());
            }

            for (Setting subSetting : groupSetting.getSubSettings()) {
               this.updateSettingFromJsonObject(groupObject, subSetting);
            }
         }
      }
   }
}
