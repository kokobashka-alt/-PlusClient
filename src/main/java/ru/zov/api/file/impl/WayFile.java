package ru.zov.api.file.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.file.ClientFile;
import ru.zov.api.file.exception.FileLoadException;
import ru.zov.api.file.exception.FileSaveException;
import ru.zov.api.repository.WayRepository;
import ru.zov.core.Main;

public class WayFile extends ClientFile {
   private final WayRepository wayRepository;

   public WayFile(Main main) {
      super("way");
      this.wayRepository = main.getWayRepository();
   }

   @Native
   @Override
   public void saveToFile(File path) throws FileSaveException {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      File file = new File(path, this.getName() + ".json");

      try (FileWriter writer = new FileWriter(file)) {
         gson.toJson(this.wayRepository.wayList, writer);
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
         WayRepository.Way[] ways = (WayRepository.Way[])gson.fromJson(reader, WayRepository.Way[].class);
         this.wayRepository.wayList.clear();
         this.wayRepository.wayList.addAll(Arrays.asList(ways));
      } catch (IOException e) {
         throw new FileLoadException(String.format("Failed to load %s from file", this.getName()), e);
      } catch (JsonSyntaxException e) {
         throw new FileLoadException(String.format("JSON syntax error, %s config cannot be loaded", this.getName()), e);
      } catch (JsonIOException e) {
         throw new FileLoadException(String.format("JSON IO error, %s config cannot be loaded", this.getName()), e);
      }
   }
}
