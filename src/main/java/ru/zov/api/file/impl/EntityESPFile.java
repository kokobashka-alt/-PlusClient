package ru.zov.api.file.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.entity.EntityType;
import net.minecraft.registry.Registries;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.file.ClientFile;
import ru.zov.api.file.exception.FileLoadException;
import ru.zov.api.file.exception.FileSaveException;
import ru.zov.api.repository.BoxESPRepository;
import ru.zov.core.Main;

public class EntityESPFile extends ClientFile {
   private final BoxESPRepository repository;

   public EntityESPFile(Main main) {
      super("entityESP");
      this.repository = main.getBoxESPRepository();
   }

   @Native
   @Override
   public void saveToFile(File path) throws FileSaveException {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      File file = new File(path, this.getName() + ".json");

      try (FileWriter writer = new FileWriter(file)) {
         List<EntityESPFile.EntityESP> blocksList = new ArrayList<>();
         this.repository.entities.forEach((entity, color) -> blocksList.add(new EntityESPFile.EntityESP(entity.getTranslationKey(), color)));
         gson.toJson(blocksList, writer);
      } catch (JsonIOException | IOException e) {
         throw new FileSaveException(String.format("Failed to save %s to file", this.getName()), e);
      }
   }

   @Native
   @Override
   public void loadFromFile(File path) throws FileLoadException {
      Gson gson = new Gson();
      File file = new File(path, this.getName() + ".json");

      try (FileReader reader = new FileReader(file)) {
         EntityESPFile.EntityESP[] blockESP = (EntityESPFile.EntityESP[])gson.fromJson(reader, EntityESPFile.EntityESP[].class);
         this.repository.entities.clear();
         Arrays.asList(blockESP)
            .forEach(
               esp -> Registries.ENTITY_TYPE
                  .stream()
                  .filter(type -> type.getTranslationKey().equals(esp.entity))
                  .findFirst()
                  .ifPresent(type -> this.repository.entities.put((EntityType<?>)type, esp.color))
            );
      } catch (IOException e) {
         throw new FileLoadException(String.format("Failed to load %s from file", this.getName()), e);
      } catch (JsonSyntaxException e) {
         throw new FileLoadException(String.format("JSON syntax error, %s config cannot be loaded", this.getName()), e);
      } catch (JsonIOException e) {
         throw new FileLoadException(String.format("JSON IO error, %s config cannot be loaded", this.getName()), e);
      }
   }

   private record EntityESP(String entity, int color) {
   }
}
