package ru.zov.api.file;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.file.exception.FileLoadException;
import ru.zov.api.file.exception.FileSaveException;
import ru.zov.api.file.impl.ModuleFile;
import ru.zov.common.util.logger.LoggerUtil;

public class FileController {
   private final List<ClientFile> clientFiles;
   private final File directory;
   private final File moduleConfigDirectory;
   private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
      Thread thread = new Thread(runnable, "client-autosave");
      thread.setDaemon(true);
      return thread;
   });

   public FileController(List<ClientFile> clientFiles, File directory, File moduleConfigDirectory) {
      this.clientFiles = clientFiles;
      this.directory = directory;
      this.moduleConfigDirectory = moduleConfigDirectory;
      this.startAutoSave();
   }

   @Native
   public void startAutoSave() {
      LoggerUtil.info("Auto-save system started!");
      this.scheduler.scheduleAtFixedRate(() -> {
         // Any exception escaping a scheduled task silently cancels every later run, so nothing may escape here.
         try {
            LoggerUtil.info("Saving with auto-save.");
            this.saveFiles();
         } catch (Exception e) {
            LoggerUtil.error("Failed to auto-save files: " + e.getMessage());
         }
      }, 1L, 1L, TimeUnit.MINUTES);
   }

   @Native
   public void stopAutoSave() {
      LoggerUtil.info("Auto-save shutdown!");
      this.scheduler.shutdown();

      try {
         if (!this.scheduler.awaitTermination(1L, TimeUnit.MINUTES)) {
            this.scheduler.shutdownNow();
         }
      } catch (InterruptedException e) {
         this.scheduler.shutdownNow();
      }
   }

   /** Saves every file even if one of them fails; the first failure is rethrown afterwards. */
   @Native
   public void saveFiles() throws FileSaveException {
      if (this.clientFiles.isEmpty()) {
         LoggerUtil.warn("No files to save from directory: " + this.directory.getPath());
         return;
      }

      this.directory.mkdirs();
      FileSaveException failure = null;
      for (ClientFile clientFile : this.clientFiles) {
         try {
            clientFile.saveToFile(this.directory);
            LoggerUtil.info("Successfully saved file: " + clientFile.getName() + " to " + this.directory.getPath());
         } catch (FileSaveException | RuntimeException e) {
            LoggerUtil.error("Failed to save file: " + clientFile.getName() + " (" + e.getMessage() + ")");
            if (failure == null) {
               failure = new FileSaveException("Failed to save file: " + clientFile.getName(), e);
            }
         }
      }

      if (failure != null) {
         throw failure;
      }
   }

   /** Loads every file even if one of them fails; the first failure is rethrown afterwards. */
   @Native
   public void loadFiles() throws FileLoadException {
      if (this.clientFiles.isEmpty()) {
         LoggerUtil.warn("No files to load from directory: " + this.directory.getPath());
         return;
      }

      FileLoadException failure = null;
      for (ClientFile clientFile : this.clientFiles) {
         try {
            clientFile.loadFromFile(this.directory);
            LoggerUtil.info("Successfully loaded file: " + clientFile.getName() + " from " + this.directory.getPath());
         } catch (FileLoadException | RuntimeException e) {
            LoggerUtil.error("Failed to load file: " + clientFile.getName() + " (" + e.getMessage() + ")");
            if (failure == null) {
               failure = new FileLoadException("Failed to load file: " + clientFile.getName(), e);
            }
         }
      }

      if (failure != null) {
         throw failure;
      }
   }

   @Native
   public void saveFile(String fileName) throws FileSaveException {
      this.moduleConfigDirectory.mkdirs();
      for (ClientFile clientFile : this.clientFiles) {
         if (clientFile instanceof ModuleFile) {
            try {
               clientFile.saveToFile(this.moduleConfigDirectory, fileName);
               LoggerUtil.info("Successfully saved file: " + fileName + " to " + this.moduleConfigDirectory.getPath());
            } catch (FileSaveException e) {
               throw new FileSaveException("Failed to save file: " + fileName, e);
            }
         }
      }
   }

   @Native
   public void loadFile(String fileName) throws FileLoadException {
      for (ClientFile clientFile : this.clientFiles) {
         if (clientFile instanceof ModuleFile) {
            try {
               clientFile.loadFromFile(this.moduleConfigDirectory, fileName);
               LoggerUtil.info("Successfully loaded file: " + fileName + " from " + this.moduleConfigDirectory.getPath());
            } catch (FileLoadException e) {
               throw new FileLoadException("Failed to load file: " + fileName, e);
            }
         }
      }
   }
}
