package ru.zov.api.file.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
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
import ru.zov.common.util.logger.LoggerUtil;
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
      if (!new File(path, this.getName() + ".json").isFile()) {
         return;
      }

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
      // Written to a temp file and moved into place, so an interrupted write cannot leave a truncated config behind.
      Path target = file.toPath().toAbsolutePath();
      Path temp = null;
      try {
         Files.createDirectories(target.getParent());
         temp = Files.createTempFile(target.getParent(), file.getName(), ".tmp");
         try (Writer writer = Files.newBufferedWriter(temp, StandardCharsets.UTF_8)) {
            this.GSON.toJson(functionObject, writer);
         }

         try {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
         } catch (AtomicMoveNotSupportedException e) {
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
         }
      } catch (IOException e) {
         if (temp != null) {
            try {
               Files.deleteIfExists(temp);
            } catch (IOException ignored) {
            }
         }

         throw new FileSaveException("Failed to save module to file", e);
      }
   }

   @Native
   private JsonObject readJsonFromFile(File file) throws FileLoadException {
      if (!file.isFile()) {
         throw new FileLoadException("Config file not found: " + file.getName());
      }

      try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
         JsonElement root = JsonParser.parseReader(reader);
         if (root == null || !root.isJsonObject()) {
            throw new FileLoadException("Config file is empty or is not a JSON object: " + file.getName());
         }

         return root.getAsJsonObject();
      } catch (IOException e) {
         throw new FileLoadException("Failed to load module from file", e);
      } catch (JsonParseException e) {
         throw new FileLoadException("Failed to parse JSON from file", e);
      }
   }

   @Native
   private void updateModulesFromJsonObject(JsonObject functionObject) {
      for (Module module : this.moduleRepository.modules()) {
         try {
            JsonObject moduleObject = functionObject.getAsJsonObject(module.getName().toLowerCase());
            if (moduleObject != null) {
               if (moduleObject.has("bind") && moduleObject.has("state")) {
                  module.setKey(moduleObject.get("bind").getAsInt());
                  module.setState(moduleObject.get("state").getAsBoolean());
               }

               module.settings().forEach(setting -> this.updateSettingFromJsonObject(moduleObject, setting));
            }
         } catch (RuntimeException e) {
            LoggerUtil.warn("Skipped module " + module.getName() + " while loading config: " + e);
         }
      }

      for (AbstractDraggable draggable : this.draggableRepository.draggable()) {
         try {
            JsonObject draggableObject = functionObject.getAsJsonObject(draggable.getName().toLowerCase());
            if (draggableObject != null && draggableObject.has("posX") && draggableObject.has("posY") && draggableObject.has("size")) {
               draggable.setX(draggableObject.get("posX").getAsInt());
               draggable.setY(draggableObject.get("posY").getAsInt());
               draggable.setSize(draggableObject.get("size").getAsDouble());
            }
         } catch (RuntimeException e) {
            LoggerUtil.warn("Skipped element " + draggable.getName() + " while loading config: " + e);
         }
      }
   }

   @Native
   private void updateSettingFromJsonObject(JsonObject moduleObject, Setting setting) {
      try {
         this.applySetting(moduleObject, setting);
      } catch (RuntimeException e) {
         LoggerUtil.warn("Skipped setting " + setting.getName() + " while loading config: " + e);
      }
   }

   private void applySetting(JsonObject moduleObject, Setting setting) {
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
