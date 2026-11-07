package ru.zov.core;

import java.io.File;
import net.fabricmc.api.ModInitializer;
import net.minecraft.client.MinecraftClient;
import ru.nexusguard.UserProfile;
import ru.nexusguard.protection.annotations.Native;
import ru.zov.api.event.EventManager;
import ru.zov.api.feature.draggable.DraggableRepository;
import ru.zov.api.feature.module.ModuleProvider;
import ru.zov.api.feature.module.ModuleRepository;
import ru.zov.api.feature.module.ModuleSwitcher;
import ru.zov.api.file.DirectoryCreator;
import ru.zov.api.file.FileController;
import ru.zov.api.file.FileRepository;
import ru.zov.api.file.exception.FileProcessingException;
import ru.zov.api.repository.AutoFarmRepository;
import ru.zov.api.repository.BoxESPRepository;
import ru.zov.api.repository.IRCRepository;
import ru.zov.api.repository.MacroRepository;
import ru.zov.api.repository.PartyRepository;
import ru.zov.api.repository.RCTRepository;
import ru.zov.api.repository.WayRepository;
import ru.zov.api.system.discord.DiscordManager;
import ru.zov.api.system.sound.SoundManager;
import ru.zov.common.util.logger.LoggerUtil;
import ru.zov.common.util.performance.PerformanceConfig;
import ru.zov.common.util.render.ScissorManager;
import ru.zov.core.client.ClientInfo;
import ru.zov.core.client.ClientInfoProvider;
import ru.zov.core.listener.ListenerRepository;
import ru.zov.implement.features.commands.CommandDispatcher;
import ru.zov.implement.features.commands.manager.CommandRepository;
import ru.zov.implement.screens.menu.MenuScreen;

public class Main implements ModInitializer {
   static Main instance;
   private EventManager eventManager = new EventManager();
   private ModuleRepository moduleRepository;
   private ModuleSwitcher moduleSwitcher;
   private CommandRepository commandRepository;
   private CommandDispatcher commandDispatcher;
   private AutoFarmRepository autoFarmRepository = new AutoFarmRepository(this.eventManager);
   private BoxESPRepository boxESPRepository = new BoxESPRepository(this.eventManager);
   private MacroRepository macroRepository = new MacroRepository(this.eventManager);
   private PartyRepository partyRepository = new PartyRepository(this.eventManager);
   private IRCRepository ircRepository = new IRCRepository(this.eventManager);
   private WayRepository wayRepository = new WayRepository(this.eventManager);
   private RCTRepository RCTRepository = new RCTRepository(this.eventManager);
   private ModuleProvider moduleProvider;
   private DraggableRepository draggableRepository;
   private DiscordManager discordManager;
   private FileRepository fileRepository;
   private FileController fileController;
   private ScissorManager scissorManager = new ScissorManager();
   private ClientInfoProvider clientInfoProvider;
   private ListenerRepository listenerRepository;
   private boolean initialized;
   private boolean viaLoaded;

   @Native
   public void onInitialize() {
      instance = this;
      PerformanceConfig.install();
      this.initClientInfoProvider();
      this.initModules();
      this.initDraggable();
      this.initFileManager();
      this.initCommands();
      this.initListeners();
      this.initDiscordRPC();
      SoundManager.init();
      MenuScreen menuScreen = new MenuScreen();
      menuScreen.initialize();

      try {
         Class.forName("com.viaversion.viafabricplus.ViaFabricPlus");
         this.viaLoaded = true;
      } catch (ClassNotFoundException var3) {
      }

      this.initialized = true;
   }

   @Native
   private void initDraggable() {
      this.draggableRepository = new DraggableRepository();
      this.draggableRepository.setup();
   }

   @Native
   private void initModules() {
      this.moduleRepository = new ModuleRepository();
      this.moduleRepository.setup();
      this.moduleProvider = new ModuleProvider(this.moduleRepository.modules());
      this.moduleSwitcher = new ModuleSwitcher(this.moduleRepository.modules(), this.eventManager);
   }

   @Native
   private void initCommands() {
      this.commandRepository = new CommandRepository();
      this.commandDispatcher = new CommandDispatcher(this.eventManager);
   }

   @Native
   private void initDiscordRPC() {
      this.discordManager = new DiscordManager();
      this.discordManager.init();
   }

   @Native
   private void initClientInfoProvider() {
      File clientDirectory = new File(MinecraftClient.getInstance().runDirectory, "\\zov\\");
      File filesDirectory = new File(clientDirectory, "\\files\\");
      File moduleFilesDirectory = new File(filesDirectory, "\\config\\");
      this.clientInfoProvider = new ClientInfo("ZOV", UserProfile.username, UserProfile.role, clientDirectory, filesDirectory, moduleFilesDirectory);
   }

   @Native
   private void initFileManager() {
      DirectoryCreator directoryCreator = new DirectoryCreator();
      directoryCreator.createDirectories(this.clientInfoProvider.clientDir(), this.clientInfoProvider.filesDir(), this.clientInfoProvider.configsDir());
      this.fileRepository = new FileRepository();
      this.fileRepository.setup(this);
      this.fileController = new FileController(this.fileRepository.getClientFiles(), this.clientInfoProvider.filesDir(), this.clientInfoProvider.configsDir());

      try {
         this.fileController.loadFiles();
      } catch (FileProcessingException e) {
         LoggerUtil.error("Error occurred while loading files: " + e.getMessage() + " " + e.getCause());
      }
   }

   @Native
   private void initListeners() {
      this.listenerRepository = new ListenerRepository();
      this.listenerRepository.setup();
   }

   public EventManager getEventManager() {
      return this.eventManager;
   }

   public ModuleRepository getModuleRepository() {
      return this.moduleRepository;
   }

   public ModuleSwitcher getModuleSwitcher() {
      return this.moduleSwitcher;
   }

   public CommandRepository getCommandRepository() {
      return this.commandRepository;
   }

   public CommandDispatcher getCommandDispatcher() {
      return this.commandDispatcher;
   }

   public AutoFarmRepository getAutoFarmRepository() {
      return this.autoFarmRepository;
   }

   public BoxESPRepository getBoxESPRepository() {
      return this.boxESPRepository;
   }

   public MacroRepository getMacroRepository() {
      return this.macroRepository;
   }

   public PartyRepository getPartyRepository() {
      return this.partyRepository;
   }

   public IRCRepository getIrcRepository() {
      return this.ircRepository;
   }

   public WayRepository getWayRepository() {
      return this.wayRepository;
   }

   public RCTRepository getRCTRepository() {
      return this.RCTRepository;
   }

   public ModuleProvider getModuleProvider() {
      return this.moduleProvider;
   }

   public DraggableRepository getDraggableRepository() {
      return this.draggableRepository;
   }

   public DiscordManager getDiscordManager() {
      return this.discordManager;
   }

   public FileRepository getFileRepository() {
      return this.fileRepository;
   }

   public FileController getFileController() {
      return this.fileController;
   }

   public ScissorManager getScissorManager() {
      return this.scissorManager;
   }

   public ClientInfoProvider getClientInfoProvider() {
      return this.clientInfoProvider;
   }

   public ListenerRepository getListenerRepository() {
      return this.listenerRepository;
   }

   public boolean isInitialized() {
      return this.initialized;
   }

   public boolean isViaLoaded() {
      return this.viaLoaded;
   }

   public void setEventManager(EventManager eventManager) {
      this.eventManager = eventManager;
   }

   public void setModuleRepository(ModuleRepository moduleRepository) {
      this.moduleRepository = moduleRepository;
   }

   public void setModuleSwitcher(ModuleSwitcher moduleSwitcher) {
      this.moduleSwitcher = moduleSwitcher;
   }

   public void setCommandRepository(CommandRepository commandRepository) {
      this.commandRepository = commandRepository;
   }

   public void setCommandDispatcher(CommandDispatcher commandDispatcher) {
      this.commandDispatcher = commandDispatcher;
   }

   public void setAutoFarmRepository(AutoFarmRepository autoFarmRepository) {
      this.autoFarmRepository = autoFarmRepository;
   }

   public void setBoxESPRepository(BoxESPRepository boxESPRepository) {
      this.boxESPRepository = boxESPRepository;
   }

   public void setMacroRepository(MacroRepository macroRepository) {
      this.macroRepository = macroRepository;
   }

   public void setPartyRepository(PartyRepository partyRepository) {
      this.partyRepository = partyRepository;
   }

   public void setIrcRepository(IRCRepository ircRepository) {
      this.ircRepository = ircRepository;
   }

   public void setWayRepository(WayRepository wayRepository) {
      this.wayRepository = wayRepository;
   }

   public void setRCTRepository(RCTRepository RCTRepository) {
      this.RCTRepository = RCTRepository;
   }

   public void setModuleProvider(ModuleProvider moduleProvider) {
      this.moduleProvider = moduleProvider;
   }

   public void setDraggableRepository(DraggableRepository draggableRepository) {
      this.draggableRepository = draggableRepository;
   }

   public void setDiscordManager(DiscordManager discordManager) {
      this.discordManager = discordManager;
   }

   public void setFileRepository(FileRepository fileRepository) {
      this.fileRepository = fileRepository;
   }

   public void setFileController(FileController fileController) {
      this.fileController = fileController;
   }

   public void setScissorManager(ScissorManager scissorManager) {
      this.scissorManager = scissorManager;
   }

   public void setClientInfoProvider(ClientInfoProvider clientInfoProvider) {
      this.clientInfoProvider = clientInfoProvider;
   }

   public void setListenerRepository(ListenerRepository listenerRepository) {
      this.listenerRepository = listenerRepository;
   }

   public void setInitialized(boolean initialized) {
      this.initialized = initialized;
   }

   public void setViaLoaded(boolean viaLoaded) {
      this.viaLoaded = viaLoaded;
   }

   public static Main getInstance() {
      return instance;
   }
}
