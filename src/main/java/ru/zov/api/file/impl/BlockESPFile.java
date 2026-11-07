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
import net.minecraft.registry.Registries;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.file.ClientFile;
import ru.zov.api.file.exception.FileLoadException;
import ru.zov.api.file.exception.FileSaveException;
import ru.zov.api.repository.BoxESPRepository;
import ru.zov.core.Main;

public class BlockESPFile extends ClientFile {
   private final BoxESPRepository repository;

   public BlockESPFile(Main main) {
      super("blockESP");
      this.repository = main.getBoxESPRepository();
   }

   @Native
   @Override
   public void saveToFile(File path) throws FileSaveException {
      Gson gson = new GsonBuilder().setPrettyPrinting().create();
      File file = new File(path, this.getName() + ".json");

      try (FileWriter writer = new FileWriter(file)) {
         List<BlockESPFile.BlockESP> blocksList = new ArrayList<>();
         this.repository.blocks.forEach((block, color) -> blocksList.add(new BlockESPFile.BlockESP(block.getTranslationKey(), color)));
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
         BlockESPFile.BlockESP[] blockESP = (BlockESPFile.BlockESP[])gson.fromJson(reader, BlockESPFile.BlockESP[].class);
         this.repository.blocks.clear();
         Arrays.asList(blockESP)
            .forEach(
               esp -> Registries.BLOCK
                  .stream()
                  .filter(type -> type.getTranslationKey().equals(esp.block))
                  .findFirst()
                  .ifPresent(type -> this.repository.blocks.put(type, esp.color))
            );
      } catch (IOException e) {
         throw new FileLoadException(String.format("Failed to load %s from file", this.getName()), e);
      } catch (JsonSyntaxException e) {
         throw new FileLoadException(String.format("JSON syntax error, %s config cannot be loaded", this.getName()), e);
      } catch (JsonIOException e) {
         throw new FileLoadException(String.format("JSON IO error, %s config cannot be loaded", this.getName()), e);
      }
   }

   private record BlockESP(String block, int color) {
   }
}
