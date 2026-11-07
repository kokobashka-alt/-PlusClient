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
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.file.ClientFile;
import ru.zov.api.file.exception.FileLoadException;
import ru.zov.api.file.exception.FileSaveException;
import ru.zov.implement.features.modules.misc.InventoryCollector;

public class CollectorFile extends ClientFile {
   private final InventoryCollector collector = InventoryCollector.getInstance();

   public CollectorFile() {
      super("collector");
   }

   @Native
   public void saveToFile(File path) throws FileSaveException {
      Gson gson = (new GsonBuilder()).setPrettyPrinting().create();
      File file = new File(path, this.getName() + ".json");

      try {
         FileWriter writer = new FileWriter(file);

         try {
            List<Collector> list = new ArrayList();
            this.collector.playerInventory.main.forEach((stack) -> list.add(new Collector(Registries.ITEM.getRawId(stack.getItem()), stack.getCount(), Boolean.TRUE.equals(stack.get(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)))));
            gson.toJson(list, writer);
         } catch (Throwable var8) {
            try {
               writer.close();
            } catch (Throwable var7) {
               var8.addSuppressed(var7);
            }

            throw var8;
         }

         writer.close();
      } catch (IOException | JsonIOException e) {
         throw new FileSaveException(String.format("Failed to save %s to file", this.getName()), e);
      }
   }

   @Native
   public void loadFromFile(File path) throws FileLoadException {
      Gson gson = new Gson();
      File file = new File(path, this.getName() + ".json");

      try {
         FileReader reader = new FileReader(file);

         try {
            Collector[] collectors = (Collector[])gson.fromJson(reader, Collector[].class);
            this.collector.playerInventory.clear();

            for(int i = 0; i < collectors.length; ++i) {
               Collector coll = (Collector)Arrays.stream(collectors).toList().get(i);
               ComponentChanges changes = ComponentChanges.builder().build();
               ItemStack stack = ((Item)Registries.ITEM.get(coll.id)).getDefaultStack();
               stack.applyChanges(changes);
               stack.setCount(coll.count);
               stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, coll.enchantment);
               this.collector.playerInventory.main.set(i, stack);
            }
         } catch (Throwable var11) {
            try {
               reader.close();
            } catch (Throwable var10) {
               var11.addSuppressed(var10);
            }

            throw var11;
         }

         reader.close();
      } catch (IOException e) {
         throw new FileLoadException(String.format("Failed to load %s from file", this.getName()), e);
      } catch (JsonSyntaxException e) {
         throw new FileLoadException(String.format("JSON syntax error, %s config cannot be loaded", this.getName()), e);
      } catch (JsonIOException e) {
         throw new FileLoadException(String.format("JSON IO error, %s config cannot be loaded", this.getName()), e);
      }
   }

   private static record Collector(int id, int count, boolean enchantment) {
   }
}
